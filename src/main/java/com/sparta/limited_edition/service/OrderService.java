package com.sparta.limited_edition.service;

import com.sparta.limited_edition.dto.OrderItemResponse;
import com.sparta.limited_edition.dto.OrderRequest;
import com.sparta.limited_edition.dto.OrderResponse;
import com.sparta.limited_edition.entity.*;
import com.sparta.limited_edition.repository.*;
import com.sparta.limited_edition.util.ReturnCompletionJob;
import com.sparta.limited_edition.util.UpdateOrderStatusJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductSnapshotRepository productSnapshotRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final SchedulerFactoryBean schedulerFactoryBean;

    public OrderService(ProductRepository productRepository, ProductSnapshotRepository productSnapshotRepository, OrderRepository orderRepository, OrderDetailRepository orderDetailRepository, UserRepository userRepository, WishlistRepository wishlistRepository, @Qualifier("schedulerFactoryBean") SchedulerFactoryBean schedulerFactoryBean) {
        this.productRepository = productRepository;
        this.productSnapshotRepository = productSnapshotRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.userRepository = userRepository;
        this.wishlistRepository = wishlistRepository;
        this.schedulerFactoryBean = schedulerFactoryBean;
    }

    // 주문하기
    @Transactional
    public OrderResponse createOrder(String email, List<OrderRequest> orderItems) {
        // 사용자 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));

        // order 객체 생성
        Orders order = new Orders(user, "주문 완료", 0); // 총 금액 초기화
        orderRepository.save(order);

        // OrderDetail 객체 생성
        List<OrderDetail> orderDetails = new ArrayList<>();
        int totalAmount = 0;

        // 상품 하나하나 처리
        for (OrderRequest item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            // 상품 재고 확인
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("재고가 부족합니다. (남은 재고: " + product.getStockQuantity() + ")");
            }

            // 스냅샷 생성/재사용
            ProductSnapshot snapshot = productSnapshotRepository.findByProduct_Id(product.getId())
                    .orElseGet(() -> productSnapshotRepository.save(new ProductSnapshot(product)));

            // 상품 재고 감소
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            // 상품별 주문 금액 계산 및 저장
            int subtotalAmount = item.getQuantity() * product.getPrice();
            OrderDetail orderDetail = new OrderDetail(order, snapshot, item.getQuantity(), subtotalAmount);
            orderDetails.add(orderDetail);
            totalAmount += subtotalAmount; // 전체 금액 누적
        }

        // 주문 정보 업데이트
        order.setTotalAmount(totalAmount);
        orderDetailRepository.saveAll(orderDetails);

        // [주문 완료] -> [배송중] -> [배송 완료] 상태 자동 업데이트하는 job 생성
        scheduleOrderStatusJobs(order.getId());

        // 위시리스트에서 삭제
        wishlistRepository.deleteAllByUserId(user.getId());

        // 주문 상세 응답 생성
        List<OrderItemResponse> orderItemResponses = orderDetails.stream()
                .map(detail -> new OrderItemResponse(
                        detail.getProductSnapshot().getProductId(),
                        detail.getProductSnapshot().getName(),
                        detail.getQuantity(),
                        detail.getProductSnapshot().getPrice(),
                        detail.getSubtotalAmount()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(order.getId(), order.getStatus(), totalAmount, orderItemResponses);
    }

    // 주문내역 목록 조회
    @Transactional
    public List<OrderResponse> getOrderDeatils(String email) {
        // 해당 사용자의 모든 주문내용 가져오기
        List<Orders> orders = orderRepository.findAllByUserEmailAndStatusIn(email, List.of("주문 완료", "배송중", "배송 완료"));
        // 주문 내역(Order)리스트를 OrderResponse 리스트로 변환
        return orders.stream().map(order -> { // 각 order 객체를 orderResponse 객체로 매핑
            // 해당 주문id에 대한 주문상세(OrderDetail) 리스트 가져오기
            List<OrderItemResponse> orderItemResponseList = orderDetailRepository.findAllByOrdersId(order.getId())
                    .stream()
                    .map(detail -> new OrderItemResponse( // orderDetail의 각 요소들을 orderItemResponse에 매핑
                            detail.getProductSnapshot().getProductId(),
                            detail.getProductSnapshot().getName(),
                            detail.getQuantity(),
                            detail.getProductSnapshot().getPrice(),
                            detail.getSubtotalAmount()
                    ))
                    .toList(); // orderItemResponseList 리스트로 변환

            // OrderResponse 객체 생성하고 orderItemResponseList 넣어서 반환
            return new OrderResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    orderItemResponseList
            );
        }).toList(); // orderResponse 리스트로 최종 변환
    }

    // 취소, 반품내역 목록 조회
    @Transactional
    public List<OrderResponse> getCancelAndReturn(String email) {
        // 해당 사용자의 모든 취소/반품 주문 가져오기
        List<Orders> orders = orderRepository.findAllByUserEmailAndStatusIn(email, List.of("취소 완료", "반품 신청", "반품 완료"));
        // 취소, 반품 내역(Order)리스트를 OrderResponse 리스트로 변환
        return orders.stream().map(order -> { // 각 order 객체를 orderResponse 객체로 매핑
            // 해당 주문id에 대한 주문상세(OrderDetail) 리스트 가져오기
            List<OrderItemResponse> orderItemResponseList = orderDetailRepository.findAllByOrdersId(order.getId())
                    .stream()
                    .map(detail -> new OrderItemResponse( // orderDetail의 각 요소들을 orderItemResponse에 매핑
                            detail.getProductSnapshot().getProductId(),
                            detail.getProductSnapshot().getName(),
                            detail.getQuantity(),
                            detail.getProductSnapshot().getPrice(),
                            detail.getSubtotalAmount()
                    ))
                    .toList(); // orderItemResponseList 리스트로 변환

            // OrderResponse 객체 생성하고 orderItemResponseList 넣어서 반환
            return new OrderResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    orderItemResponseList
            );
        }).toList(); // orderResponse 리스트로 최종 변환
    }

    // 주문 취소하기
    @Transactional
    public String cancelOrder(String email, Long orderId) {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        // 주문 검증
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
        // 취소 가능 상태인지
        if (!"주문 완료".equals(orders.getStatus())) {
            throw new IllegalArgumentException("주문을 취소할 수 없는 상태입니다.");
        }
        // 상태 변경
        orders.setStatus("취소 완료");
        orders.setUpdatedAt(LocalDateTime.now());
        // 주문한 상품 하나하나 찾아서 -> 재고 복구
        List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrdersId(orderId);
        for (OrderDetail detail : orderDetails) {
            Product product = productRepository.findById(detail.getProductSnapshot().getId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));
            product.setStockQuantity(product.getStockQuantity() + detail.getQuantity());
            productRepository.save(product);
        }
        orderRepository.save(orders);
        return "취소 완료";

    }

    // 반품 처리
    @Transactional
    public String returnOrder(String email, Long orderId) {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        // 주문 검증
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
        // 반품 가능 상태인지
        if (!"배송 완료".equals(orders.getStatus())) {
            throw new IllegalArgumentException("배송 완료된 상품만 반품 가능합니다.");
        }
        // 배송받은지 1일 이내인지
        if (orders.getUpdatedAt().plusDays(1).isBefore(LocalDateTime.now())) // updatedAt + 1일이 현재 시간보다 이전인지 확인
            throw new IllegalArgumentException("배송 완료 후 24시간이 지나 반품할 수 없습니다.");
        // 상태 변경
        orders.setStatus("반품 신청");
        orders.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(orders);

        // Quartz Job 등록
        scheduleReturnCompletionJob(orderId);

        // 스케줄러에서, D+1이 지나면 알아서 "반품완료"로 변경되고 재고 복구된다.
        return "반품 신청";
    }

    // 반품 신청 시 Quartz Job 등록
    private void scheduleReturnCompletionJob(Long orderId) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            // JobDetail 생성
            JobDetail jobDetail = JobBuilder.newJob(ReturnCompletionJob.class) // ReturnCompletionJob 클래스를 job으로 설정
                    .withIdentity("returnCompletionJob-" + orderId) // 고유식별자 이름 설정
                    .usingJobData("orderId", orderId) // 주문 ID 포함
                    .storeDurably()
                    .build();
            // Trigger 생성
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail) // 트리거와 JobDetail 연결시킴
                    .withIdentity("returnCompletionTrigger-" + orderId) // 고유식별자 이름 설정
                    // 24시간 후에 실행
                    .startAt(Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
            // JobDetail과 Trigger로 스케줄링
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw new RuntimeException("Quartz Job 등록 중 오류 발생", e);
        }
    }

    // 주문 시 Quartz Job 등록
    private void scheduleOrderStatusJobs(Long orderId) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            // 1일 뒤 -> 배송중 상태로 변경하는 Job
            JobDetail deliveryJob = JobBuilder.newJob(UpdateOrderStatusJob.class)
                    .withIdentity("deliveryJob-" + orderId)
                    .usingJobData("orderId", orderId)
                    .usingJobData("newStatus", "배송중")
                    .storeDurably()
                    .build();
            // 1일 뒤 -> 배송중 상태로 변경하는 Trigger
            Trigger deliveryTrigger = TriggerBuilder.newTrigger()
                    .forJob(deliveryJob)
                    .withIdentity("deliveryTrigger-" + orderId)
                    .startAt(Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant()))
//                    .startAt(Date.from(LocalDateTime.now().plusMinutes(3).atZone(ZoneId.systemDefault()).toInstant())) // 테스트용
                    .build();
            // [배송중] JobDetail과 Trigger로 스케줄링
            scheduler.scheduleJob(deliveryJob, deliveryTrigger);
            System.out.println("[배송중]으로 변경하는 Job이 등록되었습니다. Order ID: " + orderId);

            // 2일 뒤 -> 배송 완료 상태로 변경하는 Job
            JobDetail arriveJob = JobBuilder.newJob(UpdateOrderStatusJob.class)
                    .withIdentity("arriveJob-" + orderId)
                    .usingJobData("orderId", orderId)
                    .usingJobData("newStatus", "배송 완료")
                    .storeDurably()
                    .build();
            // 2일 뒤 -> 배송 완료 상태로 변경하는 Trigger
            Trigger arriveTrigger = TriggerBuilder.newTrigger()
                    .forJob(arriveJob)
                    .withIdentity("arriveTrigger-" + orderId)
                    .startAt(Date.from(LocalDateTime.now().plusDays(2).atZone(ZoneId.systemDefault()).toInstant()))
//                    .startAt(Date.from(LocalDateTime.now().plusMinutes(6).atZone(ZoneId.systemDefault()).toInstant())) // 테스트용
                    .build();
            // [배송 완료] JobDetail과 Trigger로 스케줄링
            scheduler.scheduleJob(arriveJob, arriveTrigger);
            System.out.println("[배송 완료]로 변경하는 Job이 등록되었습니다. Order ID: " + orderId);
        } catch (Exception e) {
            throw new RuntimeException("Quartz Job 등록 중 오류 발생");
        }
    }
}

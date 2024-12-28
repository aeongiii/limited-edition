package com.sparta.paymentservice.service;

import com.sparta.paymentservice.dto.OrderItemResponse;
import com.sparta.paymentservice.dto.OrderRequest;
import com.sparta.paymentservice.dto.OrderResponse;
import com.sparta.paymentservice.entity.*;
import com.sparta.paymentservice.repository.*;
import com.sparta.paymentservice.util.ReturnCompletionJob;
import com.sparta.paymentservice.util.UpdateOrderStatusJob;
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

    // 1. 주문하기
    @Transactional
    public OrderResponse createOrder(String email, List<OrderRequest> orderItems) {
        User user = validateUser(email); // 사용자 검증
        Orders order = new Orders(user, "주문 완료", 0); // order 객체 생성, 총 금액 0으로 초기화
        orderRepository.save(order);

        List<OrderDetail> orderDetails = processEachOrder(order, orderItems); // 상품 하나하나 주문처리
        int totalAmount = calculateTotalAmount(orderDetails); // 총 가격 구하기
        order.setTotalAmount(totalAmount); // 총 금액 업데이트
        orderDetailRepository.saveAll(orderDetails);

        // [주문 완료] -> [배송중] -> [배송 완료] 상태 자동 업데이트하는 job 생성
        scheduleOrderStatusJobs(order.getId());
        wishlistRepository.deleteAllByUserId(user.getId()); // 주문 완료 후 위시리스트에서 삭제
        List<OrderItemResponse> orderItemResponses = createOrderDetailList(orderDetails); // 반환할 주문 상세 response 생성

        return new OrderResponse(order.getId(), order.getStatus(), totalAmount, orderItemResponses);
    }

    // 2. 주문내역 확인
    @Transactional
    public List<OrderResponse> getOrderDeatils(String email) {
        // 특정 조건에 맞는 데이터를 List<OrderResponse>로 반환함
        return getOrderResposeList(email, List.of("주문 완료", "배송중", "배송 완료"));
    }

    // 3. 취소, 반품내역 목록 조회
    @Transactional
    public List<OrderResponse> getCancelAndReturn(String email) {
        // 특정 조건에 맞는 데이터를 List<OrderResponse>로 반환함
        return getOrderResposeList(email, List.of("취소 완료", "반품 신청", "반품 완료"));
    }

    // 주문취소 - 주문한 상품 하나하나 찾아서 재고 복구
    private void restoreEachStock(Long orderId) {
        List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrdersId(orderId);
        for (OrderDetail detail : orderDetails) {
            Product product = productRepository.findById(detail.getProductSnapshot().getId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));
            product.setStockQuantity(product.getStockQuantity() + detail.getQuantity());
            productRepository.save(product);
        }
    }

    // 취소, 반품 - 주문상태 변경
    private void changeOrderStatus(Orders orders, String statusName) {
        orders.setStatus(statusName);
        orders.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(orders);
    }

    // 4. 주문 취소하기
    @Transactional
    public String cancelOrder(String email, Long orderId) {
        User user = validateUser(email); // 사용자 검증
        Orders orders = validateOrder(orderId); // 주문 검증
        // 취소 가능 상태인지
        if (!"주문 완료".equals(orders.getStatus())) {
            throw new IllegalArgumentException("주문을 취소할 수 없는 상태입니다.");
        }
        restoreEachStock(orderId); // 취소한 상품마다 재고 복구
        changeOrderStatus(orders, "취소 완료"); // 주문 상태 변경

        return "취소 완료";
    }

    // 5. 반품 처리
    @Transactional
    public String returnOrder(String email, Long orderId) {
        User user = validateUser(email); // 사용자 검증
        Orders orders = validateOrder(orderId); // 주문 검증
        // 반품 가능 상태인지
        if (!"배송 완료".equals(orders.getStatus())) {
            throw new IllegalArgumentException("배송 완료된 상품만 반품 가능합니다.");
        }
        // 배송받은지 1일 이내인지
        if (orders.getUpdatedAt().plusDays(1).isBefore(LocalDateTime.now())) // updatedAt + 1일이 현재 시간보다 이전인지 확인
            throw new IllegalArgumentException("배송 완료 후 24시간이 지나 반품할 수 없습니다.");
        changeOrderStatus(orders, "반품 신청"); // 주문 상태 변경
        scheduleReturnCompletionJob(orderId); // Quartz Job 등록 (24시간 후 주문상태 변경, 재고 복구)

        return "반품 신청";
    }




    // ========================

    // 주문, 취소 반품 - 사용자 검증
    private User validateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));
    }

    // 취소, 반품 - 주문 검증
    private Orders validateOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
    }

    // 주문 - 상품 하나하나 주문처리
    private List<OrderDetail> processEachOrder(Orders order, List<OrderRequest> orderItems) {
        List<OrderDetail> orderDetails = new ArrayList<>();

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
        }
        return orderDetails;
    }

    // 주문 - 총 가격 구하기
    private int calculateTotalAmount(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
                .mapToInt(OrderDetail::getSubtotalAmount)
                .sum();
    }

    // 주문 - 반환할 '주문 상세' response 생성
    private List<OrderItemResponse> createOrderDetailList(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
                .map(detail -> new OrderItemResponse(
                        detail.getProductSnapshot().getProductId(),
                        detail.getProductSnapshot().getName(),
                        detail.getQuantity(),
                        detail.getProductSnapshot().getPrice(),
                        detail.getSubtotalAmount()
                ))
                .toList();
    }

    // 특정 조건(statusName)으로 필터링한 주문 데이터를 orderResponse 리스트로 반환
    private List<OrderResponse> getOrderResposeList(String email, List<String> statusName) {
        // 해당 사용자의 모든 주문내용 가져오기
        List<Orders> orders = orderRepository.findAllByUserEmailAndStatusIn(email, statusName);
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

    // 반품 신청 시 Quartz Job 등록
    private void scheduleReturnCompletionJob(Long orderId) {
        try {
            // 1일 뒤 [반품 완료] 상태로 변경하는 Job, Trigger 생성
            JobDetail jobDetail = createJob_return("returnCompletionJob-", orderId);
            Trigger trigger = createTrigger(jobDetail, "returnCompletionTrigger-", orderId, 1);

            // JobDetail과 Trigger로 스케줄링
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw new RuntimeException("Quartz Job 등록 중 오류 발생", e);
        }
    }

    // 주문 시 Quartz Job 등록
    private void scheduleOrderStatusJobs(Long orderId) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            // 1일 뒤 [배송중] 상태로 변경하는 Job, Trigger 생성
            JobDetail deliveryJob = createJob_delivery("deliveryJob-", "배송중", orderId);
            Trigger deliveryTrigger = createTrigger(deliveryJob, "deliveryTrigger-", orderId, 1);

            // [배송중] JobDetail과 Trigger로 스케줄링
            scheduler.scheduleJob(deliveryJob, deliveryTrigger);
            System.out.println("[배송중]으로 변경하는 Job이 등록되었습니다. Order ID: " + orderId);

            // 2일 뒤 [배송 완료] 상태로 변경하는 Job, Trigger 생성
            JobDetail arriveJob = createJob_delivery("arriveJob-", "배송 완료", orderId);
            Trigger arriveTrigger = createTrigger(arriveJob, "arriveTrigger-", orderId, 2);

            // [배송 완료] JobDetail과 Trigger로 스케줄링
            scheduler.scheduleJob(arriveJob, arriveTrigger);
            System.out.println("[배송 완료]로 변경하는 Job이 등록되었습니다. Order ID: " + orderId);
        } catch (Exception e) {
            throw new RuntimeException("Quartz Job 등록 중 오류 발생");
        }
    }

    // [반품 완료] 상태로 변경하는 Job 생성
    private JobDetail createJob_return(String IdentityName, Long orderId) {
        return JobBuilder.newJob(ReturnCompletionJob.class) // ReturnCompletionJob 클래스를 job으로 설정
                .withIdentity(IdentityName + orderId) // 고유식별자 이름 설정
                .usingJobData("orderId", orderId) // 주문 ID 포함
                .storeDurably()
                .build();
    }

    // [배송중] 또는 [배송완료] 상태로 변경하는 Job 생성
    private JobDetail createJob_delivery(String IdentityName, String JobDataValue, Long orderId) {
        return JobBuilder.newJob(UpdateOrderStatusJob.class)
                .withIdentity(IdentityName + orderId)
                .usingJobData("orderId", orderId)
                .usingJobData("newStatus", JobDataValue)
                .storeDurably()
                .build();
    }

    // [반품완료],[배송중],[배송완료] 상태로 변경하는 Trigger 생성
    private Trigger createTrigger(JobDetail jobName, String IdentityName, Long orderId, int days) {
        return TriggerBuilder.newTrigger()
                .forJob(jobName)
                .withIdentity(IdentityName + orderId)
                .startAt(Date.from(LocalDateTime.now().plusDays(days).atZone(ZoneId.systemDefault()).toInstant())) // 1일, 2일로 수행
//                    .startAt(Date.from(LocalDateTime.now().plusMinutes(days*3).atZone(ZoneId.systemDefault()).toInstant())) // 테스트용 : 3분, 6분으로 실행됨
                .build();
    }
}

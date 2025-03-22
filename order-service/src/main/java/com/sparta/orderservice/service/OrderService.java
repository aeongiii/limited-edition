package com.sparta.orderservice.service;

import com.sparta.common.dto.*;
import com.sparta.common.exception.*;
import com.sparta.common.kafkaDto.StockUpdateFailedEvent;
import com.sparta.common.kafkaDto.StockUpdatedEvent;
import com.sparta.orderservice.client.ProductServiceClient;
import com.sparta.orderservice.client.UserServiceClient;
import com.sparta.orderservice.client.WishlistServiceClient;
import com.sparta.orderservice.entity.OrderDetail;
import com.sparta.orderservice.entity.Orders;
import com.sparta.orderservice.repository.OrderDetailRepository;
import com.sparta.orderservice.repository.OrderRepository;
import com.sparta.orderservice.util.ReturnCompletionJob;
import com.sparta.orderservice.util.UpdateOrderStatusJob;
import org.quartz.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final SchedulerFactoryBean schedulerFactoryBean;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final WishlistServiceClient wishlistServiceClient;
    private final RedissonClient redissonClient;
    private final KafkaProducer kafkaProducer;


    public OrderService(OrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        SchedulerFactoryBean schedulerFactoryBean,
                        UserServiceClient userServiceClient,
                        ProductServiceClient productServiceClient,
                        WishlistServiceClient wishlistServiceClient,
                        RedissonClient redissonClient,
                        KafkaProducer kafkaProducer) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.schedulerFactoryBean = schedulerFactoryBean;
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
        this.wishlistServiceClient = wishlistServiceClient;
        this.redissonClient = redissonClient;
        this.kafkaProducer = kafkaProducer;
    }

    // 1. 주문하기
    @Transactional
    public OrderResponse createOrder(String email, List<OrderRequest> orderItems) {
        UserResponse userResponse = userServiceClient.getUserEmail(email);

        Orders order = new Orders(userResponse.getId(), "주문완료", 0);
        orderRepository.save(order);

        List<OrderDetail> orderDetails = new ArrayList<>();
        int totalAmount = 0;

        orderItems.sort(Comparator.comparing(OrderRequest::getProductId));
        // 상품별 락 생성
        List<RLock> locks = new ArrayList<>();
        for (OrderRequest item : orderItems) {
            locks.add(redissonClient.getLock("order:lock:" + item.getProductId()));
            System.out.println("상품별 Lock 객체 생성 중. productId : " + item.getProductId());
        }

        // RMultiLock - 여러 락을 한번에 획득
        RLock[]  lockArray = locks.toArray(new RLock[0]);
        RLock multiLock = redissonClient.getMultiLock(lockArray);
        System.out.println("RMultiLock 획득 완료");

        boolean acquired = false;

        try {
            acquired = multiLock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new OrderCreateException("락 획득 실패 - 주문 생성 실패");
            }
            orderDetails = processEachOrder(order, orderItems);
            totalAmount = calculateTotalAmount(orderDetails);
            order.setTotalAmount(totalAmount);
            orderDetailRepository.saveAll(orderDetails);

            for (OrderRequest item : orderItems) {
                StockUpdatedEvent event = new StockUpdatedEvent(order.getId(), item.getProductId(), item.getQuantity(), "DECREASE");
                kafkaProducer.sendStockUpdatedEvent(event);
            }

            // [주문 완료] -> [배송중] -> [배송 완료] 상태 자동 업데이트하는 job 생성
            scheduleOrderStatusJobs(order.getId());
            deleteWishlistItems(userResponse.getId(), orderItems);
            List<OrderItemResponse> orderItemResponses = createOrderDetailList(orderDetails);

            return new OrderResponse(order.getId(), order.getStatus(), totalAmount, orderItemResponses);

        } catch (InsufficientStockException e) {
            for (OrderRequest item : orderItems) {
                StockUpdateFailedEvent failedEvent = new StockUpdateFailedEvent(
                        order.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        e.getMessage()
                );
                kafkaProducer.sendStockUpdateFailedEvent(failedEvent);
                System.out.println("OrderService.createOrder - StockUpdateFailedEvent 전송 완료");
            }
            throw e;
        } catch (InterruptedException e) {
            throw new OrderCreateException("락 대기 중 인터럽트 발생");
        } finally {
            if (acquired) {
                multiLock.unlock();
                System.out.println("주문 API에서 락을 해제했습니다.");
            }
        }

    }

    // 주문 - 상품 하나하나 주문처리
    private List<OrderDetail> processEachOrder(Orders order, List<OrderRequest> orderItems) {
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (OrderRequest item : orderItems) {
            ProductResponse productResponse = productServiceClient.getProductById(item.getProductId());
            if(productResponse == null) {
                throw new ProductNotFoundException("상품을 찾을 수 없습니다.");
            }

            // 숨김 처리된 상품인지 확인
            if (!productResponse.isVisible()) {
                throw new IllegalArgumentException("상품이 숨김 처리되어 주문할 수 없습니다. 상품 ID: " + item.getProductId());
            }

            if (productResponse.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("재고가 부족합니다. (남은 재고: " + productResponse.getStockQuantity() + ")");
            }
            ProductSnapshotResponse snapshot = productServiceClient.createProductSnapshot(productResponse);

            int subtotalAmount = item.getQuantity() * productResponse.getPrice();
            OrderDetail orderDetail = new OrderDetail(order, snapshot.getId(), item.getQuantity(), subtotalAmount);
            orderDetails.add(orderDetail);
        }
        return orderDetails;
    }


    // 2. 주문내역 확인
    @Transactional
    public List<OrderResponse> getOrderDeatils(String email) {
        List<String> statusName = List.of("주문중", "주문완료", "배송중", "배송완료");
        return getOrderResposeList(email, statusName);
    }

    // 3. 취소, 반품내역 목록 조회
    @Transactional
    public List<OrderResponse> getCancelAndReturn(String email) {
        List<String> statusName = List.of("취소완료", "반품신청", "반품완료");
        return getOrderResposeList(email, statusName);
    }

    // 4. 주문 취소하기
    @Transactional
    public String cancelOrder(String email, Long orderId) {
        UserResponse userResponse = userServiceClient.getUserEmail(email);
        Orders orders = validateOrder(orderId);
        if (!"주문완료".equals(orders.getStatus())) {
            throw new IllegalArgumentException("주문을 취소할 수 없는 상태입니다.");
        }
        restoreEachStock(orderId);
        changeOrderStatus(orders, "취소완료");

        return "취소완료";
    }

    // 5. 반품 처리
    @Transactional
    public String returnOrder(String email, Long orderId) {
        UserResponse userResponse = userServiceClient.getUserEmail(email);
        Orders orders = validateOrder(orderId);
        if (!"배송완료".equals(orders.getStatus())) {
            throw new IllegalArgumentException("배송 완료된 상품만 반품 가능합니다.");
        }
        if (orders.getUpdatedAt().plusDays(1).isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("배송 완료 후 24시간이 지나 반품할 수 없습니다.");
        changeOrderStatus(orders, "반품신청");
        // Quartz Job 등록 (24시간 후 주문상태 변경, 재고 복구)
        scheduleReturnCompletionJob(orderId);

        return "반품신청";
    }




    // ========================

    // 주문한 상품은 위시리스트에서 삭제
    private void deleteWishlistItems(Long userId, List<OrderRequest> orderItems) {
        List<Long> productIds = new ArrayList<>();
        for (OrderRequest item : orderItems) {
            productIds.add(item.getProductId());
        }
        wishlistServiceClient.deleteWishlistItems(userId, productIds);
    }

    // 취소, 반품 - 주문 검증
    private Orders validateOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문 정보를 찾을 수 없습니다."));
    }

    // 주문 - 총 가격 구하기
    private int calculateTotalAmount(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
                .mapToInt(OrderDetail::getSubtotalAmount)
                .sum();
    }

    // 주문 - 반환할 '주문 상세' response 생성
    private List<OrderItemResponse> createOrderDetailList(List<OrderDetail> orderDetails) {
        List<OrderItemResponse> orderItemResponses = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetails) {
            Long productSnapshotId = orderDetail.getProductSnapshotId();
            ProductSnapshotResponse productSnapshot = productServiceClient.getProductSnapshotById(productSnapshotId);
            if (productSnapshot == null) {
                throw new ProductSnapshotNotFoundException("ProductSnapshot 정보를 찾을 수 없습니다. ProductSnapshotId : " + productSnapshotId);
            }            Long productId = productSnapshot.getProductResponse().getId();
            String productName = productSnapshot.getName();
            int quantity = orderDetail.getQuantity();
            int productPrice = productSnapshot.getPrice();
            int subtotalAmount = orderDetail.getSubtotalAmount();

            OrderItemResponse orderItemResponse = new OrderItemResponse(productId, productName, quantity, productPrice, subtotalAmount);
            orderItemResponses.add(orderItemResponse);
        }
        return orderItemResponses;
    }

    // 특정 조건(statusName)으로 필터링한 주문 데이터를 orderResponse 리스트로 반환
    private List<OrderResponse> getOrderResposeList(String email, List<String> statusName) {
        // 해당 사용자의 모든 주문내용 가져오기
        UserResponse userResponse = userServiceClient.getUserEmail(email);
        Long userId = userResponse.getId();
        List<Orders> ordersList = orderRepository.findAllByUserIdAndStatusIn(userId, statusName);
        List<OrderResponse> orderResponses = new ArrayList<>();

        for (Orders order : ordersList) {
            List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrdersId(order.getId());
            List<OrderItemResponse> orderItemResponses = new ArrayList<>();

            for (OrderDetail orderDetail : orderDetails) {
                Long productSnapshotId = orderDetail.getProductSnapshotId();
                ProductSnapshotResponse productSnapshot = productServiceClient.getProductSnapshotById(productSnapshotId);
                if (productSnapshot == null) {
                    throw new IllegalArgumentException("ProductSnapshot 정보를 찾을 수 없습니다. ProductSnapshotId : " + productSnapshotId);
                }
                OrderItemResponse orderItemResponse = new OrderItemResponse(
                        productSnapshot.getProductResponse().getId(),
                        productSnapshot.getName(),
                        orderDetail.getQuantity(),
                        productSnapshot.getPrice(),
                        orderDetail.getSubtotalAmount()
                );
                orderItemResponses.add(orderItemResponse);
            }
            OrderResponse orderResponse = new OrderResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    orderItemResponses
            );
            orderResponses.add(orderResponse);
        }
        return orderResponses;
    }

    // 주문취소 - 주문한 상품 하나하나 찾아서 재고 복구
    private void restoreEachStock(Long orderId) {
        List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrdersId(orderId);
        for (OrderDetail detail : orderDetails) {
            Long productSnapshotId = detail.getProductSnapshotId();
            ProductSnapshotResponse productSnapshot = productServiceClient.getProductSnapshotById(productSnapshotId);
            Long productId = productSnapshot.getProductResponse().getId();
            // 현재 상품 재고 가져오기
            ProductResponse product = productServiceClient.getProductById(productId);
            int restoreQuantity = product.getStockQuantity() + detail.getQuantity();
        }
    }

    // 취소, 반품 - 주문상태 변경
    private void changeOrderStatus(Orders orders, String statusName) {
        orders.setStatus(statusName);
        orders.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(orders);
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
            JobDetail arriveJob = createJob_delivery("arriveJob-", "배송완료", orderId);
            Trigger arriveTrigger = createTrigger(arriveJob, "arriveTrigger-", orderId, 2);

            // [배송 완료] JobDetail과 Trigger로 스케줄링
            scheduler.scheduleJob(arriveJob, arriveTrigger);
            System.out.println("[배송완료]로 변경하는 Job이 등록되었습니다. Order ID: " + orderId);
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

    // 아이디로 주문 찾기
    public OrderResponse getOrderById(Long orderId) {
        Orders order = validateOrder(orderId);
        List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrdersId(orderId);
        List<OrderItemResponse> orderItemResponses = createOrderDetailList(orderDetails);
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                orderItemResponses
        );
    }

    // 결제 이탈 시 주문데이터 삭제
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOrderWithDetails(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null) {
            return;
        }
        orderDetailRepository.deleteByOrdersId(orderId);
        orderRepository.delete(order);
        System.out.println("주문 데이터 삭제 완료");
    }

    // 최신 5개 조회
    public List<RecentOrderResponse> getTop5OrdersByEmail(String email) {
        UserResponse userResponse = userServiceClient.getUserEmail(email);
        if (userResponse == null) {
            throw new IllegalArgumentException("해당 이메일을 가진 사용자가 존재하지 않습니다.");
        }

        List<Orders> ordersList = orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(userResponse.getId());
        return ordersList.stream()
                .map(order -> new RecentOrderResponse(
                        order.getId(),
                        order.getStatus(),
                        order.getCreatedAt().toString()
                )).collect(Collectors.toList());
    }
}

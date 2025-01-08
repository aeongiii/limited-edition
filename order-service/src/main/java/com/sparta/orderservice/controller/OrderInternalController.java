package com.sparta.orderservice.controller;

import com.sparta.common.dto.OrderRequest;
import com.sparta.common.dto.OrderResponse;
import com.sparta.common.dto.RecentOrderResponse;
import com.sparta.orderservice.entity.Orders;
import com.sparta.orderservice.repository.OrderDetailRepository;
import com.sparta.orderservice.repository.OrderRepository;
import com.sparta.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/order")
public class OrderInternalController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;


    public OrderInternalController(OrderService orderService, OrderRepository orderRepository, OrderDetailRepository orderDetailRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    @PostMapping("/order")     // payment의 OrderServiceClient에서 들어오는 email 파라미터 괜찮은지?
    public OrderResponse createOrder(@RequestHeader("X-User-Email") String email, @RequestBody List<OrderRequest> orderItems) {
        System.out.println("OrderInternalController.createOrder로 요청이 들어왔습니다.");
        OrderResponse order = orderService.createOrder(email, orderItems);
        System.out.println("Order 생성 완료: " + order);
        return order;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    // 최근 주문목록 5개 가져오기
    @GetMapping("/top5/{userId}")
    public ResponseEntity<List<RecentOrderResponse>> getTop5OrdersByUserId(@PathVariable Long userId) {
        List<Orders> ordersList = orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        List<RecentOrderResponse> recentOrderResponseList = ordersList.stream()
                .map(Orders -> {
                    return new RecentOrderResponse(
                            Orders.getId(),
                            Orders.getStatus(),
                            Orders.getCreatedAt().toString()
                    );
                }).collect(Collectors.toList());
        return ResponseEntity.ok(recentOrderResponseList);
    }

    // 결제 이탈 시 주문데이터 삭제
    @DeleteMapping("/delete/{orderId}/{email}")
    public ResponseEntity<String> deleteOrder(@PathVariable("orderId") Long orderId, @PathVariable("email") String email) {
        orderService.deleteOrderWithDetails(orderId);
        return ResponseEntity.ok("주문이 성공적으로 삭제되었습니다.");
    }
}

package com.sparta.orderservice.controller;

import com.sparta.common.dto.OrderResponse;
import com.sparta.common.dto.OrderRequest;
import com.sparta.common.dto.RecentOrderResponse;
import com.sparta.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 주문 api : '주문중' 상태로 주문데이터 저장
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(
            @RequestBody List<OrderRequest> orderRequest,
            @RequestHeader(name = "X-User-Email", required = false) String email) {
        try {
            // 주문 로직
            OrderResponse orderResponse = orderService.createOrder(email, orderRequest);
            return ResponseEntity.ok(orderResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("주문 처리 중 오류가 발생했습니다.");
        }
    }

    // 주문 목록 조회
    @GetMapping("/order")
    public ResponseEntity<?> getOrderDetails(@RequestHeader(name = "X-User-Email", required = false) String email) {
        // 주문 목록 조회
        List<OrderResponse> orderResponse = orderService.getOrderDeatils(email);
        return ResponseEntity.ok(orderResponse);
    }

    // 취소/반품 목록 조회
    @GetMapping("/order/cancel-and-return")
    public ResponseEntity<?> getCancelAndReturn(@RequestHeader(name = "X-User-Email", required = false) String email) {
        // 주문 목록 조회
        List<OrderResponse> orderResponse = orderService.getCancelAndReturn(email);
        return ResponseEntity.ok(orderResponse);
    }

    // 주문 취소하기
    @PutMapping ("order/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader(name = "X-User-Email", required = false) String email) {
        try {
            // 주문 취소
            String status = orderService.cancelOrder(email, orderId);
            return ResponseEntity.ok(Map.of("orderId", orderId, "status", status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 반품 신청
    @PutMapping("order/{orderId}/return")
    public ResponseEntity<?> returnOrder(
            @PathVariable Long orderId,
            @RequestHeader(name = "X-User-Email", required = false) String email) {
        // 주문 취소
        try {
            String status = orderService.returnOrder(email, orderId);
            return ResponseEntity.ok(Map.of("orderId", orderId, "status", status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 최신 5개
    @GetMapping("/order/top5")
    public ResponseEntity<?> getRecentOrders(@RequestHeader(name = "X-User-Email", required = false) String email) {
        List<RecentOrderResponse> orderResponse = orderService.getTop5OrdersByEmail(email);
        return ResponseEntity.ok(orderResponse);
    }

}

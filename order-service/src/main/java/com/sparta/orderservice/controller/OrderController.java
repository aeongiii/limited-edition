package com.sparta.orderservice.controller;

import com.sparta.orderservice.dto.OrderRequest;
import com.sparta.orderservice.dto.OrderResponse;
import com.sparta.orderservice.security.JwtTokenProvider;
import com.sparta.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;

    public OrderController(OrderService orderService, JwtTokenProvider jwtTokenProvider) {
        this.orderService = orderService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 주문하기
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(
            @RequestBody List<OrderRequest> orderRequest,
            @CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 주문
        try {
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
    public ResponseEntity<?> getOrderDetails(@CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 주문 목록 조회
        List<OrderResponse> orderResponse = orderService.getOrderDeatils(email);
        return ResponseEntity.ok(orderResponse);
    }

    // 취소/반품 목록 조회
    @GetMapping("/order/cancel-and-return")
    public ResponseEntity<?> getCancelAndReturn(@CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 주문 목록 조회
        List<OrderResponse> orderResponse = orderService.getCancelAndReturn(email);
        return ResponseEntity.ok(orderResponse);
    }

    // 주문 취소하기
    @PutMapping ("order/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 주문 취소
        try {
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
            @CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 주문 취소
        try {
            String status = orderService.returnOrder(email, orderId);
            return ResponseEntity.ok(Map.of("orderId", orderId, "status", status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

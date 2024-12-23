package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.OrderRequest;
import com.sparta.limited_edition.dto.OrderResponse;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    public OrderController(OrderService orderService, JwtTokenProvider jwtTokenProvider) {
        this.orderService = orderService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;

    // 주문하기
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(
            @RequestBody List<OrderRequest> orderRequest,
            @CookieValue(name = "accessToken", required = false) String accessToken) {

        // Access Token 검증
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }

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
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }

        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 주문 목록 조회
        List<OrderResponse> orderResponse = orderService.getOrderDeatils(email);

        return ResponseEntity.ok(orderResponse);
    }

    // 주문 취소하기
    @PutMapping ("order/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }

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
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }

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

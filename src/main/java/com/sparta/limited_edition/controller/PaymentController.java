package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.OrderResponse;
import com.sparta.limited_edition.dto.PaymentResponse;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.OrderService;
import com.sparta.limited_edition.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaymentController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final JwtTokenProvider jwtTokenProvider;

    public PaymentController(OrderService orderService, PaymentService paymentService, JwtTokenProvider jwtTokenProvider) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 결제하기
    @PostMapping("/payment/{orderId}")
    public ResponseEntity<?> payment(@PathVariable Long orderId,
    @CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }
        // 이메일 추출
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        // 주문 목록 조회
        List<OrderResponse> orderResponse = orderService.getOrderDeatils(email);
        // 결제
        try {
            PaymentResponse response = paymentService.payment(email, orderId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}

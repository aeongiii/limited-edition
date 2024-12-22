package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.OrderRequest;
import com.sparta.limited_edition.dto.OrderResponse;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    public OrderController(OrderService orderService, JwtTokenProvider jwtTokenProvider) {
        this.orderService = orderService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private final OrderService orderService;
    private final JwtTokenProvider jwtTokenProvider;


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
}

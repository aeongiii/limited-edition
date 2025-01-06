package com.sparta.paymentservice.controller;

import com.sparta.paymentservice.dto.PaymentResponse;
import com.sparta.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 결제하기
    @PostMapping("/payment/{orderId}")
    public ResponseEntity<?> payment(@PathVariable Long orderId,
                                     @RequestHeader(name = "X-User-Email", required = false) String email) {
        try {
            PaymentResponse response = paymentService.payment(email, orderId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (feign.FeignException e) {
            // FeignException 발생 시, 메시지를 사용자에게 반환
            return ResponseEntity.status(e.status()).body(e.contentUTF8());
        } catch (Exception e) {
            // 기타 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 중 오류가 발생했습니다.");
        }
    }
}

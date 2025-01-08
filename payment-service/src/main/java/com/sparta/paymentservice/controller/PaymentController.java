package com.sparta.paymentservice.controller;

import com.sparta.common.dto.PaymentResponse;
import com.sparta.common.exception.PaymentProcessException;
import com.sparta.paymentservice.service.PaymentFacadeService;
import com.sparta.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentFacadeService paymentFacadeService;

    public PaymentController(PaymentService paymentService, PaymentFacadeService paymentFacadeService) {
        this.paymentService = paymentService;
        this.paymentFacadeService = paymentFacadeService;
    }

    // 결제 진입 api : '결제중'으로 결제데이터 저장
    @PostMapping("/payment/{orderId}")
    public ResponseEntity<?> startPayment(@PathVariable Long orderId,
                                     @RequestHeader(name = "X-User-Email", required = false) String email) {
        try {
            PaymentResponse response = paymentService.startPayment(email, orderId);
            return ResponseEntity.status(HttpStatus.OK).body("결제 프로세스에 진입했습니다.");
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

    // 결제 완료 api : '결제완료'로 변경
    @PutMapping("/payment/{orderId}")
    public ResponseEntity<?> endPayment(@PathVariable Long orderId,
                                        @RequestHeader(name = "X-User-Email", required = false) String email) {
        PaymentResponse response = paymentService.endPayment(email, orderId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 결제 프로세스 전체 진행
    @PostMapping("/payment/process")
    public ResponseEntity<?> processPayment(
            @RequestHeader(name = "X-User-Email", required = false) String email,
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") int quantity) {
        try {
            paymentFacadeService.paymentProcess(email, productId, quantity);
            return ResponseEntity.status(HttpStatus.OK).body("결제 프로세스 완료");
        } catch (PaymentProcessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getErrorMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 프로세스 중 오류 발생 : " + e.getMessage());
        }
    }
}

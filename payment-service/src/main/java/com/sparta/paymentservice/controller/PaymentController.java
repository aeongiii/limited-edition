package com.sparta.paymentservice.controller;

import com.sparta.common.dto.PaymentResponse;
import com.sparta.common.exception.PaymentProcessException;
import com.sparta.paymentservice.service.OrchestratorService;
import com.sparta.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final OrchestratorService orchestratorService;

    public PaymentController(PaymentService paymentService, OrchestratorService orchestratorService) {
        this.paymentService = paymentService;
        this.orchestratorService = orchestratorService;
    }

    // 결제 진입 api
    @PostMapping("/payment/{orderId}")
    public ResponseEntity<?> startPayment(@PathVariable Long orderId,
                                     @RequestHeader(name = "X-User-Email", required = false) String email) {
        try {
            PaymentResponse response = paymentService.startPayment(email, orderId);
            return ResponseEntity.status(HttpStatus.OK).body("결제 프로세스에 진입했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (PaymentProcessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("사용자가 결제를 취소했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 중 서버 오류가 발생했습니다.");
        }
    }

    // 결제 완료 api
    @PutMapping("/payment/{orderId}")
    public ResponseEntity<?> endPayment(@PathVariable Long orderId,
                                        @RequestHeader(name = "X-User-Email", required = false) String email) {
        try {
            PaymentResponse response = paymentService.endPayment(email, orderId);
            return ResponseEntity.status(HttpStatus.OK).body("결제가 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (PaymentProcessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("한도 초과로 결제에 실패했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 중 서버 오류가 발생했습니다.");
        }

    }

    // 결제 프로세스 전체 진행
    @PostMapping("/payment/process")
    public ResponseEntity<?> processPayment(
            @RequestHeader(name = "X-User-Email", required = false) String email,
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") int quantity) {
        try {
            orchestratorService.startSaga(email, productId, quantity);
            return ResponseEntity.status(HttpStatus.OK).body("결제 프로세스 완료");
        } catch (PaymentProcessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 프로세스 중 오류 발생 : " + e.getMessage());
        }
    }
}

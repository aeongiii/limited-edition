package com.sparta.paymentservice.service;

import com.sparta.common.dto.OrderResponse;
import com.sparta.common.exception.OrderNotFoundException;
import com.sparta.common.exception.PaymentProcessException;
import com.sparta.paymentservice.client.OrderServiceClient;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {

    private final OrderServiceClient orderServiceClient;
    private final PaymentService paymentService;

    public OrchestratorService(OrderServiceClient orderServiceClient,
                               PaymentService paymentService) {
        this.orderServiceClient = orderServiceClient;
        this.paymentService = paymentService;
    }

    public void startSaga(String email, Long productId, int quantity) {
        Long orderId = null;
        try {
            orderId = paymentService.createOrder(email, productId, quantity); // 1. 주문 API 호출
            paymentService.startPayment(email, orderId); // 2. 결제 진입 API 호출
            paymentService.endPayment(email, orderId); // 3. 결제 완료 API 호출
        } catch (PaymentProcessException e) {
            rollbackSaga(email, orderId, productId, quantity);
            throw new PaymentProcessException("결제 프로세스 중 문제가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            rollbackSaga(email, orderId, productId, quantity);
            throw new RuntimeException("결제 프로세스 중 문제가 발생했습니다.", e);
        }
    }

    public void rollbackSaga(String email, Long orderId, Long productId, int quantity) {
        try {
            OrderResponse order = null;
            try {
                order = orderServiceClient.getOrderById(orderId, email);
            } catch (Exception e) {
                throw new OrderNotFoundException("주문 정보를 가져오는 중 오류 발생: " + e.getMessage());
            }

            try {
                if (paymentService.existsByOrderId(orderId)) {
                    paymentService.deletePaymentByOrderId(orderId);
                }
            } catch (Exception e) {
                throw new PaymentProcessException("결제 데이터를 삭제하는 중 오류 발생: " + e.getMessage());
            }

            try {
                if (order != null) {
                    orderServiceClient.deleteOrder(orderId, email);
                }
            } catch (Exception e) {
                throw new PaymentProcessException("주문 데이터를 삭제하는 중 오류 발생: " + e.getMessage());
            }

            try {
                if (order != null) {
                    paymentService.restoreStock(productId, quantity);
                }
            } catch (Exception e) {
                throw new PaymentProcessException("재고 복구 중 오류 발생: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("롤백 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
}

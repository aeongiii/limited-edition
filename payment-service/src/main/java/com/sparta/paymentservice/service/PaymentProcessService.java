package com.sparta.paymentservice.service;

import com.sparta.common.dto.OrderRequest;
import com.sparta.common.dto.OrderResponse;
import com.sparta.common.dto.PaymentResponse;
import com.sparta.common.exception.PaymentProcessException;
import com.sparta.paymentservice.client.OrderServiceClient;
import com.sparta.paymentservice.client.ProductServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentProcessService {

    private final OrderServiceClient orderServiceClient;
    private final ProductServiceClient productServiceClient;
    private final PaymentService paymentService;

    public PaymentProcessService(OrderServiceClient orderServiceClient, ProductServiceClient productServiceClient, PaymentService paymentService) {
        this.orderServiceClient = orderServiceClient;
        this.productServiceClient = productServiceClient;
        this.paymentService = paymentService;
    }

    @Transactional
    public void paymentProcess(String email, Long productId, int quantity) {
        Long orderId = null;
        try {

            // 1. 주문 API 호출
            orderId = createOrder(email, productId, quantity);
            System.out.println("결제 프로세스 - 1. 주문 API 완료. Order ID: " + orderId);

            // 2. 결제 진입 API 호출
            PaymentResponse payment = paymentService.startPayment(email, orderId);
            System.out.println("결제 프로세스 - 2. 결제 진입 API 완료.");

            // 3. 결제 완료 API 호출
            paymentService.endPayment(email, orderId);
            System.out.println("결제 프로세스 - 3. 결제 완료 API 완료.");

            System.out.println("결제 프로세스 완료. Order ID: " + orderId);

        } catch (PaymentProcessException e) {
            // 4. 롤백 -> 재고 복구, 주문 데이터 삭제
            rollbackProcess(email, orderId, productId, quantity);
            throw new RuntimeException(e); // 메시지 구체화
        } catch (Exception e) {
            rollbackProcess(email, orderId, productId, quantity);
            throw new RuntimeException("결제 프로세스 중 문제가 발생했습니다.", e);
        }
    }

    // ==================

    // 주문 API 호출
    private Long createOrder(String email, Long productId, int quantity) {
        List<OrderRequest> orderRequests = List.of(new OrderRequest(productId, quantity));
        OrderResponse orderResponse = orderServiceClient.createOrder(email, orderRequests);
        return orderResponse.getOrderId();
    }

    // 예외 발생 시 롤백
    private void rollbackProcess(String email, Long orderId, Long productId, int quantity) {
        try {
            OrderResponse order = null;
            try {
                order = orderServiceClient.getOrderById(orderId, email);
            } catch (Exception e) {
                System.out.println("주문 정보를 가져오는 중 오류 발생: " + e.getMessage());
            }

            // 결제 데이터 삭제
            try {
                if (paymentService.existsByOrderId(orderId)) {
                    paymentService.deletePaymentByOrderId(orderId);
                    System.out.println("결제 데이터를 성공적으로 삭제했습니다.");
                } else {
                    System.out.println("결제 데이터를 찾을 수 없어 삭제를 생략합니다.");
                }
            } catch (Exception e) {
                System.out.println("결제 데이터를 삭제하는 중 오류 발생: " + e.getMessage());
            }

            // 주문 데이터 삭제
            try {
                if (order != null) {
                    orderServiceClient.deleteOrder(orderId, email);
                    System.out.println("주문 데이터를 성공적으로 삭제했습니다.");
                } else {
                    System.out.println("주문 데이터를 찾을 수 없어 삭제를 생략합니다.");
                }
            } catch (Exception e) {
                System.out.println("주문 데이터를 삭제하는 중 오류 발생: " + e.getMessage());
            }

            // 재고 복구
            try {
                if (order != null) {
                    paymentService.restoreStock(productId, quantity);
                    System.out.println("재고를 성공적으로 복구했습니다.");
                } else {
                    System.out.println("주문 데이터를 찾을 수 없어 재고 복구를 생략합니다.");
                }
            } catch (Exception e) {
                System.out.println("재고 복구 중 오류 발생: " + e.getMessage());
            }

            System.out.println("롤백이 완료되었습니다.");

        } catch (Exception e) {
            System.out.println("롤백 중 오류 발생: " + e.getMessage());
        }

    }
}

package com.sparta.paymentservice.service;

import com.sparta.common.dto.*;
import com.sparta.paymentservice.client.OrderServiceClient;
import com.sparta.paymentservice.client.ProductServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentFacadeService {

    private final OrderServiceClient orderServiceClient;
    private final ProductServiceClient productServiceClient;
    private final PaymentService paymentService;

    public PaymentFacadeService(OrderServiceClient orderServiceClient, ProductServiceClient productServiceClient, PaymentService paymentService) {
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

        } catch (Exception e) {
            // 4. 롤백 -> 재고 복구, 주문 데이터 삭제
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
                System.out.println("주문 정보를 찾을 수 없습니다.");
            }
            // 주문 데이터 삭제
            if (order != null) {
                orderServiceClient.deleteOrder(orderId, email);
            }
            //  재고 복구
            productServiceClient.updateProductStock(productId, quantity);
            System.out.println("롤백이 완료되었습니다.");
        } catch (Exception e) {
            System.out.println("롤백 중 오류 발생: " + e.getMessage());
        }

    }
}

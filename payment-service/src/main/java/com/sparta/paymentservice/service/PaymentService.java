package com.sparta.paymentservice.service;

import com.sparta.common.dto.OrderItemResponse;
import com.sparta.common.dto.OrderResponse;
import com.sparta.common.dto.PaymentResponse;
import com.sparta.common.exception.PaymentProcessException;
import com.sparta.paymentservice.client.OrderServiceClient;
import com.sparta.paymentservice.client.ProductServiceClient;
import com.sparta.paymentservice.entity.Payment;
import com.sparta.paymentservice.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final ProductServiceClient productServiceClient;

    public PaymentService(PaymentRepository paymentRepository, OrderServiceClient orderServiceClient, ProductServiceClient productServiceClient) {
        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
        this.productServiceClient = productServiceClient;
    }

    // 결제 진입 api
    @Transactional
    public PaymentResponse startPayment(String email, Long orderId) {
        OrderResponse order = getOrder(orderId, email);
        int totalAmount = order.getTotalAmount();
        checkForPaymentExistence(orderId);
        // 20% 확률로 결제 이탈
        if (leavePayment()) {
            restoreStock(order); // 재고 복구
            orderServiceClient.deleteOrder(orderId, email); // 주문 데이터 삭제
            throw new PaymentProcessException("결제를 취소했습니다.");
        }
        // 새로 저장할 payment 만들기
        Payment payment = createAndSavePayment(order);
        return changeToPaymentResponse(payment);
    }

    // 결제 완료 api
    @Transactional
    public PaymentResponse endPayment(String email, Long orderId) {
        Payment payment = getPayment(orderId);
        // 20% 확률로 결제 이탈
        if (leavePayment()) {
            OrderResponse order = getOrder(orderId, email);
            restoreStock(order); // 재고 복구
            orderServiceClient.deleteOrder(orderId, email); // 주문 데이터 삭제
            deletePaymentByOrderId(orderId); // 결제 데이터 삭제
            throw new PaymentProcessException("한도 초과로 결제에 실패했습니다.");
        }
        payment.setPaymentStatus("결제완료");
        return changeToPaymentResponse(payment);
    }

        // ===================================

    // 주문 가져오기
    private OrderResponse getOrder(Long orderId, String email) {
        OrderResponse order = orderServiceClient.getOrderById(orderId, email);
        if (order == null) {
            throw new IllegalArgumentException("주문 정보를 찾을 수 없습니다.");
        }
        return order;
    }

    // 결제 가져오기
    private Payment getPayment(Long orderId) {
        Payment payment = paymentRepository.getByOrderId(orderId);
        if (payment == null) {
            throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다.");
        }
        return payment;
    }

    // 이미 결제된 주문인지
    private void checkForPaymentExistence(Long orderId) {
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("이미 결제된 주문입니다.");
        }
    }

    // 새로운 결제 데이터 저장
    private Payment createAndSavePayment(OrderResponse order) {
        Payment payment = new Payment(order.getOrderId(), "결제중", order.getTotalAmount(), LocalDateTime.now(), null);
        paymentRepository.save(payment);
        return payment;
    }

    // payment -> paymentResponse로 바꾸기
    private PaymentResponse changeToPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getOrderId(),
                payment.getPaymentStatus(),
                payment.getTotalAmount(),
                payment.getCreatedAt());
    }

    // 20% 확률
    private boolean leavePayment() {
        return Math.random() < 0.2;
//        return true; // 이탈
//        return false;
    }

    // 결제 이탈 시 재고 복구
    public void restoreStock (OrderResponse order) {
        for (OrderItemResponse orderItem : order.getOrderItems()) {
            productServiceClient.restoreStock(orderItem.getProductSnapshotId(), orderItem.getQuantity());
        }
    }

    // 결제 데이터 삭제
    public void deletePaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.getByOrderId(orderId);
        if (payment != null) {
            paymentRepository.deleteByOrderId(payment.getOrderId());
            entityManager.flush();
        } else {
            System.out.println("결제 데이터 삭제 실패. 해당 orderId의 결제 데이터를 찾을 수 없습니다.");
        }
    }
}

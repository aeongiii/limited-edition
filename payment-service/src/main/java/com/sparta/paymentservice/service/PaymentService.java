package com.sparta.paymentservice.service;

import com.sparta.common.dto.OrderItemResponse;
import com.sparta.common.dto.OrderResponse;
import com.sparta.paymentservice.client.OrderServiceClient;
import com.sparta.paymentservice.client.ProductServiceClient;
import com.sparta.paymentservice.dto.PaymentResponse;
import com.sparta.paymentservice.entity.Payment;
import com.sparta.paymentservice.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

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
        // 주문 아이디로 주문 가져오기
        OrderResponse order = orderServiceClient.getOrderById(orderId, email);
        if (order == null) {
            throw new IllegalArgumentException("주문 정보를 찾을 수 없습니다.");
        }
        // 주문에서 총금액 추출
        int totalAmount = order.getTotalAmount();
        // payment에 이미 orderId가 있는지 확인 (이미 결제된건지)
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("이미 결제된 주문입니다.");
        }
        // 20% 확률로 결제 이탈
        if (leavePayment()) {
            restoreStock(order); // 재고 복구
            orderServiceClient.deleteOrder(orderId, email); // 주문 데이터 삭제
            throw new IllegalArgumentException("결제 프로세스를 이탈했습니다.");
        }
        // 새로 저장할 payment 만들기
        Payment payment = new Payment(order.getOrderId(), "결제중", order.getTotalAmount(), LocalDateTime.now(), null);
        paymentRepository.save(payment);
        System.out.println("결제중입니다... orderId : " + orderId);
        return new PaymentResponse(orderId, payment.getPaymentStatus(), payment.getTotalAmount(), payment.getCreatedAt());
    }

    // 결제 완료 api
    @Transactional
    public PaymentResponse endPayment(String email, Long orderId) {
        Payment payment = paymentRepository.getByOrderId(orderId);
        if (payment == null) {
            throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다.");
        }
        // 20% 확률로 결제 이탈
        if (leavePayment()) {
            OrderResponse order = orderServiceClient.getOrderById(orderId, email);
            if (order == null) {
                throw new IllegalArgumentException("주문 정보를 찾을 수 없습니다.");
            }
            restoreStock(order);
            orderServiceClient.deleteOrder(orderId, email); // 주문 데이터 삭제
            deletePaymentByOrderId(orderId); // 결제 데이터 삭제

            throw new IllegalArgumentException("결제 이탈이 발생했습니다. 주문 및 결제 정보가 삭제되었습니다.");
        }
        payment.setPaymentStatus("결제완료");
        return new PaymentResponse(orderId, payment.getPaymentStatus(), payment.getTotalAmount(), payment.getCreatedAt());
    }

        // ===================================

    // 20% 확률
    private boolean leavePayment() {
        return new Random().nextInt(100) < 20;
//        return true;
//        return false;
    }

    // 결제 이탈 시 재고 복구
    private void restoreStock (OrderResponse order) {
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

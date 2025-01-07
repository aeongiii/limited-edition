package com.sparta.paymentservice.service;

import com.sparta.common.dto.OrderResponse;
import com.sparta.paymentservice.client.OrderServiceClient;
import com.sparta.paymentservice.dto.PaymentResponse;
import com.sparta.paymentservice.entity.Payment;
import com.sparta.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;

    public PaymentService(PaymentRepository paymentRepository, OrderServiceClient orderServiceClient) {
        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
    }

    public PaymentResponse payment(String email, Long orderId) {
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
        // 새로 저장할 payment 만들기
        Payment payment = new Payment(order.getOrderId(), "결제완료", order.getTotalAmount(), LocalDateTime.now(), null);
        paymentRepository.save(payment);
        System.out.println("결제완료. orderId : " + orderId);
        return new PaymentResponse(orderId, payment.getPaymentStatus(), payment.getTotalAmount(), payment.getCreatedAt());
    }
}

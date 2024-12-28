package com.sparta.orderservice.service;

import com.sparta.orderservice.dto.PaymentResponse;
import com.sparta.orderservice.entity.Orders;
import com.sparta.orderservice.entity.Payment;
import com.sparta.orderservice.repository.OrderRepository;
import com.sparta.orderservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse payment(String email, Long orderId) {
        // 주문 아이디로 주문 가져오기
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
        // 주문에서 총금액 추출
        int totalAmount = order.getTotalAmount();
        // payment에 이미 orderId가 있는지 확인 (이미 결제된건지)
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("이미 결제된 주문입니다.");
        }
        // 새로 저장할 payment 만들기
        Payment payment = new Payment(order, "결제 완료", order.getTotalAmount(), LocalDateTime.now(), null);
        paymentRepository.save(payment);
        System.out.println("결제 완료. orderId : " + orderId);
        return new PaymentResponse(order.getId(), payment.getPaymentStatus(), payment.getTotalAmount(), payment.getCreatedAt());
    }
}

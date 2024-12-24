package com.sparta.limited_edition.repository;

import com.sparta.limited_edition.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 이미 결제된 내역이 있는지
    boolean existsByOrderId(Long orderId);
}


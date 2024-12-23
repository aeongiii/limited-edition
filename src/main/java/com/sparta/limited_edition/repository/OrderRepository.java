package com.sparta.limited_edition.repository;

import com.sparta.limited_edition.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
    // email에 해당하는 사용자의 주문 내역 조회
    List<Orders> findAllByUserEmail(String email);
    // 취소, 반품 제외하고 주문 내역 조회
    List<Orders> findAllByUserEmailAndStatusIn(String email, List<String> status);
}

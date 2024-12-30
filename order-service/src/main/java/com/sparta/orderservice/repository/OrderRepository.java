package com.sparta.orderservice.repository;

import com.sparta.orderservice.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
    // email에 해당하는 사용자의 주문 내역 조회
//    List<Orders> findAllByUserEmail(String email);
    // 취소, 반품 제외하고 주문 내역 조회
//    List<Orders> findAllByUserEmailAndStatusIn(String email, List<String> status);
    // "반품 신청" 상태인 주문 모두 가져오기
//    List<Orders> findAllByStatus(String 반품_신청);
    // 주문내역 최신순 5개 가져오기
//    List<Orders> findTop5ByUserIdOrderByCreatedAtDesc(Long id);
}

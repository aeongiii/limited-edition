package com.sparta.productservice.repository;

import com.sparta.productservice.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    // 주문 목록 조회
    List<OrderDetail> findAllByOrdersId(Long id);
}

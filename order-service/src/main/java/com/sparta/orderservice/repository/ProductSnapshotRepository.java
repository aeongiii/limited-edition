package com.sparta.orderservice.repository;

import com.sparta.orderservice.entity.ProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSnapshotRepository extends JpaRepository<ProductSnapshot, Long> {
    // productId 대신 product.id로 검색하도록 수정
    Optional<ProductSnapshot> findByProduct_Id(Long productId);
}

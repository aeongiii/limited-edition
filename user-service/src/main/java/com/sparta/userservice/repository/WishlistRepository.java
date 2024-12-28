package com.sparta.userservice.repository;

import com.sparta.userservice.entity.Product;
import com.sparta.userservice.entity.User;
import com.sparta.userservice.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // 위시리스트에서 user, product로 특정 위시 찾기
    Optional<Wishlist> findByUserAndProduct(User user, Product product);
    // userId로 전체 위시리스트 목록 불러오기
    List<Wishlist> findAllByUserId(Long userId);
    // 주문 완료한 뒤 -> userId에 해당하는 위시 상품 모두 삭제
    void deleteAllByUserId(Long id);
    // 위시리스트 최신순 5개 가져오기
    List<Wishlist> findTop5ByUserIdOrderByCreatedAtDesc(Long id);
}

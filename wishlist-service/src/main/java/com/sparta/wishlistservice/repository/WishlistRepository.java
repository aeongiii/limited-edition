package com.sparta.wishlistservice.repository;

import com.sparta.wishlistservice.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // 위시리스트에서 user, product로 특정 위시 찾기
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    // userId로 전체 위시리스트 목록 불러오기
    List<Wishlist> findAllByUserId(Long userId);
    // 위시리스트 최신순 5개 가져오기
    List<Wishlist> findTop5ByUserIdOrderByCreatedAtDesc(Long id);
}

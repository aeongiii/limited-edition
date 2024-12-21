package com.sparta.limited_edition.repository;

import com.sparta.limited_edition.entity.Product;
import com.sparta.limited_edition.entity.User;
import com.sparta.limited_edition.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // 위시리스트에서 user, product로 찾기
    Optional<Wishlist> findByUserAndProduct(User user, Product product);

}

package com.sparta.limited_edition.service;

import com.sparta.limited_edition.dto.WishlistResponse;
import com.sparta.limited_edition.entity.Product;
import com.sparta.limited_edition.entity.User;
import com.sparta.limited_edition.entity.Wishlist;
import com.sparta.limited_edition.repository.ProductRepository;
import com.sparta.limited_edition.repository.UserRepository;
import com.sparta.limited_edition.repository.WishlistRepository;
import org.springframework.stereotype.Service;

@Service
public class WishlistService {

    private WishlistRepository wishlistRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // 위시리스트에 상품 추가
    public WishlistResponse addToWishlist(String email, long productId, int quantity) {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));
        System.out.println("유저 검증 완료");
        // 상품 검증
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품정보를 찾을 수 없습니다."));
        System.out.println("상품 검증 완료");
        // 상품 노출 false
        if (!product.isVisible()) {
            throw new IllegalArgumentException("해당 상품은 숨김 처리되었습니다.");
        }
        // 품절된 경우
        if (product.getStockQuantity() == 0) {
            throw new IllegalArgumentException("해당 상품은 품절되었습니다.");
        }
        // 위시리스트에 추가 (또는 수량 업데이트)
        Wishlist wishlist = wishlistRepository.findByUserAndProduct(user, product)
                .orElseGet(() -> new Wishlist(user, product, 0)); // 일단 기본수량 0으로 세팅
        wishlist.setQuantity(wishlist.getQuantity() + quantity); // 기존수량 + 새로 받은 수량 합치기
        wishlistRepository.save(wishlist);
        // 반환
        return new WishlistResponse(
                product.getId(),
                product.getName(),
                wishlist.getQuantity(), // 누적된 수량으로 입력해야한다...
                product.getPrice(),
                product.getImageUrl(),
                "http://localhost:8080/product/"+product.getId() // 상세정보 조회 url
        );
    }
}

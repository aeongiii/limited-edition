package com.sparta.wishlistservice.controller;

import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.WishlistResponse;
import com.sparta.wishlistservice.client.ProductServiceClient;
import com.sparta.wishlistservice.entity.Wishlist;
import com.sparta.wishlistservice.repository.WishlistRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/wishlist")
public class WishlistInternalController {

    private final WishlistRepository wishlistRepository;
    private final ProductServiceClient productServiceClient;

    public WishlistInternalController(WishlistRepository wishlistRepository, ProductServiceClient productServiceClient) {
        this.wishlistRepository = wishlistRepository;
        this.productServiceClient = productServiceClient;
    }

    // 주문한 상품은 위시리스트에서 삭제
    @DeleteMapping("/delete-items")
    public void deleteWishlistItems(@RequestParam Long userId, @RequestParam List<Long> productIds) {
        List<Wishlist> wishlists = wishlistRepository.findAllByUserId(userId);
        for(Long productId : productIds) {
            for(Wishlist wishlist : wishlists) {
                if(wishlist.getProductId().equals(productId)) {
                    wishlistRepository.delete(wishlist);
                }
            }
        }
        System.out.println("주문한 상품을 위시리스트에서 삭제했습니다.");
    }

    // 위시리스트 최근 5개 가져오기 (Product 모듈도 연결)
    @GetMapping("/top5/{userId}")
    public ResponseEntity<List<WishlistResponse>> getTop5Wishlist(@PathVariable Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        List<WishlistResponse> response = wishlists.stream()
                .map(wishlist -> {
                    ProductResponse product = productServiceClient.getProductById(wishlist.getProductId());
                    return new WishlistResponse(
                            product.getId(),
                            product.getName(),
                            wishlist.getQuantity(),
                            product.getPrice(),
                            product.getImageUrl(),
                            "http://localhost:8080/product/" + product.getId()
                    );
                }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}

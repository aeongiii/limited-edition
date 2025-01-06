package com.sparta.wishlistservice.controller;

import com.sparta.wishlistservice.dto.WishlistResponse;
import com.sparta.wishlistservice.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // 1. 상품을 위시리스트에 추가
    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable long productId,
                                           @RequestHeader(name = "X-User-Email", required = false) String email,
                                           @RequestBody Map<String, Integer> requestBody) {
        Integer quantity = validateQuantity(requestBody); // 수량 검증
        // 상품을 위시리스트에 추가
        WishlistResponse response = wishlistService.addToWishlist(email, productId, quantity);
        return ResponseEntity.ok(response);
    }

    // 2. 위시리스트 목록 조회
    @GetMapping("/wishlist")
    public List<WishlistResponse> getWishlist(@RequestHeader(name = "X-User-Email", required = false) String email) {
        return wishlistService.getWishlist(email);
    }

    // 3. 위시리스트 상품 수량 변경
    @PutMapping("/wishlist/{productId}")
    public ResponseEntity<?> updateWishlistQuantity (
            @PathVariable Long productId,
            @RequestHeader(name = "X-User-Email", required = false) String email,
            @RequestBody Map<String, Integer> requestBody) {
        Integer quantity = validateQuantity(requestBody); // 수량 검증
        // 수량 업데이트
        WishlistResponse updateWishlistItem = wishlistService.updateWishlistQuantity(email, productId, quantity);
        return ResponseEntity.ok(updateWishlistItem);
    }

    // 4. 위시리스트 상품 삭제
    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<String> deleteWishlistItem(
            @PathVariable Long productId,
            @RequestHeader(name = "X-User-Email", required = false) String email) {
        wishlistService.removeWishlistItem(email, productId); // 상품 삭제
        return ResponseEntity.ok("상품이 위시리스트에서 삭제되었습니다.");
    }

    // ======================

    // 입력받은 수량 검증
    private Integer validateQuantity(Map<String, Integer> requestBody) {
        Integer quantity = requestBody.getOrDefault("quantity", 0);
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다."); // 예외를 던져 일관된 처리
        }
        return quantity;
    }

}

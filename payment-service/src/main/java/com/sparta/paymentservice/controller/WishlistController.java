package com.sparta.paymentservice.controller;

import com.sparta.paymentservice.security.JwtTokenProvider;
import com.sparta.paymentservice.service.WishlistService;
import com.sparta.paymentservice.dto.WishlistResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WishlistController {

    private final JwtTokenProvider jwtTokenProvider;
    private final WishlistService wishlistService;

    public WishlistController(JwtTokenProvider jwtTokenProvider, WishlistService wishlistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.wishlistService = wishlistService;
    }

    // 1. 상품을 위시리스트에 추가
    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable long productId,
                                           @CookieValue(name = "accessToken", required = false) String accessToken,
                                           @RequestBody Map<String, Integer> requestBody) {
        jwtTokenProvider.validateAccessToken(accessToken); // Access Token 검증
        Integer quantity = validateQuantity(requestBody); // 수량 검증
        // 상품을 위시리스트에 추가
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        WishlistResponse response = wishlistService.addToWishlist(email, productId, quantity);
        return ResponseEntity.ok(response);
    }

    // 2. 위시리스트 목록 조회
    @GetMapping("/wishlist")
    public List<WishlistResponse> getWishlist(@CookieValue(name = "accessToken", required = false) String accessToken) {
        jwtTokenProvider.validateAccessToken(accessToken); // Access Token 검증
        // 목록 조회
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        return wishlistService.getWishlist(email);
    }

    // 3. 위시리스트 상품 수량 변경
    @PutMapping("/wishlist/{productId}")
    public ResponseEntity<?> updateWishlistQuantity (
            @PathVariable Long productId,
            @CookieValue(name = "accessToken", required = false) String accessToken,
            @RequestBody Map<String, Integer> requestBody) {
        String email = jwtTokenProvider.validateAndExtractEmail(accessToken);// Access Token 검증, 이메일 추출
        Integer quantity = validateQuantity(requestBody); // 수량 검증
        // 수량 업데이트
        WishlistResponse updateWishlistItem = wishlistService.updateWishlistQuantity(email, productId, quantity);
        return ResponseEntity.ok(updateWishlistItem);
    }

    // 4. 위시리스트 상품 삭제
    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<String> deleteWishlistItem(
            @PathVariable Long productId,
            @CookieValue(name = "accessToken", required = false) String accessToken) {
        String email = jwtTokenProvider.validateAndExtractEmail(accessToken); // Access Token 검증, 이메일 추출
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

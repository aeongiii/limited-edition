package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.WishlistResponse;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.WishlistService;
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

    // 상품을 위시리스트에 추가
    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable long productId,
                                           @CookieValue(name = "accessToken", required = false) String accessToken,
                                           @RequestBody Map<String, Integer> requestBody) {

        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);

        // 수량 검증
        Integer quantity = requestBody.get("quantity");
        if (quantity == null || quantity < 1) {
            return ResponseEntity.badRequest().body("수량은 1 이상이어야 합니다.");
        }
        System.out.println("수량 검증 완료");

        // 상품을 위시리스트에 추가
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        WishlistResponse response = wishlistService.addToWishlist(email, productId, quantity);
        System.out.println("상품을 위시리스트에 추가했습니다.");
        return ResponseEntity.ok(response);
    }

    // 위시리스트 목록 조회
    @GetMapping("/wishlist")
    public List<WishlistResponse> getWishlist(@CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);

        // 목록 조회
        String email = jwtTokenProvider.getUserIdFromToken(accessToken);
        List<WishlistResponse> allWishlist = wishlistService.getWishlist(email);
        return allWishlist;
    }

    // 위시리스트 상품 수량 변경
    @PutMapping("/wishlist/{productId}")
    public ResponseEntity<?> updateWishlistQuantity (
            @PathVariable Long productId,
            @CookieValue(name = "accessToken", required = false) String accessToken,
            @RequestBody Map<String, Integer> requestBody) {

        // Access Token 검증하고 이메일 추출하기
        String email = jwtTokenProvider.validateAndExtractEmail(accessToken);

        int quantity = requestBody.getOrDefault("quantity", 0);
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("수량은 0 이상이어야 합니다."); // 수량이 0 이하인 경우 예외 처리
        }

        // 수량 업데이트
        WishlistResponse updateWishlistItem = wishlistService.updateWishlistQuantity(email, productId, quantity);
        return ResponseEntity.ok(updateWishlistItem);
    }



}

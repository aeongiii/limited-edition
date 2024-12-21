package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.WishlistResponse;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class WishlistController {

    private final JwtTokenProvider jwtTokenProvider;
    private final WishlistService wishlistService;

    public WishlistController(JwtTokenProvider jwtTokenProvider, WishlistService wishlistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.wishlistService = wishlistService;
    }

    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable long productId,
                                           @CookieValue(name = "accessToken", required = false) String accessToken,
                                           @RequestBody Map<String, Integer> requestBody) {

        // Access Token 검증
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }
        System.out.println("Access Token 검증 완료");

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
}

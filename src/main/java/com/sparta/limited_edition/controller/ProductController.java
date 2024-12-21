package com.sparta.limited_edition.controller;

import com.sparta.limited_edition.dto.ProductDetailResponse;
import com.sparta.limited_edition.security.JwtTokenProvider;
import com.sparta.limited_edition.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {

    private final ProductService productService;
    private final JwtTokenProvider jwtTokenProvider;

    public ProductController(ProductService productService, JwtTokenProvider jwtTokenProvider) {
        this.productService = productService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 상품 상세정보 가져오기
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductDetails (@PathVariable long productId, @CookieValue(name = "accessToken", required = false) String accessToken) {
        // Access Token 유효성 검증
        if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Access Token입니다.");
        }
        // 상품 정보 가져오기
        ProductDetailResponse productDetail = productService.getProductDetails(productId);
        return ResponseEntity.ok(productDetail);
    }
}

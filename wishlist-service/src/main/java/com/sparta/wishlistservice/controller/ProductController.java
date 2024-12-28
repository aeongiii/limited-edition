package com.sparta.wishlistservice.controller;

import com.sparta.wishlistservice.dto.ProductDetailResponse;
import com.sparta.wishlistservice.service.ProductService;
import com.sparta.wishlistservice.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
        // Access Token 검증
        jwtTokenProvider.validateAccessToken(accessToken);

        // 상품 정보 가져오기
        ProductDetailResponse productDetail = productService.getProductDetails(productId);
        return ResponseEntity.ok(productDetail);
    }
}

package com.sparta.productservice.controller;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 상품 상세정보 가져오기
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductDetails (
            @PathVariable long productId,
            @RequestHeader(name = "X-User-Email", required = false) String email) {
        // 상품 정보 가져오기
        ProductDetailResponse productDetail = productService.getProductDetails(productId);
        return ResponseEntity.ok(productDetail);
    }
}

package com.sparta.productservice.controller;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    // 일반 상품 목록 조회
    @GetMapping("/product/unlimited")
    public ResponseEntity<?> getUnlimitedProductList(@RequestHeader(name = "X-User-Email", required = false) String email) {
        String limitedType = "unlimited";
        // 리스트로 가져오기
        List<ProductDetailResponse> productList = productService.getProductList(limitedType);
        return ResponseEntity.ok(productList);
    }

    // 선착순 상품 목록 조회
    @GetMapping("/product/limited")
    public ResponseEntity<?> getLimitedProductList(@RequestHeader(name = "X-User-Email", required = false) String email) {
        String limitedType = "limited";
        // 리스트로 가져오기
        List<ProductDetailResponse> productList = productService.getProductList(limitedType);
        return ResponseEntity.ok(productList);
    }
}

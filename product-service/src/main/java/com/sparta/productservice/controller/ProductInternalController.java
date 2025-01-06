package com.sparta.productservice.controller;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import com.sparta.productservice.entity.ProductSnapshot;
import com.sparta.productservice.repository.ProductSnapshotRepository;
import com.sparta.productservice.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/product")
public class ProductInternalController {

    private final ProductSnapshotRepository productSnapshotRepository;
    private final ProductService productService;

    public ProductInternalController(ProductSnapshotRepository productSnapshotRepository, ProductService productService) {
        this.productSnapshotRepository = productSnapshotRepository;
        this.productService = productService;
    }

    // 상품 조회
    @GetMapping("/just/{productId}")
    public ProductResponse getProductById(@PathVariable Long productId) {
        return productService.getJustProductResponse(productId);
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ProductDetailResponse getProductDetailById(@PathVariable Long productId) {
        return productService.getProductDetails(productId);
    }

    // 상품 재고 업데이트
    @PutMapping("/{productId}/update-stock")
    public void updateProductStock(@PathVariable Long productId, @RequestParam int quantity) {
        productService.updateProductStock(productId, quantity);
    }

    // 스냅샷 생성, 저장
    @PostMapping("/snapshot")
    public ProductSnapshotResponse createProductSnapshot(@RequestBody ProductResponse productResponse) {
        ProductSnapshot productSnapshot = productService.createProductSnapshot(productResponse);
        return productService.createProductSnapshotResponse(productSnapshot);
    }

    // id로 스냅샷 찾기
    @GetMapping("/snapshot/{ProductSnapshotId}")
    public ProductSnapshotResponse getProductSnapshotById(@PathVariable Long ProductSnapshotId) {
        ProductSnapshot productSnapshot = productSnapshotRepository.findById(ProductSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("ProductSnapshot 정보를 찾을 수 없습니다. Id : " + ProductSnapshotId));
        return productService.createProductSnapshotResponse(productSnapshot);
    }
}

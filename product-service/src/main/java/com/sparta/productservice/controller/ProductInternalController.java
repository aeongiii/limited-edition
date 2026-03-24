package com.sparta.productservice.controller;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import com.sparta.productservice.entity.Product;
import com.sparta.productservice.entity.ProductSnapshot;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.repository.ProductSnapshotRepository;
import com.sparta.productservice.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/product")
public class ProductInternalController {

    private final ProductSnapshotRepository productSnapshotRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;

    public ProductInternalController(ProductSnapshotRepository productSnapshotRepository, ProductService productService, ProductRepository productRepository) {
        this.productSnapshotRepository = productSnapshotRepository;
        this.productService = productService;
        this.productRepository = productRepository;
    }
    // productSnapshotId로 productResponse 반환
    @GetMapping("/product")
    public ProductResponse getProductByProductId(Long productSnapshotId) {
        Product product = productRepository.findById(productSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return productService.getJustProductResponse(product.getId());
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

    // 재고 복구
    @PutMapping("/{productSnapshotId}/restore")
    public void restoreStock(@PathVariable Long productSnapshotId, @RequestParam int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("복구 수량은 1 이상이어야 합니다.");
        }
        ProductSnapshot productSnapshot = productSnapshotRepository.findById(productSnapshotId)
                .orElseThrow(() -> new IllegalArgumentException("ProductSnapshot 정보를 찾을 수 없습니다. Id : " + productSnapshotId));
        Product product = productSnapshot.getProduct();
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    // productId 기준 재고 복구 (결제 롤백에서 사용)
    @PutMapping("/restore-by-product/{productId}")
    public void restoreStockByProductId(@PathVariable Long productId, @RequestParam int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("복구 수량은 1 이상이어야 합니다.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. Id : " + productId));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

}

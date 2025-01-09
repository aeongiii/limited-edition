package com.sparta.paymentservice.client;

import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "http://localhost:8082/api/internal/product")
public interface ProductServiceClient {

    // 재고 업데이트
    @PutMapping("/{productId}/update-stock")
    void updateProductStock(@PathVariable Long productId, @RequestParam int quantity);

    // 재고 복구
    @PutMapping("/{productSnapshotId}/restore")
    void restoreStock(@PathVariable("productSnapshotId") Long productSnapshotId, @RequestParam("quantity") int quantity);

    // 상품 조회
    @GetMapping("/just/{productId}")
    ProductResponse getProductById(@PathVariable("productId")Long productId);

    // id로 스냅샷 찾기
    @GetMapping("/snapshot/{ProductSnapshotId}")
    ProductSnapshotResponse getProductSnapshotById(Long productSnapshotId);
}

package com.sparta.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "http://localhost:8082/api/internal/product")
public interface ProductServiceClient {
    // 재고 복구
    @PutMapping("/{productSnapshotId}/restore")
    void restoreStock(@PathVariable("productSnapshotId") Long productSnapshotId, @RequestParam("quantity") int quantity);
}

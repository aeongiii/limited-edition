package com.sparta.orderservice.client;

import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", url = "http://localhost:8082/api/internal/product")
public interface ProductServiceClient {

    @GetMapping("/just/{productId}")
    ProductResponse getProductById(@PathVariable Long productId);

    @PutMapping("/{productId}/update-stock")
    void updateProductStock(@PathVariable Long productId, @RequestParam int quantity);

    @PostMapping("/snapshot")
    ProductSnapshotResponse createProductSnapshot(@RequestBody ProductResponse productResponse);

    @GetMapping("/snapshot/{ProductSnapshotId}")
    ProductSnapshotResponse getProductSnapshotById(@PathVariable Long ProductSnapshotId);
}

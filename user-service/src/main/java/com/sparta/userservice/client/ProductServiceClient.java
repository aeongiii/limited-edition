package com.sparta.userservice.client;

import com.sparta.common.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8082/api/internal/product")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    ProductResponse getProductById(@PathVariable Long productId);
}

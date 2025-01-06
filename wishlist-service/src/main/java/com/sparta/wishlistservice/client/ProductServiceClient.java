package com.sparta.wishlistservice.client;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8082/api/internal/product")
public interface ProductServiceClient {

    @GetMapping("/just/{productId}")
    ProductResponse getProductById(@PathVariable("productId") Long productId);

    @GetMapping("/detail/{productId}")
    ProductDetailResponse getProductDetailById(@PathVariable("productId") Long productId);
}

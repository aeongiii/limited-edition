package com.sparta.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "wishlist-service", url = "http://localhost:8084/api/internal/wishlist")
public interface WishlistServiceClient {

    @DeleteMapping("/delete-items")
    void deleteWishlistItems(@RequestParam Long userId, @RequestParam List<Long> productIds);
}

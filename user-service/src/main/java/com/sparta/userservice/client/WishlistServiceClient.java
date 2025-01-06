package com.sparta.userservice.client;

import com.sparta.common.dto.WishlistResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "wishlist-service", url = "http://localhost:8084/api/internal/wishlist")
public interface WishlistServiceClient {

    @GetMapping("/top5/{userId}")
    List<WishlistResponse> getTop5Wishlist(@PathVariable Long userId);
}

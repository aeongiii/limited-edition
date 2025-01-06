package com.sparta.userservice.client;

import com.sparta.common.dto.OrderResponse;
import com.sparta.common.dto.RecentOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "order-service", url = "http://localhost:8083/api/internal/order")
public interface OrderServiceClient {

    @GetMapping("/{orderId}")
    OrderResponse getOrderById(@PathVariable Long orderId);

    @GetMapping("/top5/{userId}")
    List<RecentOrderResponse> getTop5OrderList(@PathVariable Long userid);
}

package com.sparta.paymentservice.client;

import com.sparta.common.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service", url = "http://localhost:8083/api/internal/order")
public interface OrderServiceClient {

    @GetMapping("/{orderId}")
    OrderResponse getOrderById(@PathVariable Long orderId, @RequestHeader("X-User-Email") String email);
}

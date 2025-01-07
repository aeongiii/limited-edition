package com.sparta.paymentservice.client;

import com.sparta.common.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service", url = "http://localhost:8083/api/internal/order")
public interface OrderServiceClient {

    @GetMapping("/{orderId}")
    OrderResponse getOrderById(@PathVariable Long orderId, @RequestHeader("X-User-Email") String email);

    // 결제 이탈 시 주문데이터 삭제
    @DeleteMapping("/delete/{orderId}/{email}")
    void deleteOrder(@PathVariable("orderId") Long orderId, @PathVariable("email") String email);
}

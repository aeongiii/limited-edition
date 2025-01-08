package com.sparta.paymentservice.client;

import com.sparta.common.dto.OrderRequest;
import com.sparta.common.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "order-service", url = "http://localhost:8083")
public interface OrderServiceClient {

    // 주문 api 호출
    @PostMapping("/order")
    OrderResponse createOrder(@RequestHeader("X-User-Email") String email,
                              @RequestBody List<OrderRequest> orderItems
                              );

    // id로 주문 가져오기
    @GetMapping("/api/internal/order/{orderId}")
    OrderResponse getOrderById(@PathVariable Long orderId, @RequestHeader("X-User-Email") String email);

    // 결제 이탈 시 주문데이터 삭제
    @DeleteMapping("/api/internal/order/delete/{orderId}/{email}")
    void deleteOrder(@PathVariable("orderId") Long orderId, @PathVariable("email") String email);
}

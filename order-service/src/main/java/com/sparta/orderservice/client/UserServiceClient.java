package com.sparta.orderservice.client;

import com.sparta.common.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://localhost:8081/api/internal/user")
public interface UserServiceClient {

    @GetMapping("/{email}")
    UserResponse getUserEmail(@PathVariable("email") String email);

}

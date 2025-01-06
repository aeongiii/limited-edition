package com.sparta.orderservice;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sparta.orderservice", "com.sparta.common"})
@EnableFeignClients(basePackages = {"com.sparta.orderservice.client", "com.sparta.common.security"})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}

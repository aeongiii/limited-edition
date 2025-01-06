package com.sparta.wishlistservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.sparta.wishlistservice", "com.sparta.common"})
@EnableFeignClients(basePackages = {"com.sparta.wishlistservice.client", "com.sparta.common"})
public class WishlistServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WishlistServiceApplication.class, args);
    }

}

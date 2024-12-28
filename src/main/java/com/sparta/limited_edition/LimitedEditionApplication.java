package com.sparta.limited_edition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.sparta.orderservice", "com.sparta.paymentservice", "com.sparta.productservice", "com.sparta.userservice", "com.sparta.wishlistservice",})
public class LimitedEditionApplication {

    public static void main(String[] args) {
        SpringApplication.run(LimitedEditionApplication.class, args);
    }

}

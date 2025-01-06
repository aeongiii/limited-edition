package com.sparta.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"com.sparta.paymentservice.client", "com.sparta.common"})
@SpringBootApplication(scanBasePackages = {"com.sparta.paymentservice", "com.sparta.common.security"})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}

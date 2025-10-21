package com.sparta.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")  // 모듈명에 맞게 변경
                        .description("E-Commerce 서비스의 결제 관련 API 명세서입니다.")  // 모듈명 변경
                        .version("1.0"));
    }
}
package com.sparta.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

// gateway에서 사용하는 SecurityConfig - jwtAuthenticationFilter(JWT 토큰 검증) 사용
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // BCrypt 비밀번호 암호화 설정 -> Password를 Bean으로 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable) // CSRF 비활성화
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll()) // 모든 요청 허용
                .build(); // SecurityWebFilterChain 객체 반환
    }
}

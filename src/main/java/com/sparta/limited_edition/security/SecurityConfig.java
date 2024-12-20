package com.sparta.limited_edition.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    // BCrypt 비밀번호 암호화 설정하기 -> Password를 Bean으로 등록한다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (필요 시 활성화해야 함)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 모두 인증없이 접근가능
//                        .requestMatchers("/public/**").permitAll()  -> 해당 경로만 인증 없이 접근 가능하도록 하는 내용
//                                .anyRequest().authenticated()       -> permitAll() 지정한 경로를 제외하고 나머지는 인증 필요하도록 설정.
                );
        return http.build();
    }

    // AuthenticationManager -> Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}

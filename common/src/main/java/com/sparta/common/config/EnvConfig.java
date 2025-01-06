package com.sparta.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.configure().load();

    @Bean
    public Dotenv dotenv() {
        return dotenv;
    }
}

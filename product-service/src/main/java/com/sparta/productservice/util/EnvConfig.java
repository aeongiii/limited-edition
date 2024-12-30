package com.sparta.productservice.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    // .env 파일을 읽도록
    private static final Dotenv dotenv = Dotenv.configure().load();

    @Bean
    public Dotenv dotenv() {
        String host = dotenv.get("MYSQL_HOST");
        String port = dotenv.get("MYSQL_PORT");
        System.out.println("MYSQL_HOST: " + host);
        System.out.println("MYSQL_PORT: " + port);
        return dotenv;
    }
}

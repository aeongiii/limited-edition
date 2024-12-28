package com.sparta.productservice.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    // Spring과 Redis 상호작용
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory); // redis 연결 설정
        template.setKeySerializer(new StringRedisSerializer()); // key를 String으로 직렬화
        template.setValueSerializer(new StringRedisSerializer()); // vlaue를 String으로 직렬화
        return template;
    }
}

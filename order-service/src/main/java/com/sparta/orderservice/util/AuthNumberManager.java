package com.sparta.orderservice.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AuthNumberManager {

    private final RedisTemplate<String, String> redisTemplate;

    public AuthNumberManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 인증번호 저장
    public void saveAuthNumber(String email, String authNumber, long validityInSeconds) {
        // key에 이메일 저장
        String redisKey = "auth:" + email;
        // value에 인증번호 저장, 만료시간(시간, 단위) 설정
        redisTemplate.opsForValue().set(redisKey, authNumber, validityInSeconds, TimeUnit.SECONDS);
        System.out.println("Redis에 인증번호 저장 완료. key = " + redisKey + " value = " + authNumber);
            }

    // 인증번호 검증
    public boolean validateAuthNumber(String email, String authNumber) {
        // key 만들기
        String redisKey = "auth:" + email;
        // key값으로 value 가져오기
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        // value가 없거나 다를때
        if (redisValue == null || !redisValue.equals(authNumber)) {
            System.out.println("인증번호 검증 실패");
            return false;
        }
        System.out.println("인증번호 검증 성공");
        return true;
    }
}

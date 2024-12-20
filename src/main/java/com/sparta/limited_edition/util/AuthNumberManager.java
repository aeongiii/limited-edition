package com.sparta.limited_edition.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class AuthNumberManager {
    // 이메일 인증번호를 임시로 ConcurrentHashMap에 저장. (key : 이메일주소, value : 인증번호)
    // 추후 redis로 변경할 예정.
    private final Map<String, String> authNumberMap = new ConcurrentHashMap<>();
    // 스케줄링 (인증번호 유효시간 관리)
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 인증번호 저장
    public void saveAuthNumber(String email, String authNumber, long validityInSeconds) {
        authNumberMap.put(email, authNumber); // map에 이메일 + 인증번호 저장
        // 유효시간 지나면 해당 이메일의 인증번호를 map에서 제거
        scheduler.schedule(() -> authNumberMap.remove(email), validityInSeconds, TimeUnit.SECONDS);
    }

    // 인증번호 검증
    public boolean validateAuthNumber(String email, String authNumber) {
        // map에 해당 이메일 존재하는지 + 인증번호가 같은지 확인
        return authNumberMap.containsKey(email) && authNumberMap.get(email).equals(authNumber);
    }
}

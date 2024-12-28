package com.sparta.wishlistservice.util;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    // 비밀번호 유효성 검사 -> 영문 + 숫자 + 특수문자 = 8자리 이상
    public static boolean isValidPassword(String password) {
        // 정규식
        String passwordPattern = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!~*]).{8,}$";
        return password.matches(passwordPattern);
    }
}

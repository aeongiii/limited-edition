package com.sparta.common.exception;

// 인증번호 검증
public class AuthNumberValidationException extends RuntimeException {
    public AuthNumberValidationException(String message) {
        super(message);
    }
}

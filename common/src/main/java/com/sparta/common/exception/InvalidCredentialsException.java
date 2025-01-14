package com.sparta.common.exception;

// 로그인 - 이메일 또는 비밀번호가 올바르지 않습니다. (401)
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

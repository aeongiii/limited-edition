package com.sparta.common.exception;

// 이메일 중복
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
      super(message);
    }
}

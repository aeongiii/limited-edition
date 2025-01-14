package com.sparta.common.exception;

import org.springframework.mail.MailException;

public class MailSendFailureException extends RuntimeException {
    public MailSendFailureException(String message, MailException cause) {
        super(message, cause); // 메시지와 원인 예외를 부모 클래스에 전달
    }
}
package com.sparta.common.exception;

public class PaymentProcessException extends RuntimeException {

    public PaymentProcessException(String message) {
        super(message);
    }
}
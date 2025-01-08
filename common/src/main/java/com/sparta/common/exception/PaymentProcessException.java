package com.sparta.common.exception;

import lombok.Getter;

@Getter
public class PaymentProcessException extends RuntimeException {
    private final String errorMessage;

    public PaymentProcessException(String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
    }
}

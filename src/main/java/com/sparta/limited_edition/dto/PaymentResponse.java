package com.sparta.limited_edition.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long orderId;
    private String paymentStatus;
    private int totalAmount;
    private LocalDateTime createdAt;
}

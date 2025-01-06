package com.sparta.common.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderResponse {
    private Long orderId;
    private String status;
    private String createdAt;
}

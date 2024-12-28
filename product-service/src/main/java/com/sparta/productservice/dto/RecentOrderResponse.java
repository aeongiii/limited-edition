package com.sparta.productservice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderResponse {
    // 마이페이지에서 주문내역 최신순 5개 가져오기

    private Long orderId;
    private String status;
    private String createdAt;
}

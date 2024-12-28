package com.sparta.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyPageResponse {

    // 개인정보 + 위시리스트 5개 + 주문정보 5개
    private String email;
    private String name;
    private String address;
    private LocalDateTime createdAt;

    private List<WishlistResponse> recentWishlist;
    private List<RecentOrderResponse> recentOrder;
}


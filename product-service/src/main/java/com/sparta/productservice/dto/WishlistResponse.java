package com.sparta.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WishlistResponse {

    private Long productId;
    private String name;
    private int quantity;
    private int price;
    private String imageUrl;
    private String detailUrl;

}

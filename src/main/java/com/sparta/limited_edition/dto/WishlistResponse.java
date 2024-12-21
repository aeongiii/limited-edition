package com.sparta.limited_edition.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WishlistResponse {

    private long productId;
    private String name;
    private int quantity;
    private int price;
    private String imageUrl;
    private String detailUrl;

    public WishlistResponse(long productId, String name, int quantity, int price, String imageUrl, String detailUrl) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.imageUrl = imageUrl;
        this.detailUrl = detailUrl;
    }
}

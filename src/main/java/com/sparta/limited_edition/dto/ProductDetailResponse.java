package com.sparta.limited_edition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDetailResponse {

    private final Long productId;
    private final String name;
    private final String description;
    private final int price;
    private final String imageUrl;
    private final boolean isSoldOut;


    public ProductDetailResponse(Long productId, String name, String description, int price, String imageUrl, boolean isSoldOut) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isSoldOut = isSoldOut;
    }
}

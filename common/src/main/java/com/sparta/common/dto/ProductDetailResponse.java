package com.sparta.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProductDetailResponse {

    private final Long productId;
    private final String name;
    private final String description;
    private final int price;
    private final String imageUrl;
    private final int stockQuantity;
    private final boolean isSoldOut;
    private final String limitedType;

}

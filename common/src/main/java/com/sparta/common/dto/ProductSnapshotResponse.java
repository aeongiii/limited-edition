package com.sparta.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSnapshotResponse {
    private Long id;
    private ProductResponse productResponse;
    private String name;
    private String description;
    private int price;
    private String imageUrl;

}

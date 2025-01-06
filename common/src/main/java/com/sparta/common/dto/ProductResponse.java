package com.sparta.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private long id;
    private String name;
    private String description;
    private int price;
    private int stockQuantity;

    @JsonProperty("is_visible") // JSON 필드와 매핑
    private boolean isVisible = true; // 기본값을 true로 설정

    private String imageUrl;

    public boolean isVisible() {
        return Boolean.TRUE.equals(isVisible); // null 방지 처리
    }

    @Override
    public String toString() {
        return "ProductResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", isVisible=" + isVisible +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }

}

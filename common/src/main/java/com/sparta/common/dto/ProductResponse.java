package com.sparta.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private String limitedType; // 새로 추가된 필드
    private LocalDateTime createdAt; // 새로 추가된 필드
    private LocalDateTime updatedAt; // 새로 추가된 필드

    public boolean isVisible() {
        return Boolean.TRUE.equals(isVisible); // null 방지 처리
    }



}

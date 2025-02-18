package com.sparta.common.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    // 상품 주문 처리한 후에 OrderResponse에 리스트로 포함되어 전달됨
    private Long productSnapshotId;
    private String name;
    private int quantity;
    private int price;
    private int subtotalAmount; // 상품별 총금액
}

package com.sparta.wishlistservice.dto;
import com.sparta.wishlistservice.entity.ProductSnapshot;
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

    public OrderItemResponse(ProductSnapshot snapshot, int quantity, int subtotalAmount) {
        this.productSnapshotId = snapshot.getProductId();
        this.name = snapshot.getName();
        this.quantity = quantity;
        this.price = snapshot.getPrice();
        this.subtotalAmount = subtotalAmount;
    }
}

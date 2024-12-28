package com.sparta.productservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_detail")
@NoArgsConstructor
@Getter
@Setter
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_snapshot_id", nullable = false)
    private ProductSnapshot productSnapshot;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int subtotalAmount; // 상품별 부분합계 (가격 * 주문수량)

    public OrderDetail(Orders order, ProductSnapshot productSnapshot, int quantity, int subtotalAmount) {
        this.orders = order;
        this.productSnapshot = productSnapshot;
        this.quantity = quantity;
        this.subtotalAmount = subtotalAmount;
    }


}

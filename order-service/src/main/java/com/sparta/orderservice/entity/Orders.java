package com.sparta.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@Getter
@Setter
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String status; // 주문 상태 (예: 주문 완료)

    @Column(nullable = false)
    private int totalAmount; // 총 주문 가격

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    public Orders(Long userId, String status, int totalAmount) {
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
    }
}

package com.sparta.limited_edition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@NoArgsConstructor
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private String description;
    private int price;
    private int stockQuantity;
    private boolean isVisible;
    private String imageUrl;
}

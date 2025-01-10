package com.sparta.productservice;

import com.sparta.productservice.entity.Product;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;

    @Test
    void openProduct_success() {
        // Given
        Product product = new Product(1L, "상품 A", "설명", 10000, 10, false, "url", "limited", LocalDateTime.now(), LocalDateTime.now());
        productRepository.save(product);

        // When
        productService.openProduct();

        // Then
        Product updatedProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        assertTrue(updatedProduct.isVisible());
    }
}


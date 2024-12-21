package com.sparta.limited_edition.service;

import com.sparta.limited_edition.dto.ProductDetailResponse;
import com.sparta.limited_edition.entity.Product;
import com.sparta.limited_edition.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;


    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductDetailResponse getProductDetails(Long productId) {
        // 상품 존재 여부 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
        // 상품 노출 여부 확인
        if (!product.isVisible()) {
            throw new IllegalArgumentException("해당 상품은 숨김 처리되었습니다.");
        }
        // 상품 품절 여부 확인
        boolean isSoldOut = product.getStockQuantity() == 0;
        // 상품 정보 반환
        return new ProductDetailResponse(
                    product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getImageUrl(),
                        isSoldOut

        );
    }
}

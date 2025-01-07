package com.sparta.productservice.service;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import com.sparta.productservice.entity.Product;
import com.sparta.productservice.entity.ProductSnapshot;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.repository.ProductSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSnapshotRepository productSnapshotRepository;


    public ProductService(ProductRepository productRepository, ProductSnapshotRepository productSnapshotRepository) {
        this.productRepository = productRepository;
        this.productSnapshotRepository = productSnapshotRepository;
    }

    // 상품 상세정보 반환
    public ProductDetailResponse getProductDetails(Long productId) {
        // 상품 존재 여부 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
        // 상품 노출 여부 확인
        if (!product.isVisible()) {
            throw new IllegalArgumentException("해당 상품은 숨김 처리되었습니다.");
        }
        // 상품 품절 여부 확인
//        boolean isSoldOut = product.getStockQuantity() <= 0;
        // 상품 정보 반환
        return new ProductDetailResponse(
                    product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getImageUrl(),
                        product.getStockQuantity(),
                product.getStockQuantity() <= 0,
                    product.getLimitedType()

        );
    }

    // 재고 수량 업데이트
    public void updateProductStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.setStockQuantity(quantity);
        productRepository.save(product);
    }

    // 스냅샷 생성, 저장
    public ProductSnapshot createProductSnapshot(ProductResponse productResponse) {
        ProductSnapshot snapshot = new ProductSnapshot(
                new Product(
                        productResponse.getId(),
                        productResponse.getName(),
                        productResponse.getDescription(),
                        productResponse.getPrice(),
                        productResponse.getStockQuantity(),
                        productResponse.isVisible(),
                        productResponse.getImageUrl(),
                        productResponse.getLimitedType(),
                        productResponse.getCreatedAt(),
                        productResponse.getUpdatedAt()
                )
        );
        return productSnapshotRepository.save(snapshot);
    }

    // 상품 정보 반환
    public ProductResponse getJustProductResponse(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // ProductResponse로 변환 후 반환
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isVisible(),
                product.getImageUrl(),
                product.getLimitedType(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    // 일반 / 선착순 상품 리스트 반환
    public List<ProductDetailResponse> getProductList(String limitedType) {
        List<Product> productList = productRepository.findByLimitedType(limitedType);
        // Product 엔티티를 ProductDetailResponse로 변환
        List<ProductDetailResponse> productDetailResponseList = productList.stream()
                .filter(Product::isVisible) // 공개된 상품만
                .map(product -> new ProductDetailResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getImageUrl(),
                        product.getStockQuantity(),
                        product.getStockQuantity() <= 0, // 재고가 0 이하일 때 sold out
                        product.getLimitedType()
                ))
                .toList();
        return productDetailResponseList;
    }

    // ProductSnapshot을 ProductSnapshotResponse로 변환
    public ProductSnapshotResponse createProductSnapshotResponse(ProductSnapshot productSnapshot) {
        ProductResponse productResponse = new ProductResponse(
                productSnapshot.getProduct().getId(),
                productSnapshot.getProduct().getName(),
                productSnapshot.getProduct().getDescription(),
                productSnapshot.getProduct().getPrice(),
                productSnapshot.getProduct().getStockQuantity(),
                productSnapshot.getProduct().isVisible(),
                productSnapshot.getProduct().getImageUrl(),
                productSnapshot.getProduct().getLimitedType(),
                productSnapshot.getProduct().getCreatedAt(),
                productSnapshot.getProduct().getUpdatedAt()
        );

        return new ProductSnapshotResponse(
                productSnapshot.getId(),
                productResponse,
                productSnapshot.getProduct().getName(),
                productSnapshot.getProduct().getDescription(),
                productSnapshot.getProduct().getPrice(),
                productSnapshot.getProduct().getImageUrl()
        );
    }

    // 남은 재고 수량 확인
    public int getStockQuantity(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
        return product.getStockQuantity();
    }
}
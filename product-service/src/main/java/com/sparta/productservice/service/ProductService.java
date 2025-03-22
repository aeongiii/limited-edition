package com.sparta.productservice.service;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import com.sparta.common.exception.ProductNotFoundException;
import com.sparta.productservice.entity.Product;
import com.sparta.productservice.entity.ProductSnapshot;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.repository.ProductSnapshotRepository;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSnapshotRepository productSnapshotRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    public ProductService(ProductRepository productRepository, ProductSnapshotRepository productSnapshotRepository, StringRedisTemplate redisTemplate, RedissonClient redissonClient) {
        this.productRepository = productRepository;
        this.productSnapshotRepository = productSnapshotRepository;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    // 상품 상세정보 반환
    public ProductDetailResponse getProductDetails(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품이 존재하지 않습니다."));
        if (!product.isVisible()) {
            throw new IllegalArgumentException("해당 상품은 숨김 처리되었습니다.");
        }
//        boolean isSoldOut = product.getStockQuantity() <= 0;
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
                .orElseThrow(() -> new ProductNotFoundException("상품이 존재하지 않습니다."));

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
        List<ProductDetailResponse> productDetailResponseList = productList.stream()
                .filter(Product::isVisible)
                .map(product -> new ProductDetailResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getImageUrl(),
                        product.getStockQuantity(),
                        product.getStockQuantity() <= 0,
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
        String redisKey = "product:stock:" + productId;
        Integer stockQuantity = getQuantityFromRedis(redisKey);
        if (stockQuantity != null) {
            return stockQuantity;
        }
        int newQuantity = getQuantityFromDatabase(productId);
        saveQuantityToRedis(redisKey, newQuantity, 300);
        return newQuantity;
    }

    // 매일 오후 2시에 상품 오픈
    @Scheduled(cron = "0 0 14 * * ?")
    public void openProduct() {
        List<Product> productList = productRepository.findByLimitedType("limited");
        for (Product product : productList) {
            if (!product.isVisible()) {
                product.setVisible(true);
                productRepository.save(product);
                System.out.println(product.getId() + "번 상품 오픈");
            }
        }
    }


    // ====================================

    // redis에서 재고수량 조회
    private Integer getQuantityFromRedis(String redisKey) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String stockQuantity = ops.get(redisKey);
        return (stockQuantity != null) ? Integer.parseInt(stockQuantity) : null;
    }

    // DB에서 재고 수량 조회
    private int getQuantityFromDatabase(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품이 존재하지 않습니다."));
        return product.getStockQuantity();
    }

    // Redis에 재고 수량 저장
    private void saveQuantityToRedis(String redisKey, int stockQuantity, long ttlSeconds) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(redisKey, String.valueOf(stockQuantity), ttlSeconds, TimeUnit.SECONDS);
        System.out.println("Redis에 재고 수량을 저장했습니다. TTL: " + ttlSeconds + "초");
    }



}
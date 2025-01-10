package com.sparta.productservice.service;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import com.sparta.productservice.entity.Product;
import com.sparta.productservice.entity.ProductSnapshot;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.repository.ProductSnapshotRepository;
import org.redisson.api.RLock;
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
        String lockKey = "product:lock:"+productId; // 분산락 키 설정
        RLock lock = redissonClient.getFairLock(lockKey);
        System.out.println("분산락 키 설정 완료. lockKey : " + lockKey);

        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) { // 락 획득 시도 (10초 대기, 5초 유지)
                System.out.println("락을 획득했습니다.");
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
                product.setStockQuantity(quantity);
                productRepository.save(product);
                System.out.println("DB에 재고 업데이트 완료");

                // Redis에 재고 업데이트
                String redisKey = "product:stock:" + productId;
                saveQuantityToRedis(redisKey, quantity);
                System.out.println("Redis에 재고 업데이트 완료");

            } else {
                // 대기시간(10초)동안 락 획득 실패
                throw new RuntimeException("락 획득 실패 - 재고 업데이트 실패");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } finally {
            lock.unlock();
            System.out.println("락을 해제했습니다.");
        }
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
        String redisKey = "product:stock:" + productId;
        Integer stockQuantity = getQuantityFromRedis(redisKey);
        if (stockQuantity != null) {
            return stockQuantity;
        }
        int newQuantity = getQuantityFromDatabase(productId);
        saveQuantityToRedis(redisKey, newQuantity); // redis 업데이트
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
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
        return product.getStockQuantity();
    }

    // Redis에 재고 수량 저장
    private void saveQuantityToRedis(String redisKey, int stockQuantity) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(redisKey, String.valueOf(stockQuantity));
        System.out.println("Redis에 재고 수량을 저장했습니다.");
    }
}
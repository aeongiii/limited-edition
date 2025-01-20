package com.sparta.productservice.service;

import com.sparta.common.dto.ProductDetailResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.ProductSnapshotResponse;
import com.sparta.common.exception.InsufficientStockException;
import com.sparta.common.exception.ProductNotFoundException;
import com.sparta.common.exception.StockUpdateException;
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

    // 재고 수량 업데이트
    public void updateProductStock(Long productId, Integer quantity) {
        String lockKey = "product:lock:"+productId; // 분산락 키 설정
        RLock lock = redissonClient.getFairLock(lockKey);
        System.out.println("분산락 키 설정 완료. lockKey : " + lockKey);

        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) { // 락 획득 시도 (10초 대기, 5초 유지)
                System.out.println("락을 획득했습니다.");
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
                if (quantity < 0) {
                    throw new InsufficientStockException("재고는 0보다 작을 수 없습니다.");
                }
                product.setStockQuantity(quantity);
                productRepository.save(product);
                System.out.println("DB에 재고 업데이트 완료");

                // Redis에 재고 업데이트
                String redisKey = "product:stock:" + productId;
                saveQuantityToRedis(redisKey, quantity, 300);
                System.out.println("Redis에 재고 업데이트 완료");

            } else {
                // 대기시간(10초)동안 락 획득 실패
                throw new StockUpdateException("락 획득 실패 - 재고 업데이트 실패");
            }
        } catch (InterruptedException e) {
            throw new StockUpdateException("락 대기 중 인터럽트 발생");
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



//    // 재고 감소 이벤트 처리
//    @KafkaListener(
//            topics = "product.events.requested",
//            groupId = "product-group",
//            containerFactory = "stockDecrementRequestedKafkaListenerContainerFactory"
//    )
//    public void handleStockDecrementRequestedEvent(StockDecrementRequestedEvent event) {
//        System.out.println("prodctService.handleStockDecrementRequestedEvent에서 StockDecrementRequestedEvent 수신: " + event);
//        try {
//            // 상품 재고 감소 로직
//            Product product = productRepository.findById(event.getProductId())
//                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
//            if (product.getStockQuantity() < event.getQuantity()) {
//                throw new IllegalArgumentException("재고가 부족합니다.");
//            }
//            System.out.println("orderService에서 productService로 보낼 때 설정한 productId : " + product.getId());
//
//            // 스냅샷 생성 및 저장
//            ProductSnapshot snapshot = new ProductSnapshot(product);
//            productSnapshotRepository.save(snapshot);
//            // 재고 감소
//            product.setStockQuantity(product.getStockQuantity() - event.getQuantity());
//            productRepository.save(product);
//            System.out.println(product.getId() + "번 상품 재고 " + product.getStockQuantity() + "개에서 " + (product.getStockQuantity() -event.getQuantity()) + "개로 감소 완료");
//
//            // 재고 감소 완료 이벤트 발행 :
//            StockDecrementCompletedEvent completedEvent = new StockDecrementCompletedEvent();
//            completedEvent.setOrderId(event.getOrderId());
//            System.out.println("productService에서 orderService로 보낼 때 설정한 orderId : " + event.getOrderId());
//            completedEvent.setProductId(event.getProductId());
//            System.out.println("productService에서 orderService로 보낼 때 설정한 productId : " + event.getProductId());
//            completedEvent.setQuantity(event.getQuantity());
//            completedEvent.setTotalOrderItems(event.getTotalOrderItems());
//            completedEvent.setUserResponse(event.getUserResponse());
//            System.out.println("productService에서 orderService로 보낼 때 설정한 userId : " + event.getUserResponse().getId());
//            completedEvent.setOrderItems(event.getOrderItems());
//            productEventProducer.sendStockDecrementCompletedEvent(completedEvent);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("재고 감소 실패 이벤트 발행. 구현 예정입니다.");
//        }
//    }
}
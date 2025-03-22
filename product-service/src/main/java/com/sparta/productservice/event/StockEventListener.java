package com.sparta.productservice.event;

import com.sparta.common.kafkaDto.StockUpdateFailedEvent;
import com.sparta.common.kafkaDto.StockUpdatedEvent;
import com.sparta.productservice.entity.Product;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.service.KafkaProducer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class StockEventListener {

    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final KafkaProducer kafkaProducer;

    public StockEventListener(ProductRepository productRepository, RedissonClient redissonClient, KafkaProducer kafkaProducer) {
        this.productRepository = productRepository;
        this.redissonClient = redissonClient;
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(topics = "product-events", groupId = "product-group")
    @Transactional
    public void handleStockUpdate(StockUpdatedEvent event) {
        System.out.println("StockUpdatedEvent 수신: " + event);

        String lockKey = "product:lock:" + event.getProductId();
        RLock lock = redissonClient.getFairLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("락 획득 실패: " + lockKey);
            }

            // 상품 정보 조회
            Product product = productRepository.findById(event.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + event.getProductId()));

            if ("DECREASE".equals(event.getActionType())) {
                if (product.getStockQuantity() < event.getQuantity()) {
                    throw new IllegalStateException("재고 부족: 현재 재고(" + product.getStockQuantity() + ") 요청된 수량(" + event.getQuantity() + ")");
                }
                product.setStockQuantity(product.getStockQuantity() - event.getQuantity());
                System.out.println("재고 감소 완료. 상품 ID: " + event.getProductId() + ", 남은 재고: " + product.getStockQuantity());

            } else if ("INCREASE".equals(event.getActionType())) {
                product.setStockQuantity(product.getStockQuantity() + event.getQuantity());
                System.out.println("재고 복구 완료. 상품 ID: " + event.getProductId() + ", 현재 재고: " + product.getStockQuantity());

            }
            productRepository.save(product);

        } catch (Exception e) {
            // 실패 이벤트
            StockUpdateFailedEvent failedEvent = new StockUpdateFailedEvent(
                    event.getOrderId(),
                    event.getProductId(),
                    event.getQuantity(),
                    e.getMessage()
            );
            kafkaProducer.sendStockUpdateFailedEvent(failedEvent);
            System.out.println("StockEventListener - StockUpdateFailedEvent 전송 완료");
        } finally {
            if (acquired) {
                lock.unlock();
                System.out.println("락 해제 완료: " + lockKey);
            }
        }
    }
}
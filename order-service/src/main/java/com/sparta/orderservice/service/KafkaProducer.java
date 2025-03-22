package com.sparta.orderservice.service;

import com.sparta.common.kafkaDto.StockUpdateFailedEvent;
import com.sparta.common.kafkaDto.StockUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {
    private final KafkaTemplate<String, StockUpdatedEvent> stockUpdatedKafkaTemplate;
    private final KafkaTemplate<String, StockUpdateFailedEvent> stockUpdateFailedKafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, StockUpdatedEvent> kafkaTemplate, KafkaTemplate<String, StockUpdateFailedEvent> stockUpdateFailedKafkaTemplate) {
        this.stockUpdatedKafkaTemplate = kafkaTemplate;
        this.stockUpdateFailedKafkaTemplate = stockUpdateFailedKafkaTemplate;
    }

    // StockUpdatedEvent 발송
    public void sendStockUpdatedEvent(StockUpdatedEvent event) {
        stockUpdatedKafkaTemplate.send("product-events", event);
        System.out.println("StockUpdatedEvent 전송 완료: " + event);
    }

    // StockUpdateFailedEvent 발송
    public void sendStockUpdateFailedEvent(StockUpdateFailedEvent event) {
        stockUpdateFailedKafkaTemplate.send("stock-update-failed-events", event);
        System.out.println("orderService.KafkaProducer - StockUpdateFailedEvent 전송 완료 : " + event);
    }
}

package com.sparta.productservice.service;

import com.sparta.common.kafkaDto.StockUpdateFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, StockUpdateFailedEvent> stockUpdateFailedKafkaTemplate;


    public KafkaProducer(KafkaTemplate<String, StockUpdateFailedEvent> stockUpdateFailedKafkaTemplate) {
        this.stockUpdateFailedKafkaTemplate = stockUpdateFailedKafkaTemplate;
    }

    public void sendStockUpdateFailedEvent(StockUpdateFailedEvent event) {
        stockUpdateFailedKafkaTemplate.send("stock-update-failed-events", event);
        System.out.println("productservice.KafkaProducer - StockUpdateFailedEvent 전송 완료 : " + event);
    }
}

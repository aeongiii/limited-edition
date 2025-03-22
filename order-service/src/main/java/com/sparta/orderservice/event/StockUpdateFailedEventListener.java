package com.sparta.orderservice.event;

import com.sparta.common.kafkaDto.StockUpdateFailedEvent;
import com.sparta.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class StockUpdateFailedEventListener {

    private final OrderService orderService;

    public StockUpdateFailedEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "stock-update-failed-events", groupId = "order-group")
    public void handleStockUpdateFailed(StockUpdateFailedEvent event) {
        System.out.println("StockUpdateFailedEvent 수신: " + event);
        orderService.deleteOrderWithDetails(event.getOrderId());
    }
}

package com.sparta.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    // 재고 업데이트 이벤트 (order > product)
    public NewTopic stockUpdatedTopic() {
        return new NewTopic("product-events", 1, (short) 1);
    }

    @Bean
    // 재고 업데이트 실패 이벤트 (product > order)
    public NewTopic stockUpdateFailedTopic() {
        return new NewTopic("stock-update-failed-events", 1, (short) 1);
    }
}
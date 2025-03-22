package com.sparta.common.kafkaDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockUpdateFailedEvent {
    private Long orderId;
    private Long productId;
    private int quantity;
    private String failureReason;

}

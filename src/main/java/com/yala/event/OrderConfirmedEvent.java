package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderConfirmedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long buyerId;
    private final Long sellerId;

    public OrderConfirmedEvent(Object source, Long orderId, Long buyerId, Long sellerId) {
        super(source);
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
    }
}

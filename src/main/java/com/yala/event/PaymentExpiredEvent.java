package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentExpiredEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long originalBuyerId;
    private final Long secondBidderId;

    public PaymentExpiredEvent(Object source, Long orderId, Long originalBuyerId, Long secondBidderId) {
        super(source);
        this.orderId = orderId;
        this.originalBuyerId = originalBuyerId;
        this.secondBidderId = secondBidderId;
    }
}

package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SellerVerifiedEvent extends ApplicationEvent {
    private final Long userId;

    public SellerVerifiedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}

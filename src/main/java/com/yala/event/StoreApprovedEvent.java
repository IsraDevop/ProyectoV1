package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StoreApprovedEvent extends ApplicationEvent {
    private final Long storeId;
    private final String email;
    private final String storeName;

    public StoreApprovedEvent(Object source, Long storeId, String email, String storeName) {
        super(source);
        this.storeId = storeId;
        this.email = email;
        this.storeName = storeName;
    }
}

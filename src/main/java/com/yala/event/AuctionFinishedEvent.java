package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuctionFinishedEvent extends ApplicationEvent {
    private final Long auctionId;

    public AuctionFinishedEvent(Object source, Long auctionId) {
        super(source);
        this.auctionId = auctionId;
    }
}

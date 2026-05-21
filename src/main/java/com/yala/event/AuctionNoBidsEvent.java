package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuctionNoBidsEvent extends ApplicationEvent {
    private final Long auctionId;

    public AuctionNoBidsEvent(Object source, Long auctionId) {
        super(source);
        this.auctionId = auctionId;
    }
}

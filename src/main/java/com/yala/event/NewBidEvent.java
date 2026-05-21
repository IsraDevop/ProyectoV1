package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewBidEvent extends ApplicationEvent {
    private final Long auctionId;
    private final Double newAmount;
    private final Long previousBidderId;
    private final Long currentBidderId;

    public NewBidEvent(Object source, Long auctionId, Double newAmount, Long previousBidderId, Long currentBidderId) {
        super(source);
        this.auctionId = auctionId;
        this.newAmount = newAmount;
        this.previousBidderId = previousBidderId;
        this.currentBidderId = currentBidderId;
    }
}

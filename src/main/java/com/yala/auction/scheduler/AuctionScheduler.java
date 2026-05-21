package com.yala.auction.scheduler;

import com.yala.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionService auctionService;

    @Scheduled(fixedRate = 60000)
    public void processAuctions() {
        log.debug("Running auction scheduler");
        auctionService.activateScheduledAuctions();
        auctionService.closeExpiredAuctions();
    }
}

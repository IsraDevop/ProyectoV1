package com.yala.order.scheduler;

import com.yala.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 0 * * * *")
    public void processOrders() {
        log.debug("Running order scheduler");
        orderService.processExpiredOrders();
        orderService.autoCompleteOrders();
    }
}

package com.yala.order.service;

import com.yala.order.dto.CreateOrderRequest;
import com.yala.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    Page<OrderResponse> getMyOrders(Pageable pageable);
    OrderResponse getOrderById(Long id);
    OrderResponse confirmOrder(Long id);
    OrderResponse cancelOrder(Long id);
    OrderResponse shipOrder(Long id, String trackingNumber);
    OrderResponse completeOrder(Long id);
    void processExpiredOrders();
    void autoCompleteOrders();
}

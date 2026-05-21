package com.yala.order.controller;

import com.yala.order.dto.CreateOrderRequest;
import com.yala.order.dto.OrderResponse;
import com.yala.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a direct purchase order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get authenticated user orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Confirm order (SELLER only)")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order (buyer, only before payment)")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "Mark order as shipped")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable Long id,
                                                    @RequestParam(required = false) String trackingNumber) {
        return ResponseEntity.ok(orderService.shipOrder(id, trackingNumber));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Buyer confirms receipt — completes order")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }
}

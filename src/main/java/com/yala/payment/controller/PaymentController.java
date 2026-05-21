package com.yala.payment.controller;

import com.yala.payment.dto.CreatePaymentIntentRequest;
import com.yala.payment.dto.PaymentIntentResponse;
import com.yala.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe payment processing")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intent")
    @Operation(summary = "Create a Stripe PaymentIntent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPaymentIntent(request));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook endpoint (public)")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}

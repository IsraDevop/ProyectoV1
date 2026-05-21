package com.yala.payment.service;

import com.yala.payment.dto.CreatePaymentIntentRequest;
import com.yala.payment.dto.PaymentIntentResponse;

public interface PaymentService {
    PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);
    void handleWebhook(String payload, String sigHeader);
}

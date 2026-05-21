package com.yala.email.service;

public interface EmailService {
    void sendWelcomeEmail(String to, String name);
    void sendAuctionWonEmail(String to, String name, double amount);
    void sendPaymentConfirmedEmail(String to, String name);
    void sendPaymentExpiredEmail(String to, String name);
    void sendStoreApprovedEmail(String to, String storeName);
}

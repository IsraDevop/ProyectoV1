package com.yala.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${resend.from:noreply@yala.pe}")
    private String fromEmail;

    @Override
    public void sendWelcomeEmail(String to, String name) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        sendEmail(to, "Welcome to Yala!", "email/welcome", ctx);
    }

    @Override
    public void sendAuctionWonEmail(String to, String name, double amount) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("amount", amount);
        sendEmail(to, "You won an auction!", "email/auction-won", ctx);
    }

    @Override
    public void sendPaymentConfirmedEmail(String to, String name) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        sendEmail(to, "Payment confirmed - Yala", "email/payment-confirmed", ctx);
    }

    @Override
    public void sendPaymentExpiredEmail(String to, String name) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        sendEmail(to, "Payment deadline passed - Yala", "email/payment-expired", ctx);
    }

    @Override
    public void sendStoreApprovedEmail(String to, String storeName) {
        Context ctx = new Context();
        ctx.setVariable("storeName", storeName);
        sendEmail(to, "Your store has been approved!", "email/store-approved", ctx);
    }

    private void sendEmail(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {} - subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

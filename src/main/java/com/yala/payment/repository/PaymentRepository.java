package com.yala.payment.repository;

import com.yala.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByExternalReference(String externalReference);
}

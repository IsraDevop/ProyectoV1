package com.yala.payment.model;

import com.yala.order.model.Order;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String gateway;

    private String externalReference;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime attemptedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}

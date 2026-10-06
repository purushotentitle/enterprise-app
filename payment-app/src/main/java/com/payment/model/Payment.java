package com.payment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Column(nullable = false)
    private String fromAccount;

    @NotBlank
    @Column(nullable = false)
    private String toAccount;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank
    private String currency;     // e.g. USD, EUR

    @Enumerated(EnumType.STRING)
    private MessageStandard standard; // ISO_8583 or ISO_20022

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String description;

    private String referenceId;  // external transaction ref

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime processedAt;

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REVERSED
    }

    public enum MessageStandard {
        ISO_8583,   // card/POS transactions
        ISO_20022   // SEPA / modern bank transfers
    }
}

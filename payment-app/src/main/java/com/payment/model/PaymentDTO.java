package com.payment.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "fromAccount is required")
        private String fromAccount;

        @NotBlank(message = "toAccount is required")
        private String toAccount;

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be > 0")
        private BigDecimal amount;

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be 3-char ISO code")
        private String currency;

        private String description;

        // ISO_8583 or ISO_20022 — defaults to ISO_20022
        private Payment.MessageStandard standard = Payment.MessageStandard.ISO_20022;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private String currency;
        private Payment.MessageStandard standard;
        private Payment.PaymentStatus status;
        private String description;
        private String referenceId;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
        private String message;
    }
}

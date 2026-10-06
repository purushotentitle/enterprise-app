package com.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.config.CorrelationIdFilter;
import com.payment.config.KafkaTopicConfig;
import com.payment.model.PaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYMENT_INITIATED, groupId = "payment-group")
    public void onPaymentInitiated(
            String message,
            @Header(name = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) byte[] correlationIdBytes) {
        setupMdc(correlationIdBytes);
        try {
            PaymentDTO.Response payment = objectMapper.readValue(message, PaymentDTO.Response.class);
            log.info("[CONSUMER] Payment initiated event received: id={} amount={} {}",
                    payment.getId(), payment.getAmount(), payment.getCurrency());
        } catch (Exception e) {
            log.error("[CONSUMER] Error processing payment initiated message: {}", e.getMessage(), e);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYMENT_PROCESSED, groupId = "payment-group")
    public void onPaymentProcessed(
            String message,
            @Header(name = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) byte[] correlationIdBytes) {
        setupMdc(correlationIdBytes);
        try {
            log.info("[CONSUMER] Payment processed event received: {}", message);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYMENT_FAILED, groupId = "payment-group")
    public void onPaymentFailed(
            String message,
            @Header(name = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) byte[] correlationIdBytes) {
        setupMdc(correlationIdBytes);
        try {
            log.warn("[CONSUMER] Payment failed/reversed event received: {}", message);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    private void setupMdc(byte[] correlationIdBytes) {
        if (correlationIdBytes != null && correlationIdBytes.length > 0) {
            MDC.put(CorrelationIdFilter.MDC_KEY, new String(correlationIdBytes));
        }
    }
}

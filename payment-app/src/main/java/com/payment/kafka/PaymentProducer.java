package com.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.config.CorrelationIdFilter;
import com.payment.model.PaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPaymentInitiated(String topic, PaymentDTO.Response payment) {
        try {
            String payload = objectMapper.writeValueAsString(payment);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, payment.getId(), payload);

            // Propagate correlation ID across Kafka headers for distributed tracing
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null) {
                record.headers().add(new RecordHeader(
                        CorrelationIdFilter.CORRELATION_ID_HEADER,
                        correlationId.getBytes(StandardCharsets.UTF_8)));
            }

            kafkaTemplate.send(record);
            log.info("Published to [{}] paymentId={} correlationId={}", topic, payment.getId(), correlationId);
        } catch (Exception e) {
            log.error("Failed to publish payment event: {}", e.getMessage(), e);
        }
    }
}

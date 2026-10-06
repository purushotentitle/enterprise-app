package com.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.config.PaymentMetrics;
import com.payment.config.KafkaTopicConfig;
import com.payment.iso.Iso8583Service;
import com.payment.iso.Iso20022Service;
import com.payment.kafka.PaymentProducer;
import com.payment.model.IdempotencyKey;
import com.payment.model.Payment;
import com.payment.model.PaymentDTO;
import com.payment.repository.IdempotencyRepository;
import com.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final PaymentProducer paymentProducer;
    private final PaymentMetrics paymentMetrics;
    private final Iso8583Service iso8583Service;
    private final Iso20022Service iso20022Service;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────
    // CREATE (with Distributed Idempotency)
    // ─────────────────────────────────────────────
    @Transactional
    public PaymentDTO.Response createPayment(PaymentDTO.Request request, String idempotencyKey) {
        // If idempotency key is provided, check for existing transaction
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<IdempotencyKey> existingKey = idempotencyRepository.findByKey(idempotencyKey);
            if (existingKey.isPresent()) {
                IdempotencyKey keyRecord = existingKey.get();
                if (keyRecord.getStatus() == IdempotencyKey.IdempotencyStatus.COMPLETED) {
                    log.info("[Idempotency] Cached response hit for key: {}", idempotencyKey);
                    try {
                        return objectMapper.readValue(keyRecord.getResponsePayload(), PaymentDTO.Response.class);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize idempotency cached response, re-fetching payment", e);
                        return getPayment(keyRecord.getPaymentId());
                    }
                }
            }
        }

        log.info("Creating payment: from={} to={} amount={} {} standard={}",
                request.getFromAccount(), request.getToAccount(),
                request.getAmount(), request.getCurrency(), request.getStandard());

        Payment payment = Payment.builder()
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .standard(request.getStandard())
                .description(request.getDescription())
                .referenceId(generateReferenceId(request.getStandard()))
                .status(Payment.PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        paymentMetrics.recordPaymentCreated(request.getStandard());
        PaymentDTO.Response response = toResponse(payment, "Payment created successfully");

        // Save Idempotency record
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            try {
                IdempotencyKey keyRecord = IdempotencyKey.builder()
                        .key(idempotencyKey)
                        .paymentId(payment.getId())
                        .requestHash(Integer.toHexString(request.hashCode()))
                        .status(IdempotencyKey.IdempotencyStatus.COMPLETED)
                        .responsePayload(objectMapper.writeValueAsString(response))
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build();
                idempotencyRepository.save(keyRecord);
            } catch (Exception e) {
                log.error("Failed to store idempotency record", e);
            }
        }

        // Publish to Kafka
        paymentProducer.sendPaymentInitiated(KafkaTopicConfig.TOPIC_PAYMENT_INITIATED, response);

        return response;
    }

    @Transactional
    public PaymentDTO.Response createPayment(PaymentDTO.Request request) {
        return createPayment(request, null);
    }

    // ─────────────────────────────────────────────
    // PROCESS (Production ISO 8583 / ISO 20022 Engine)
    // ─────────────────────────────────────────────
    @Transactional
    public PaymentDTO.Response processPayment(String paymentId) {
        Payment payment = findOrThrow(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING state: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        // Process via dedicated ISO message services
        Payment finalPayment = payment;
        String processingNote = paymentMetrics.getProcessingTimer().record(() -> {
            if (finalPayment.getStandard() == Payment.MessageStandard.ISO_8583) {
                return iso8583Service.processAndAuthorize(finalPayment);
            } else {
                return iso20022Service.processPacs008(finalPayment);
            }
        });

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        paymentMetrics.recordPaymentCompleted();
        PaymentDTO.Response response = toResponse(payment, processingNote);
        paymentProducer.sendPaymentInitiated(KafkaTopicConfig.TOPIC_PAYMENT_PROCESSED, response);

        return response;
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────
    public PaymentDTO.Response getPayment(String paymentId) {
        return toResponse(findOrThrow(paymentId), null);
    }

    public List<PaymentDTO.Response> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(p -> toResponse(p, null))
                .collect(Collectors.toList());
    }

    public List<PaymentDTO.Response> getPaymentsByAccount(String account) {
        return paymentRepository.findByFromAccount(account)
                .stream()
                .map(p -> toResponse(p, null))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // REVERSE
    // ─────────────────────────────────────────────
    @Transactional
    public PaymentDTO.Response reversePayment(String paymentId) {
        Payment payment = findOrThrow(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED payments can be reversed");
        }

        payment.setStatus(Payment.PaymentStatus.REVERSED);
        payment = paymentRepository.save(payment);
        paymentMetrics.recordPaymentReversed();
        PaymentDTO.Response response = toResponse(payment, "Payment reversed and refunded");
        paymentProducer.sendPaymentInitiated(KafkaTopicConfig.TOPIC_PAYMENT_FAILED, response);

        return response;
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────
    private String generateReferenceId(Payment.MessageStandard standard) {
        String prefix = (standard == Payment.MessageStandard.ISO_8583) ? "POS" : "SEPA";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Payment findOrThrow(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    private PaymentDTO.Response toResponse(Payment p, String message) {
        return PaymentDTO.Response.builder()
                .id(p.getId())
                .fromAccount(p.getFromAccount())
                .toAccount(p.getToAccount())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .standard(p.getStandard())
                .status(p.getStatus())
                .description(p.getDescription())
                .referenceId(p.getReferenceId())
                .createdAt(p.getCreatedAt())
                .processedAt(p.getProcessedAt())
                .message(message)
                .build();
    }
}

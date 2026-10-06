package com.payment.config;

import com.payment.model.Payment;
import com.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;
    private final PaymentRepository paymentRepository;

    private Counter paymentsCreatedTotal;
    private Counter iso8583Counter;
    private Counter iso20022Counter;
    private Counter paymentsCompletedTotal;
    private Counter paymentsFailedTotal;
    private Counter paymentsReversedTotal;
    private Timer paymentProcessingTimer;

    @PostConstruct
    public void initMetrics() {
        paymentsCreatedTotal = Counter.builder("payment.created.total")
                .description("Total payments created")
                .register(meterRegistry);

        iso8583Counter = Counter.builder("payment.standard.total")
                .tag("standard", "ISO_8583")
                .register(meterRegistry);

        iso20022Counter = Counter.builder("payment.standard.total")
                .tag("standard", "ISO_20022")
                .register(meterRegistry);

        paymentsCompletedTotal = Counter.builder("payment.status.total")
                .tag("status", "COMPLETED")
                .register(meterRegistry);

        paymentsFailedTotal = Counter.builder("payment.status.total")
                .tag("status", "FAILED")
                .register(meterRegistry);

        paymentsReversedTotal = Counter.builder("payment.status.total")
                .tag("status", "REVERSED")
                .register(meterRegistry);

        paymentProcessingTimer = Timer.builder("payment.processing.duration")
                .description("Time taken to process a payment")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        Gauge.builder("payment.pending.count", paymentRepository,
                        repo -> repo.findByStatus(Payment.PaymentStatus.PENDING).size())
                .description("Payments in PENDING state")
                .register(meterRegistry);

        log.info("Payment metrics initialized");
    }

    public void recordPaymentCreated(Payment.MessageStandard standard) {
        paymentsCreatedTotal.increment();
        if (standard == Payment.MessageStandard.ISO_8583) {
            iso8583Counter.increment();
        } else {
            iso20022Counter.increment();
        }
    }

    public void recordPaymentCompleted() { paymentsCompletedTotal.increment(); }
    public void recordPaymentFailed()    { paymentsFailedTotal.increment(); }
    public void recordPaymentReversed()  { paymentsReversedTotal.increment(); }
    public Timer getProcessingTimer()    { return paymentProcessingTimer; }
}
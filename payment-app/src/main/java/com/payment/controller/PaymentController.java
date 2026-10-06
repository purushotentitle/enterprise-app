package com.payment.controller;

import com.payment.model.PaymentDTO;
import com.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/v1/payments
    @PostMapping
    public ResponseEntity<PaymentDTO.Response> create(
            @Valid @RequestBody PaymentDTO.Request request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request, idempotencyKey));
    }

    // GET /api/v1/payments
    @GetMapping
    public ResponseEntity<List<PaymentDTO.Response>> getAll() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // GET /api/v1/payments/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO.Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    // GET /api/v1/payments/account/{account}
    @GetMapping("/account/{account}")
    public ResponseEntity<List<PaymentDTO.Response>> getByAccount(@PathVariable String account) {
        return ResponseEntity.ok(paymentService.getPaymentsByAccount(account));
    }

    // POST /api/v1/payments/{id}/process
    @PostMapping("/{id}/process")
    public ResponseEntity<PaymentDTO.Response> process(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.processPayment(id));
    }

    // POST /api/v1/payments/{id}/reverse
    @PostMapping("/{id}/reverse")
    public ResponseEntity<PaymentDTO.Response> reverse(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.reversePayment(id));
    }
}

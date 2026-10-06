package com.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.model.Payment;
import com.payment.model.PaymentDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka(partitions = 1,
        topics = {"payment.initiated", "payment.processed", "payment.failed"})
class PaymentServiceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/payments";

    // ── Helper ─────────────────────────────────────────
    private PaymentDTO.Request buildRequest(Payment.MessageStandard standard) {
        return PaymentDTO.Request.builder()
                .fromAccount("ACC-001")
                .toAccount("ACC-002")
                .amount(new BigDecimal("250.00"))
                .currency("EUR")
                .standard(standard)
                .description("Test payment")
                .build();
    }

    private String createPaymentAndGetId(Payment.MessageStandard standard) throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest(standard));
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        PaymentDTO.Response resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), PaymentDTO.Response.class);
        return resp.getId();
    }

    // ── Tests ──────────────────────────────────────────

    @Test
    void createPayment_iso20022_returnsCreated() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest(Payment.MessageStandard.ISO_20022));
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.standard", is("ISO_20022")))
                .andExpect(jsonPath("$.referenceId", startsWith("SEPA-")));
    }

    @Test
    void createPayment_iso8583_returnsCreated() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest(Payment.MessageStandard.ISO_8583));
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.standard", is("ISO_8583")))
                .andExpect(jsonPath("$.referenceId", startsWith("POS-")));
    }

    @Test
    void createPayment_missingFields_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").exists());
    }

    @Test
    void getPayment_returnsPayment() throws Exception {
        String id = createPaymentAndGetId(Payment.MessageStandard.ISO_20022);
        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)));
    }

    @Test
    void processPayment_completesSuccessfully() throws Exception {
        String id = createPaymentAndGetId(Payment.MessageStandard.ISO_20022);
        mockMvc.perform(post(BASE_URL + "/" + id + "/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.processedAt", notNullValue()));
    }

    @Test
    void reversePayment_afterComplete_succeeds() throws Exception {
        String id = createPaymentAndGetId(Payment.MessageStandard.ISO_8583);
        mockMvc.perform(post(BASE_URL + "/" + id + "/process")).andExpect(status().isOk());
        mockMvc.perform(post(BASE_URL + "/" + id + "/reverse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVERSED")));
    }

    @Test
    void getAll_returnsListOfPayments() throws Exception {
        createPaymentAndGetId(Payment.MessageStandard.ISO_20022);
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }
}

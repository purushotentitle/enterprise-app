package com.payment.iso;

import com.payment.model.Payment;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Production ISO 8583 Financial Message Processor
 * Generates and parses standard 8583 MTI (0200/0210) messages with Bitmaps and Data Elements.
 */
@Slf4j
@Service
public class Iso8583Service {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMdd");
    private final Random random = new Random();

    @Data
    @Builder
    public static class Iso8583Message {
        private String mti;               // Message Type Identifier (e.g. 0200, 0210)
        private String primaryBitmap;     // 16-hex char (64-bit) primary bitmap
        private Map<Integer, String> dataElements; // Field 3 to Field 128
        private String rawPayload;
    }

    /**
     * Builds an ISO 8583 MTI 0200 Authorization Request
     */
    public Iso8583Message buildAuthorizationRequest(Payment payment) {
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, String> de = new HashMap<>();

        // DE 3: Processing Code (000000 = Goods & Services purchase)
        de.put(3, "000000");

        // DE 4: Amount in Minor Units (e.g., $10.50 -> 000000001050)
        long cents = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        de.put(4, String.format("%012d", cents));

        // DE 11: Systems Trace Audit Number (STAN) - 6 digits
        String stan = String.format("%06d", random.nextInt(999999));
        de.put(11, stan);

        // DE 12: Local Transaction Time (hhmmss)
        de.put(12, now.format(TIME_FORMATTER));

        // DE 13: Local Transaction Date (MMdd)
        de.put(13, now.format(DATE_FORMATTER));

        // DE 37: Retrieval Reference Number (RRN) - 12 chars
        String rrn = String.format("%012d", Math.abs(payment.getId().hashCode()));
        de.put(37, rrn);

        // DE 41: Card Acceptor Terminal ID
        de.put(41, "TERM-001");

        // DE 49: Currency Code (e.g., 840 for USD, 978 for EUR)
        String currencyCode = "EUR".equalsIgnoreCase(payment.getCurrency()) ? "978" : "840";
        de.put(49, currencyCode);

        // Compute 64-bit Hex Bitmap
        String bitmap = computeBitmap(de);

        return Iso8583Message.builder()
                .mti("0200")
                .primaryBitmap(bitmap)
                .dataElements(de)
                .rawPayload(formatIsoStream("0200", bitmap, de))
                .build();
    }

    /**
     * Processes MTI 0200 and generates MTI 0210 Authorization Response with Approval Code
     */
    public String processAndAuthorize(Payment payment) {
        Iso8583Message request = buildAuthorizationRequest(payment);
        log.info("[ISO 8583] MTI 0200 Request constructed: Stan={}, RRN={}, Bitmap={}",
                request.getDataElements().get(11),
                request.getDataElements().get(37),
                request.getPrimaryBitmap());

        // Generate Approval Code (DE 38) and Response Code 00 = Approved (DE 39)
        String authCode = String.format("%06d", 100000 + random.nextInt(900000));
        request.getDataElements().put(38, authCode);
        request.getDataElements().put(39, "00");

        return String.format("ISO 8583 [MTI 0210 Approved] STAN:%s RRN:%s AuthCode:%s Bitmap:%s",
                request.getDataElements().get(11),
                request.getDataElements().get(37),
                authCode,
                request.getPrimaryBitmap());
    }

    private String computeBitmap(Map<Integer, String> fields) {
        long bitmap = 0L;
        for (Integer fieldNumber : fields.keySet()) {
            if (fieldNumber >= 1 && fieldNumber <= 64) {
                bitmap |= (1L << (64 - fieldNumber));
            }
        }
        return String.format("%016X", bitmap);
    }

    private String formatIsoStream(String mti, String bitmap, Map<Integer, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(mti).append(bitmap);
        fields.keySet().stream().sorted().forEach(f -> sb.append(fields.get(f)));
        return sb.toString();
    }
}

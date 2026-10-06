package com.payment.iso;

import com.payment.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Production ISO 20022 SEPA pacs.008 XML Message Generator
 * Implements Financial Institutional Customer Credit Transfer message structure.
 */
@Slf4j
@Service
public class Iso20022Service {

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Generates structured pacs.008.001.10 XML document
     */
    public String buildPacs008Xml(Payment payment, String uetr) {
        String msgId = "MSG-" + UUID.randomUUID().toString().substring(0, 16).toUpperCase();
        String endToEndId = "E2E-" + payment.getReferenceId();
        String creationDateTime = LocalDateTime.now().format(ISO_DATE_TIME);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.10\">\n" +
                "  <FIToFICstmrCdtTrf>\n" +
                "    <GrpHdr>\n" +
                "      <MsgId>" + msgId + "</MsgId>\n" +
                "      <CreDtTm>" + creationDateTime + "</CreDtTm>\n" +
                "      <NbOfTxs>1</NbOfTxs>\n" +
                "      <TtlIntrBkSttlmAmt Ccy=\"" + payment.getCurrency() + "\">" + payment.getAmount() + "</TtlIntrBkSttlmAmt>\n" +
                "      <SttlmInf>\n" +
                "        <SttlmMtd>CLRG</SttlmMtd>\n" +
                "      </SttlmInf>\n" +
                "    </GrpHdr>\n" +
                "    <CdtTrfTxInf>\n" +
                "      <PmtId>\n" +
                "        <EndToEndId>" + endToEndId + "</EndToEndId>\n" +
                "        <UETR>" + uetr + "</UETR>\n" +
                "      </PmtId>\n" +
                "      <IntrBkSttlmAmt Ccy=\"" + payment.getCurrency() + "\">" + payment.getAmount() + "</IntrBkSttlmAmt>\n" +
                "      <DbtrAcct>\n" +
                "        <Id><IBAN>" + payment.getFromAccount() + "</IBAN></Id>\n" +
                "      </DbtrAcct>\n" +
                "      <CdtrAcct>\n" +
                "        <Id><IBAN>" + payment.getToAccount() + "</IBAN></Id>\n" +
                "      </CdtrAcct>\n" +
                "      <RmtInf>\n" +
                "        <Ustrd>" + (payment.getDescription() != null ? payment.getDescription() : "Payment Transfer") + "</Ustrd>\n" +
                "      </RmtInf>\n" +
                "    </CdtTrfTxInf>\n" +
                "  </FIToFICstmrCdtTrf>\n" +
                "</Document>";
    }

    public String processPacs008(Payment payment) {
        String uetr = UUID.randomUUID().toString();
        String xml = buildPacs008Xml(payment, uetr);
        log.info("[ISO 20022] pacs.008 Credit Transfer XML generated successfully. UETR: {}", uetr);
        log.debug("[ISO 20022] Message Body:\n{}", xml);
        return String.format("ISO 20022 [pacs.008.001.10 Cleared] UETR:%s E2E:%s", uetr, payment.getReferenceId());
    }
}

package com.telcostream.bss.consumer;

import com.telcostream.bss.entity.Invoice;
import com.telcostream.bss.model.CdrRecord;
import com.telcostream.bss.service.BalanceService;
import com.telcostream.bss.service.CdrValidationService;
import com.telcostream.bss.service.CdrValidationService.ValidatedCdr;
import com.telcostream.bss.service.InvoiceBatchService;
import com.telcostream.bss.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Entry point of the pipeline: raw-network-cdrs -> validate -> rate ->
 * idempotency-check -> deduct balance -> hand off to the batch writer.
 * Anything that fails validation is routed to the DLQ topic instead of
 * being dropped or crashing the consumer.
 */
@Component
public class CdrConsumer {

    private static final Logger log = LoggerFactory.getLogger(CdrConsumer.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    private final CdrValidationService validationService;
    private final RatingService ratingService;
    private final BalanceService balanceService;
    private final InvoiceBatchService invoiceBatchService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String dlqTopic;

    public CdrConsumer(CdrValidationService validationService,
                        RatingService ratingService,
                        BalanceService balanceService,
                        InvoiceBatchService invoiceBatchService,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        @Value("${bss.topics.dlq}") String dlqTopic) {
        this.validationService = validationService;
        this.ratingService = ratingService;
        this.balanceService = balanceService;
        this.invoiceBatchService = invoiceBatchService;
        this.kafkaTemplate = kafkaTemplate;
        this.dlqTopic = dlqTopic;
    }

    @KafkaListener(topics = "${bss.topics.raw-cdrs}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(String rawJson, Acknowledgment ack) {
        try {
            CdrRecord raw = OBJECT_MAPPER.readValue(rawJson, CdrRecord.class);
            Optional<ValidatedCdr> validated = validationService.validate(raw);
            if (validated.isEmpty()) {
                sendToDlq(raw);
                ack.acknowledge();
                return;
            }

            ValidatedCdr cdr = validated.get();

            if (balanceService.isDuplicate(cdr.recordId())) {
                log.debug("Skipping duplicate/redelivered record {}", cdr.recordId());
                ack.acknowledge();
                return;
            }

            BigDecimal charge = ratingService.rate(cdr.serviceType(), cdr.durationSeconds(), cdr.dataVolumeMb());
            balanceService.deduct(cdr.callingNumber(), charge);

            Invoice invoice = new Invoice(
                    cdr.recordId(),
                    cdr.callingNumber(),
                    cdr.calledNumber(),
                    cdr.serviceType().name(),
                    cdr.durationSeconds(),
                    charge,
                    cdr.locationCellId(),
                    cdr.eventTimestamp()
            );
            invoiceBatchService.add(invoice);

            ack.acknowledge();
        } catch (Exception e) {
            // Never let one bad message take down the listener container.
            log.error("Failed to process CDR payload {}, routing to DLQ", rawJson, e);
            sendToDlq(rawJson);
            ack.acknowledge();
        }
    }

    private void sendToDlq(CdrRecord raw) {
        kafkaTemplate.send(dlqTopic, raw.getRecordId(), raw);
    }

    private void sendToDlq(String rawJson) {
        kafkaTemplate.send(dlqTopic, null, rawJson);
    }
}

package com.banking.shadowledger.kafka;

import com.banking.shadowledger.dto.TransactionEventDto;
import com.banking.shadowledger.service.LedgerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    public TransactionConsumer(LedgerService ledgerService, ObjectMapper objectMapper) {
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"transactions.raw", "transactions.corrections"}, groupId = "shadow-ledger-group")
    public void consumeTransaction(String message) {
        String traceId = java.util.UUID.randomUUID().toString();
        MDC.put("X-Trace-Id", traceId);

        try {
            TransactionEventDto event = objectMapper.readValue(message, TransactionEventDto.class);
            logger.info("Consumed event: {} for account: {}", event.getEventId(), event.getAccountId());

            ledgerService.processEvent(event);

            logger.info("Successfully processed event: {}", event.getEventId());
        } catch (Exception e) {
            logger.error("Failed to process message: {}", message, e);
        } finally {
            MDC.clear();
        }
    }
}

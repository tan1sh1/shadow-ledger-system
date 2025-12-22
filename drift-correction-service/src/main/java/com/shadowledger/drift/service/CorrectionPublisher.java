package com.shadowledger.drift.service;

import com.shadowledger.drift.model.CorrectionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CorrectionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(CorrectionPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CorrectionPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(CorrectionEvent event) {
        logger.info("Publishing CorrectionEvent to Kafka: {}", event);
        kafkaTemplate.send("transactions.corrections", event.getAccountId(), event);
        logger.info("CorrectionEvent sent to topic 'transactions.corrections' for accountId={}", event.getAccountId());
    }
}

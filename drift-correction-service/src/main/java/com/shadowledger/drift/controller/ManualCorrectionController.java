package com.shadowledger.drift.controller;

import com.shadowledger.drift.model.CorrectionEvent;
import com.shadowledger.drift.service.CorrectionPublisher;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/correct")
public class ManualCorrectionController {

    private static final Logger logger = LoggerFactory.getLogger(ManualCorrectionController.class);

    private final CorrectionPublisher publisher;

    public ManualCorrectionController(CorrectionPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/{accountId}")
    public void correct(@PathVariable String accountId,
                        @RequestParam BigDecimal amount) {
        logger.info("Manual correction requested for accountId={}, amount={}", accountId, amount);

        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("MANUAL-" + UUID.randomUUID())
                .accountId(accountId)
                .type("credit")
                .amount(amount)
                .build();

        publisher.publish(event);

        logger.info("CorrectionEvent published: {}", event);
    }
}

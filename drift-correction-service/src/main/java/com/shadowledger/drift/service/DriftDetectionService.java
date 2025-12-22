package com.shadowledger.drift.service;

import com.shadowledger.drift.model.CbsBalance;
import com.shadowledger.drift.model.CorrectionEvent;
import com.shadowledger.drift.repository.ShadowLedgerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DriftDetectionService {

    private final ShadowLedgerRepository repository;
    private final CorrectionPublisher publisher;

    public DriftDetectionService(ShadowLedgerRepository repository,
                                 CorrectionPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public void checkAndCorrect(CbsBalance cbs) {
        var shadow = repository.findBalance(cbs.getAccountId());
        if (shadow.isEmpty()) return;

        BigDecimal diff =
                cbs.getReportedBalance().subtract(shadow.get().getBalance());

        if (diff.compareTo(BigDecimal.ZERO) == 0) return;

        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-" + UUID.randomUUID())
                .accountId(cbs.getAccountId())
                .type(diff.signum() > 0 ? "credit" : "debit")
                .amount(diff.abs())
                .build();

        publisher.publish(event);
    }
}


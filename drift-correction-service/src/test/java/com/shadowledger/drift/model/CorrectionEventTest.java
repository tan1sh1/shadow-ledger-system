package com.shadowledger.drift.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class CorrectionEventTest {

    @Test
    void testBuilderCreatesCorrectEvent() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-123")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        assertThat(event.getEventId()).isEqualTo("CORR-123");
        assertThat(event.getAccountId()).isEqualTo("acc-001");
        assertThat(event.getType()).isEqualTo("credit");
        assertThat(event.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void testCreditEvent() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-001")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("500.00"))
                .build();

        assertThat(event.getType()).isEqualTo("credit");
        assertThat(event.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void testDebitEvent() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-002")
                .accountId("acc-002")
                .type("debit")
                .amount(new BigDecimal("300.00"))
                .build();

        assertThat(event.getType()).isEqualTo("debit");
        assertThat(event.getAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void testEventWithLargeAmount() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-003")
                .accountId("acc-003")
                .type("credit")
                .amount(new BigDecimal("999999999.99"))
                .build();

        assertThat(event.getAmount()).isEqualByComparingTo("999999999.99");
    }

    @Test
    void testEventWithSmallAmount() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-004")
                .accountId("acc-004")
                .type("debit")
                .amount(new BigDecimal("0.01"))
                .build();

        assertThat(event.getAmount()).isEqualByComparingTo("0.01");
    }

    @Test
    void testEventIdPrefix() {
        CorrectionEvent manualEvent = CorrectionEvent.builder()
                .eventId("MANUAL-UUID")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        CorrectionEvent autoEvent = CorrectionEvent.builder()
                .eventId("CORR-UUID")
                .accountId("acc-002")
                .type("debit")
                .amount(new BigDecimal("50.00"))
                .build();

        assertThat(manualEvent.getEventId()).startsWith("MANUAL-");
        assertThat(autoEvent.getEventId()).startsWith("CORR-");
    }

    @Test
    void testEventWithDecimalPrecision() {
        CorrectionEvent event = CorrectionEvent.builder()
                .eventId("CORR-005")
                .accountId("acc-005")
                .type("credit")
                .amount(new BigDecimal("123.456"))
                .build();

        assertThat(event.getAmount().toPlainString()).isEqualTo("123.456");
    }

    @Test
    void testMultipleEventsIndependence() {
        CorrectionEvent event1 = CorrectionEvent.builder()
                .eventId("CORR-001")
                .accountId("acc-001")
                .type("credit")
                .amount(new BigDecimal("100.00"))
                .build();

        CorrectionEvent event2 = CorrectionEvent.builder()
                .eventId("CORR-002")
                .accountId("acc-002")
                .type("debit")
                .amount(new BigDecimal("200.00"))
                .build();

        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        assertThat(event1.getAccountId()).isNotEqualTo(event2.getAccountId());
        assertThat(event1.getType()).isNotEqualTo(event2.getType());
        assertThat(event1.getAmount()).isNotEqualByComparingTo(event2.getAmount());
    }
}


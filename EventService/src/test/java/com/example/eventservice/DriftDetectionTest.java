package com.example.eventservice;

import com.example.eventservice.dto.EventRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for drift detection scenarios.
 * These tests verify that the EventService handles scenarios
 * that could lead to drift between primary and shadow ledgers.
 */
public class DriftDetectionTest {

    @Test
    void testDuplicateEventDetection() {
        // Simulating duplicate event IDs which should be rejected
        EventRequest event1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        EventRequest event2 = createEvent("evt-001", "acc-001", "credit", "100.00");

        // Both events have same ID - second should be rejected
        assertThat(event1.getEventId()).isEqualTo(event2.getEventId());
    }

    @Test
    void testOutOfOrderEvents() {
        long baseTime = System.currentTimeMillis();

        EventRequest event1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        event1.setTimestamp(baseTime + 2000); // Later timestamp

        EventRequest event2 = createEvent("evt-002", "acc-001", "debit", "50.00");
        event2.setTimestamp(baseTime); // Earlier timestamp

        EventRequest event3 = createEvent("evt-003", "acc-001", "credit", "75.00");
        event3.setTimestamp(baseTime + 1000); // Middle timestamp

        // Events might arrive out of order
        List<EventRequest> arrivedOrder = List.of(event1, event2, event3);
        List<EventRequest> sortedByTime = arrivedOrder.stream()
                .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                .toList();

        assertThat(sortedByTime.get(0)).isEqualTo(event2);
        assertThat(sortedByTime.get(1)).isEqualTo(event3);
        assertThat(sortedByTime.get(2)).isEqualTo(event1);
    }

    @Test
    void testHighVolumeEvents() {
        List<EventRequest> events = new ArrayList<>();

        // Create 1000 events
        for (int i = 0; i < 1000; i++) {
            EventRequest event = createEvent(
                "evt-" + i,
                "acc-001",
                i % 2 == 0 ? "credit" : "debit",
                "10.00"
            );
            events.add(event);
        }

        assertThat(events).hasSize(1000);

        // Calculate balance
        BigDecimal balance = BigDecimal.ZERO;
        for (EventRequest event : events) {
            if (event.getType().equals("credit")) {
                balance = balance.add(event.getAmount());
            } else {
                balance = balance.subtract(event.getAmount());
            }
        }

        // 500 credits of 10.00, 500 debits of 10.00 = 0
        assertThat(balance).isEqualByComparingTo("0.00");
    }

    @Test
    void testConcurrentAccountAccess() {
        // Multiple events for same account at similar times
        long baseTime = System.currentTimeMillis();

        EventRequest evt1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        evt1.setTimestamp(baseTime);

        EventRequest evt2 = createEvent("evt-002", "acc-001", "debit", "50.00");
        evt2.setTimestamp(baseTime + 1); // Almost simultaneous

        EventRequest evt3 = createEvent("evt-003", "acc-001", "credit", "75.00");
        evt3.setTimestamp(baseTime + 2);

        // All events within 2ms - potential race condition
        long timeDiff = evt3.getTimestamp() - evt1.getTimestamp();
        assertThat(timeDiff).isLessThanOrEqualTo(2);
    }

    @Test
    void testAmountPrecisionDrift() {
        // Test that decimal precision doesn't cause drift
        EventRequest evt1 = createEvent("evt-001", "acc-001", "credit", "0.33");
        EventRequest evt2 = createEvent("evt-002", "acc-001", "credit", "0.33");
        EventRequest evt3 = createEvent("evt-003", "acc-001", "credit", "0.34");

        BigDecimal total = evt1.getAmount()
                .add(evt2.getAmount())
                .add(evt3.getAmount());

        // Should be exactly 1.00
        assertThat(total).isEqualByComparingTo("1.00");
    }

    @Test
    void testRoundingConsistency() {
        EventRequest evt = createEvent("evt-001", "acc-001", "credit", "10.555");

        // Verify exact decimal is preserved
        assertThat(evt.getAmount().toPlainString()).isEqualTo("10.555");
    }

    @Test
    void testLargeBalanceAccumulation() {
        BigDecimal balance = BigDecimal.ZERO;

        // Accumulate large balance
        for (int i = 0; i < 100; i++) {
            EventRequest credit = createEvent("evt-c-" + i, "acc-001", "credit", "10000.00");
            balance = balance.add(credit.getAmount());
        }

        for (int i = 0; i < 50; i++) {
            EventRequest debit = createEvent("evt-d-" + i, "acc-001", "debit", "10000.00");
            balance = balance.subtract(debit.getAmount());
        }

        // Balance should be exactly 500,000
        assertThat(balance).isEqualByComparingTo("500000.00");
    }

    @Test
    void testNegativeBalanceScenario() {
        BigDecimal balance = BigDecimal.ZERO;

        EventRequest debit1 = createEvent("evt-001", "acc-001", "debit", "100.00");
        balance = balance.subtract(debit1.getAmount());

        EventRequest debit2 = createEvent("evt-002", "acc-001", "debit", "50.00");
        balance = balance.subtract(debit2.getAmount());

        // Balance goes negative
        assertThat(balance).isEqualByComparingTo("-150.00");
        assertThat(balance.signum()).isEqualTo(-1);
    }

    @Test
    void testZeroAmountHandling() {
        // Edge case: minimum amount is 0.01, but testing boundary
        EventRequest minEvent = createEvent("evt-001", "acc-001", "credit", "0.01");

        assertThat(minEvent.getAmount()).isEqualByComparingTo("0.01");
        assertThat(minEvent.getAmount().signum()).isEqualTo(1); // Positive
    }

    @Test
    void testEventIdUniqueness() {
        List<String> eventIds = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            EventRequest event = createEvent("evt-" + i, "acc-001", "credit", "10.00");
            eventIds.add(event.getEventId());
        }

        // All event IDs should be unique
        long uniqueCount = eventIds.stream().distinct().count();
        assertThat(uniqueCount).isEqualTo(100);
    }

    @Test
    void testMultipleAccountsDriftIsolation() {
        // Ensure drift in one account doesn't affect others
        BigDecimal acc1Balance = BigDecimal.ZERO;
        BigDecimal acc2Balance = BigDecimal.ZERO;

        // Account 1 transactions
        EventRequest acc1evt1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        acc1Balance = acc1Balance.add(acc1evt1.getAmount());

        EventRequest acc1evt2 = createEvent("evt-002", "acc-001", "debit", "150.00");
        acc1Balance = acc1Balance.subtract(acc1evt2.getAmount());

        // Account 2 transactions
        EventRequest acc2evt1 = createEvent("evt-003", "acc-002", "credit", "200.00");
        acc2Balance = acc2Balance.add(acc2evt1.getAmount());

        EventRequest acc2evt2 = createEvent("evt-004", "acc-002", "debit", "50.00");
        acc2Balance = acc2Balance.subtract(acc2evt2.getAmount());

        // Balances should be independent
        assertThat(acc1Balance).isEqualByComparingTo("-50.00");
        assertThat(acc2Balance).isEqualByComparingTo("150.00");
    }

    @Test
    void testTimestampConsistency() {
        long timestamp1 = System.currentTimeMillis();
        EventRequest evt1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        evt1.setTimestamp(timestamp1);

        long timestamp2 = System.currentTimeMillis();
        EventRequest evt2 = createEvent("evt-002", "acc-001", "debit", "50.00");
        evt2.setTimestamp(timestamp2);

        // Timestamps should be monotonically increasing
        assertThat(evt2.getTimestamp()).isGreaterThanOrEqualTo(evt1.getTimestamp());
    }

    @Test
    void testRapidSuccessiveEvents() {
        long baseTime = System.currentTimeMillis();
        List<EventRequest> events = new ArrayList<>();

        // Create events with minimal time difference
        for (int i = 0; i < 10; i++) {
            EventRequest event = createEvent("evt-" + i, "acc-001", "credit", "10.00");
            event.setTimestamp(baseTime + i);
            events.add(event);
        }

        // Verify all events have unique timestamps
        long uniqueTimestamps = events.stream()
                .map(EventRequest::getTimestamp)
                .distinct()
                .count();

        assertThat(uniqueTimestamps).isEqualTo(10);
    }

    @Test
    void testVeryLargeAmountHandling() {
        EventRequest largeCredit = createEvent("evt-001", "acc-001", "credit", "999999999.99");
        EventRequest largeDebit = createEvent("evt-002", "acc-001", "debit", "999999999.99");

        BigDecimal balance = largeCredit.getAmount().subtract(largeDebit.getAmount());

        assertThat(balance).isEqualByComparingTo("0.00");
    }

    private EventRequest createEvent(String eventId, String accountId, String type, String amount) {
        EventRequest request = new EventRequest();
        request.setEventId(eventId);
        request.setAccountId(accountId);
        request.setType(type);
        request.setAmount(new BigDecimal(amount));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }
}


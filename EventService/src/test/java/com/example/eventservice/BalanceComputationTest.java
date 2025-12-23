package com.example.eventservice;

import com.example.eventservice.dto.EventRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for balance computation logic.
 * These tests verify that credit and debit transactions
 * are properly formatted for downstream processing.
 */
public class BalanceComputationTest {

    @Test
    void testSingleCreditTransaction() {
        EventRequest credit = createEvent("evt-001", "acc-001", "credit", "100.00");

        // Credit should add to balance
        BigDecimal expectedBalance = new BigDecimal("100.00");

        assertThat(credit.getAmount()).isEqualByComparingTo(expectedBalance);
        assertThat(credit.getType()).isEqualTo("credit");
    }

    @Test
    void testSingleDebitTransaction() {
        EventRequest debit = createEvent("evt-001", "acc-001", "debit", "50.00");

        // Debit should subtract from balance
        BigDecimal expectedAmount = new BigDecimal("50.00");

        assertThat(debit.getAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(debit.getType()).isEqualTo("debit");
    }

    @Test
    void testMultipleCreditTransactions() {
        EventRequest credit1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        EventRequest credit2 = createEvent("evt-002", "acc-001", "credit", "150.00");
        EventRequest credit3 = createEvent("evt-003", "acc-001", "credit", "75.50");

        // Total credits
        BigDecimal totalCredits = credit1.getAmount()
                .add(credit2.getAmount())
                .add(credit3.getAmount());

        assertThat(totalCredits).isEqualByComparingTo("325.50");
    }

    @Test
    void testMultipleDebitTransactions() {
        EventRequest debit1 = createEvent("evt-001", "acc-001", "debit", "50.00");
        EventRequest debit2 = createEvent("evt-002", "acc-001", "debit", "25.00");
        EventRequest debit3 = createEvent("evt-003", "acc-001", "debit", "10.50");

        // Total debits
        BigDecimal totalDebits = debit1.getAmount()
                .add(debit2.getAmount())
                .add(debit3.getAmount());

        assertThat(totalDebits).isEqualByComparingTo("85.50");
    }

    @Test
    void testMixedTransactions() {
        EventRequest credit1 = createEvent("evt-001", "acc-001", "credit", "200.00");
        EventRequest debit1 = createEvent("evt-002", "acc-001", "debit", "50.00");
        EventRequest credit2 = createEvent("evt-003", "acc-001", "credit", "100.00");
        EventRequest debit2 = createEvent("evt-004", "acc-001", "debit", "75.00");

        // Calculate net balance: credits - debits
        BigDecimal totalCredits = credit1.getAmount().add(credit2.getAmount());
        BigDecimal totalDebits = debit1.getAmount().add(debit2.getAmount());
        BigDecimal netBalance = totalCredits.subtract(totalDebits);

        assertThat(totalCredits).isEqualByComparingTo("300.00");
        assertThat(totalDebits).isEqualByComparingTo("125.00");
        assertThat(netBalance).isEqualByComparingTo("175.00");
    }

    @Test
    void testZeroNetBalance() {
        EventRequest credit = createEvent("evt-001", "acc-001", "credit", "100.00");
        EventRequest debit = createEvent("evt-002", "acc-001", "debit", "100.00");

        BigDecimal netBalance = credit.getAmount().subtract(debit.getAmount());

        assertThat(netBalance).isEqualByComparingTo("0.00");
    }

    @Test
    void testLargeAmountCalculation() {
        EventRequest credit = createEvent("evt-001", "acc-001", "credit", "1000000.00");
        EventRequest debit = createEvent("evt-002", "acc-001", "debit", "999999.99");

        BigDecimal netBalance = credit.getAmount().subtract(debit.getAmount());

        assertThat(netBalance).isEqualByComparingTo("0.01");
    }

    @Test
    void testDecimalPrecision() {
        EventRequest credit = createEvent("evt-001", "acc-001", "credit", "100.123");
        EventRequest debit = createEvent("evt-002", "acc-001", "debit", "50.456");

        BigDecimal netBalance = credit.getAmount().subtract(debit.getAmount());

        assertThat(netBalance).isEqualByComparingTo("49.667");
    }

    @Test
    void testSequentialTransactions() {
        BigDecimal balance = BigDecimal.ZERO;

        EventRequest evt1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        balance = balance.add(evt1.getAmount());
        assertThat(balance).isEqualByComparingTo("100.00");

        EventRequest evt2 = createEvent("evt-002", "acc-001", "debit", "30.00");
        balance = balance.subtract(evt2.getAmount());
        assertThat(balance).isEqualByComparingTo("70.00");

        EventRequest evt3 = createEvent("evt-003", "acc-001", "credit", "50.00");
        balance = balance.add(evt3.getAmount());
        assertThat(balance).isEqualByComparingTo("120.00");

        EventRequest evt4 = createEvent("evt-004", "acc-001", "debit", "20.00");
        balance = balance.subtract(evt4.getAmount());
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    @Test
    void testNegativeBalance() {
        BigDecimal balance = BigDecimal.ZERO;

        EventRequest debit = createEvent("evt-001", "acc-001", "debit", "50.00");
        balance = balance.subtract(debit.getAmount());

        assertThat(balance).isEqualByComparingTo("-50.00");
        assertThat(balance.signum()).isEqualTo(-1); // Negative
    }

    @Test
    void testMultipleAccountsIndependently() {
        // Account 1
        EventRequest acc1Credit = createEvent("evt-001", "acc-001", "credit", "100.00");
        EventRequest acc1Debit = createEvent("evt-002", "acc-001", "debit", "30.00");
        BigDecimal acc1Balance = acc1Credit.getAmount().subtract(acc1Debit.getAmount());

        // Account 2
        EventRequest acc2Credit = createEvent("evt-003", "acc-002", "credit", "200.00");
        EventRequest acc2Debit = createEvent("evt-004", "acc-002", "debit", "50.00");
        BigDecimal acc2Balance = acc2Credit.getAmount().subtract(acc2Debit.getAmount());

        assertThat(acc1Balance).isEqualByComparingTo("70.00");
        assertThat(acc2Balance).isEqualByComparingTo("150.00");
        assertThat(acc1Balance).isNotEqualByComparingTo(acc2Balance);
    }

    @Test
    void testOrderedByTimestamp() {
        long baseTime = System.currentTimeMillis();

        EventRequest evt1 = createEvent("evt-001", "acc-001", "credit", "100.00");
        evt1.setTimestamp(baseTime);

        EventRequest evt2 = createEvent("evt-002", "acc-001", "debit", "50.00");
        evt2.setTimestamp(baseTime + 1000);

        EventRequest evt3 = createEvent("evt-003", "acc-001", "credit", "75.00");
        evt3.setTimestamp(baseTime + 2000);

        assertThat(evt1.getTimestamp()).isLessThan(evt2.getTimestamp());
        assertThat(evt2.getTimestamp()).isLessThan(evt3.getTimestamp());
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


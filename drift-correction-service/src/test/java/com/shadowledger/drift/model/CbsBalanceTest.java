package com.shadowledger.drift.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class CbsBalanceTest {

    @Test
    void testGettersAndSetters() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(new BigDecimal("1000.00"));

        assertThat(balance.getAccountId()).isEqualTo("acc-001");
        assertThat(balance.getReportedBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void testPositiveBalance() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(new BigDecimal("5000.00"));

        assertThat(balance.getReportedBalance()).isEqualByComparingTo("5000.00");
        assertThat(balance.getReportedBalance().signum()).isEqualTo(1);
    }

    @Test
    void testNegativeBalance() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-002");
        balance.setReportedBalance(new BigDecimal("-500.00"));

        assertThat(balance.getReportedBalance()).isEqualByComparingTo("-500.00");
        assertThat(balance.getReportedBalance().signum()).isEqualTo(-1);
    }

    @Test
    void testZeroBalance() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-003");
        balance.setReportedBalance(BigDecimal.ZERO);

        assertThat(balance.getReportedBalance()).isEqualByComparingTo("0");
        assertThat(balance.getReportedBalance().signum()).isEqualTo(0);
    }

    @Test
    void testDecimalPrecision() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-004");
        balance.setReportedBalance(new BigDecimal("123.456"));

        assertThat(balance.getReportedBalance().toPlainString()).isEqualTo("123.456");
    }

    @Test
    void testLargeBalance() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-005");
        balance.setReportedBalance(new BigDecimal("999999999.99"));

        assertThat(balance.getReportedBalance()).isEqualByComparingTo("999999999.99");
    }

    @Test
    void testSmallBalance() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-006");
        balance.setReportedBalance(new BigDecimal("0.01"));

        assertThat(balance.getReportedBalance()).isEqualByComparingTo("0.01");
    }

    @Test
    void testMultipleBalancesIndependence() {
        CbsBalance balance1 = new CbsBalance();
        balance1.setAccountId("acc-001");
        balance1.setReportedBalance(new BigDecimal("1000.00"));

        CbsBalance balance2 = new CbsBalance();
        balance2.setAccountId("acc-002");
        balance2.setReportedBalance(new BigDecimal("2000.00"));

        assertThat(balance1.getAccountId()).isNotEqualTo(balance2.getAccountId());
        assertThat(balance1.getReportedBalance()).isNotEqualByComparingTo(balance2.getReportedBalance());
    }

    @Test
    void testAccountIdWithSpecialCharacters() {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001-ABC-xyz");
        balance.setReportedBalance(new BigDecimal("1000.00"));

        assertThat(balance.getAccountId()).isEqualTo("acc-001-ABC-xyz");
    }
}


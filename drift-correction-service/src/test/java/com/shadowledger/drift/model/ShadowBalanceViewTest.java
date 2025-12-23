package com.shadowledger.drift.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ShadowBalanceViewTest {

    @Test
    void testConstructorAndGetters() {
        ShadowBalanceView view = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));

        assertThat(view.getAccountId()).isEqualTo("acc-001");
        assertThat(view.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void testPositiveBalance() {
        ShadowBalanceView view = new ShadowBalanceView("acc-001", new BigDecimal("5000.00"));

        assertThat(view.getBalance()).isEqualByComparingTo("5000.00");
        assertThat(view.getBalance().signum()).isEqualTo(1);
    }

    @Test
    void testNegativeBalance() {
        ShadowBalanceView view = new ShadowBalanceView("acc-002", new BigDecimal("-500.00"));

        assertThat(view.getBalance()).isEqualByComparingTo("-500.00");
        assertThat(view.getBalance().signum()).isEqualTo(-1);
    }

    @Test
    void testZeroBalance() {
        ShadowBalanceView view = new ShadowBalanceView("acc-003", BigDecimal.ZERO);

        assertThat(view.getBalance()).isEqualByComparingTo("0");
        assertThat(view.getBalance().signum()).isEqualTo(0);
    }

    @Test
    void testDecimalPrecision() {
        ShadowBalanceView view = new ShadowBalanceView("acc-004", new BigDecimal("123.456"));

        assertThat(view.getBalance().toPlainString()).isEqualTo("123.456");
    }

    @Test
    void testLargeBalance() {
        ShadowBalanceView view = new ShadowBalanceView("acc-005", new BigDecimal("999999999.99"));

        assertThat(view.getBalance()).isEqualByComparingTo("999999999.99");
    }

    @Test
    void testSmallBalance() {
        ShadowBalanceView view = new ShadowBalanceView("acc-006", new BigDecimal("0.01"));

        assertThat(view.getBalance()).isEqualByComparingTo("0.01");
    }

    @Test
    void testMultipleViewsIndependence() {
        ShadowBalanceView view1 = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        ShadowBalanceView view2 = new ShadowBalanceView("acc-002", new BigDecimal("2000.00"));

        assertThat(view1.getAccountId()).isNotEqualTo(view2.getAccountId());
        assertThat(view1.getBalance()).isNotEqualByComparingTo(view2.getBalance());
    }

    @Test
    void testBalanceComparison() {
        ShadowBalanceView view1 = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        ShadowBalanceView view2 = new ShadowBalanceView("acc-002", new BigDecimal("1000.00"));
        ShadowBalanceView view3 = new ShadowBalanceView("acc-003", new BigDecimal("2000.00"));

        assertThat(view1.getBalance()).isEqualByComparingTo(view2.getBalance());
        assertThat(view1.getBalance()).isNotEqualByComparingTo(view3.getBalance());
    }

    @Test
    void testAccountIdWithSpecialCharacters() {
        ShadowBalanceView view = new ShadowBalanceView("acc-001-ABC-xyz", new BigDecimal("1000.00"));

        assertThat(view.getAccountId()).isEqualTo("acc-001-ABC-xyz");
    }

    @Test
    void testBalanceDifferenceCalculation() {
        ShadowBalanceView shadow = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        BigDecimal cbsBalance = new BigDecimal("1500.00");

        BigDecimal difference = cbsBalance.subtract(shadow.getBalance());

        assertThat(difference).isEqualByComparingTo("500.00");
    }

    @Test
    void testBalanceMatchCheck() {
        ShadowBalanceView shadow = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        BigDecimal cbsBalance = new BigDecimal("1000.00");

        boolean matches = shadow.getBalance().compareTo(cbsBalance) == 0;

        assertThat(matches).isTrue();
    }
}


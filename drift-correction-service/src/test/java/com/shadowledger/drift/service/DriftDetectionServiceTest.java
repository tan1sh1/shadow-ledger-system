package com.shadowledger.drift.service;

import com.shadowledger.drift.model.CbsBalance;
import com.shadowledger.drift.model.CorrectionEvent;
import com.shadowledger.drift.model.ShadowBalanceView;
import com.shadowledger.drift.repository.ShadowLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DriftDetectionServiceTest {

    @Mock
    private ShadowLedgerRepository repository;

    @Mock
    private CorrectionPublisher publisher;

    @InjectMocks
    private DriftDetectionService service;

    @Test
    void testNoDriftWhenBalancesMatch() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1000.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        verify(publisher, never()).publish(any(CorrectionEvent.class));
    }

    @Test
    void testCreditCorrectionWhenCbsHigher() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1500.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAccountId()).isEqualTo("acc-001");
        assertThat(event.getType()).isEqualTo("credit");
        assertThat(event.getAmount()).isEqualByComparingTo("500.00");
        assertThat(event.getEventId()).startsWith("CORR-");
    }

    @Test
    void testDebitCorrectionWhenShadowHigher() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("800.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAccountId()).isEqualTo("acc-001");
        assertThat(event.getType()).isEqualTo("debit");
        assertThat(event.getAmount()).isEqualByComparingTo("200.00");
        assertThat(event.getEventId()).startsWith("CORR-");
    }

    @Test
    void testNoActionWhenShadowBalanceNotFound() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-999");
        cbsBalance.setReportedBalance(new BigDecimal("1000.00"));

        when(repository.findBalance("acc-999")).thenReturn(Optional.empty());

        service.checkAndCorrect(cbsBalance);

        verify(publisher, never()).publish(any(CorrectionEvent.class));
    }

    @Test
    void testSmallDriftCorrection() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1000.01"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAmount()).isEqualByComparingTo("0.01");
        assertThat(event.getType()).isEqualTo("credit");
    }

    @Test
    void testLargeDriftCorrection() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("10000.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAmount()).isEqualByComparingTo("9000.00");
        assertThat(event.getType()).isEqualTo("credit");
    }

    @Test
    void testNegativeBalanceDrift() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("-500.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("-300.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAmount()).isEqualByComparingTo("200.00");
        assertThat(event.getType()).isEqualTo("debit");
    }

    @Test
    void testZeroCbsBalanceWithPositiveShadow() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(BigDecimal.ZERO);

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(event.getType()).isEqualTo("debit");
    }

    @Test
    void testPositiveCbsBalanceWithZeroShadow() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1000.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", BigDecimal.ZERO);
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(event.getType()).isEqualTo("credit");
    }

    @Test
    void testDecimalPrecisionDrift() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1000.123"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.456"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getAmount()).isEqualByComparingTo("0.333");
        assertThat(event.getType()).isEqualTo("debit");
    }

    @Test
    void testMultipleAccountsDrift() {
        // Account 1
        CbsBalance cbs1 = new CbsBalance();
        cbs1.setAccountId("acc-001");
        cbs1.setReportedBalance(new BigDecimal("1500.00"));

        ShadowBalanceView shadow1 = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadow1));

        // Account 2
        CbsBalance cbs2 = new CbsBalance();
        cbs2.setAccountId("acc-002");
        cbs2.setReportedBalance(new BigDecimal("800.00"));

        ShadowBalanceView shadow2 = new ShadowBalanceView("acc-002", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-002")).thenReturn(Optional.of(shadow2));

        service.checkAndCorrect(cbs1);
        service.checkAndCorrect(cbs2);

        verify(publisher, times(2)).publish(any(CorrectionEvent.class));
    }

    @Test
    void testEventIdUniqueness() {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1500.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        service.checkAndCorrect(cbsBalance);

        ArgumentCaptor<CorrectionEvent> captor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(publisher, times(1)).publish(captor.capture());

        CorrectionEvent event = captor.getValue();
        assertThat(event.getEventId()).contains("CORR-");
        assertThat(event.getEventId()).hasSize(41); // "CORR-" + UUID
    }
}


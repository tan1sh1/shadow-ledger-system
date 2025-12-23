package com.shadowledger.drift;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowledger.drift.model.CbsBalance;
import com.shadowledger.drift.model.ShadowBalanceView;
import com.shadowledger.drift.repository.ShadowLedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Drift Correction Service.
 * Tests the full flow from HTTP request through drift detection to Kafka publishing.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DriftCorrectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShadowLedgerRepository repository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void testEndToEndDriftDetectionAndCorrection() throws Exception {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1500.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        List<CbsBalance> balances = List.of(cbsBalance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }

    @Test
    void testNoDriftNoCorrection() throws Exception {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1000.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        List<CbsBalance> balances = List.of(cbsBalance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void testManualCorrectionEndToEnd() throws Exception {
        mockMvc.perform(post("/correct/acc-001")
                .param("amount", "500.00"))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }

    @Test
    void testMultipleAccountsDriftCorrection() throws Exception {
        CbsBalance cbs1 = new CbsBalance();
        cbs1.setAccountId("acc-001");
        cbs1.setReportedBalance(new BigDecimal("1500.00"));

        CbsBalance cbs2 = new CbsBalance();
        cbs2.setAccountId("acc-002");
        cbs2.setReportedBalance(new BigDecimal("800.00"));

        ShadowBalanceView shadow1 = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        ShadowBalanceView shadow2 = new ShadowBalanceView("acc-002", new BigDecimal("1000.00"));

        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadow1));
        when(repository.findBalance("acc-002")).thenReturn(Optional.of(shadow2));

        List<CbsBalance> balances = Arrays.asList(cbs1, cbs2);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(2)).send(eq("transactions.corrections"), anyString(), any());
    }

    @Test
    void testMixedDriftScenarios() throws Exception {
        CbsBalance cbs1 = new CbsBalance();
        cbs1.setAccountId("acc-001");
        cbs1.setReportedBalance(new BigDecimal("1500.00")); // Drift: +500

        CbsBalance cbs2 = new CbsBalance();
        cbs2.setAccountId("acc-002");
        cbs2.setReportedBalance(new BigDecimal("1000.00")); // No drift

        CbsBalance cbs3 = new CbsBalance();
        cbs3.setAccountId("acc-003");
        cbs3.setReportedBalance(new BigDecimal("800.00")); // Drift: -200

        ShadowBalanceView shadow1 = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        ShadowBalanceView shadow2 = new ShadowBalanceView("acc-002", new BigDecimal("1000.00"));
        ShadowBalanceView shadow3 = new ShadowBalanceView("acc-003", new BigDecimal("1000.00"));

        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadow1));
        when(repository.findBalance("acc-002")).thenReturn(Optional.of(shadow2));
        when(repository.findBalance("acc-003")).thenReturn(Optional.of(shadow3));

        List<CbsBalance> balances = Arrays.asList(cbs1, cbs2, cbs3);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        // Only acc-001 and acc-003 should trigger corrections
        verify(kafkaTemplate, times(2)).send(eq("transactions.corrections"), anyString(), any());
        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-003"), any());
        verify(kafkaTemplate, never()).send(eq("transactions.corrections"), eq("acc-002"), any());
    }

    @Test
    void testAccountNotFoundInShadowLedger() throws Exception {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-999");
        cbsBalance.setReportedBalance(new BigDecimal("1000.00"));

        when(repository.findBalance("acc-999")).thenReturn(Optional.empty());

        List<CbsBalance> balances = List.of(cbsBalance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void testLargeDriftCorrection() throws Exception {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("10000.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        List<CbsBalance> balances = List.of(cbsBalance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }

    @Test
    void testSmallDriftCorrection() throws Exception {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("1000.01"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("1000.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        List<CbsBalance> balances = List.of(cbsBalance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }

    @Test
    void testNegativeBalanceDriftCorrection() throws Exception {
        CbsBalance cbsBalance = new CbsBalance();
        cbsBalance.setAccountId("acc-001");
        cbsBalance.setReportedBalance(new BigDecimal("-500.00"));

        ShadowBalanceView shadowBalance = new ShadowBalanceView("acc-001", new BigDecimal("-300.00"));
        when(repository.findBalance("acc-001")).thenReturn(Optional.of(shadowBalance));

        List<CbsBalance> balances = List.of(cbsBalance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }

    @Test
    void testHighVolumeBalanceChecks() throws Exception {
        List<CbsBalance> balances = new java.util.ArrayList<>();

        for (int i = 0; i < 50; i++) {
            CbsBalance balance = new CbsBalance();
            balance.setAccountId("acc-" + i);
            balance.setReportedBalance(new BigDecimal("1500.00"));
            balances.add(balance);

            ShadowBalanceView shadow = new ShadowBalanceView("acc-" + i, new BigDecimal("1000.00"));
            when(repository.findBalance("acc-" + i)).thenReturn(Optional.of(shadow));
        }

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(50)).send(eq("transactions.corrections"), anyString(), any());
    }

    @Test
    void testManualCorrectionWithLargeAmount() throws Exception {
        mockMvc.perform(post("/correct/acc-001")
                .param("amount", "999999999.99"))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }

    @Test
    void testManualCorrectionWithSmallAmount() throws Exception {
        mockMvc.perform(post("/correct/acc-001")
                .param("amount", "0.01"))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(eq("transactions.corrections"), eq("acc-001"), any());
    }
}


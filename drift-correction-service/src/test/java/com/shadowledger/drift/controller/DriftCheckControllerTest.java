package com.shadowledger.drift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowledger.drift.model.CbsBalance;
import com.shadowledger.drift.service.DriftDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriftCheckController.class)
public class DriftCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DriftDetectionService driftDetectionService;

    @Test
    void testCheckSingleBalance() throws Exception {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(new BigDecimal("1000.00"));

        List<CbsBalance> balances = List.of(balance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(1)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckMultipleBalances() throws Exception {
        CbsBalance balance1 = new CbsBalance();
        balance1.setAccountId("acc-001");
        balance1.setReportedBalance(new BigDecimal("1000.00"));

        CbsBalance balance2 = new CbsBalance();
        balance2.setAccountId("acc-002");
        balance2.setReportedBalance(new BigDecimal("2000.00"));

        CbsBalance balance3 = new CbsBalance();
        balance3.setAccountId("acc-003");
        balance3.setReportedBalance(new BigDecimal("500.00"));

        List<CbsBalance> balances = Arrays.asList(balance1, balance2, balance3);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(3)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckEmptyBalanceList() throws Exception {
        List<CbsBalance> balances = List.of();

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, never()).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckWithZeroBalance() throws Exception {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(BigDecimal.ZERO);

        List<CbsBalance> balances = List.of(balance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(1)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckWithNegativeBalance() throws Exception {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(new BigDecimal("-500.00"));

        List<CbsBalance> balances = List.of(balance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(1)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckWithLargeBalance() throws Exception {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(new BigDecimal("999999999.99"));

        List<CbsBalance> balances = List.of(balance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(1)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckWithDecimalPrecision() throws Exception {
        CbsBalance balance = new CbsBalance();
        balance.setAccountId("acc-001");
        balance.setReportedBalance(new BigDecimal("123.456"));

        List<CbsBalance> balances = List.of(balance);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(1)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckHighVolumeBalances() throws Exception {
        List<CbsBalance> balances = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CbsBalance balance = new CbsBalance();
            balance.setAccountId("acc-" + i);
            balance.setReportedBalance(new BigDecimal("1000.00"));
            balances.add(balance);
        }

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(100)).checkAndCorrect(any(CbsBalance.class));
    }

    @Test
    void testCheckDuplicateAccountIds() throws Exception {
        CbsBalance balance1 = new CbsBalance();
        balance1.setAccountId("acc-001");
        balance1.setReportedBalance(new BigDecimal("1000.00"));

        CbsBalance balance2 = new CbsBalance();
        balance2.setAccountId("acc-001");
        balance2.setReportedBalance(new BigDecimal("1500.00"));

        List<CbsBalance> balances = Arrays.asList(balance1, balance2);

        mockMvc.perform(post("/drift-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(balances)))
                .andExpect(status().isOk());

        verify(driftDetectionService, times(2)).checkAndCorrect(any(CbsBalance.class));
    }
}


package com.banking.shadowledger;

import com.banking.shadowledger.repository.LedgerRepository;
import com.banking.shadowledger.service.LedgerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class DriftDetectionTest {
    @Test
    void testDriftDetection() {
        LedgerRepository repo = Mockito.mock(LedgerRepository.class);
        LedgerService service = new LedgerService(repo);
        Mockito.when(repo.calculateShadowBalance("A3")).thenReturn(new BigDecimal("300"));
        BigDecimal cbsBalance = new BigDecimal("250");
        BigDecimal drift = service.getShadowBalance("A3").getBalance().subtract(cbsBalance);
        Assertions.assertEquals(new BigDecimal("50"), drift);
    }
}


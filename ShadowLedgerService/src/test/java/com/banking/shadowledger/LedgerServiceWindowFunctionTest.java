package com.banking.shadowledger;

import com.banking.shadowledger.dto.TransactionEventDto;
import com.banking.shadowledger.entity.LedgerEntry;
import com.banking.shadowledger.repository.LedgerRepository;
import com.banking.shadowledger.service.LedgerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class LedgerServiceWindowFunctionTest {
    @Test
    void testBalanceComputedCorrectly() {
        LedgerRepository repo = Mockito.mock(LedgerRepository.class);
        LedgerService service = new LedgerService(repo);
        Mockito.when(repo.calculateShadowBalance("A2")).thenReturn(new BigDecimal("200"));
        Assertions.assertEquals(new BigDecimal("200"), service.getShadowBalance("A2").getBalance());
    }
}


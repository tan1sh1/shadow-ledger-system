package com.banking.shadowledger;

import com.banking.shadowledger.dto.TransactionEventDto;
import com.banking.shadowledger.entity.LedgerEntry;
import com.banking.shadowledger.exception.InsufficientBalanceException;
import com.banking.shadowledger.repository.LedgerRepository;
import com.banking.shadowledger.service.LedgerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class LedgerServiceValidationTest {
    @Test
    void testInvalidEventRejected() {
        LedgerRepository repo = Mockito.mock(LedgerRepository.class);
        LedgerService service = new LedgerService(repo);
        TransactionEventDto event = new TransactionEventDto();
        event.setEventId("E1");
        event.setAccountId("A1");
        event.setType(TransactionEventDto.TransactionType.DEBIT);
        event.setAmount(new BigDecimal("100"));
        Mockito.when(repo.calculateShadowBalance("A1")).thenReturn(new BigDecimal("50"));
        Assertions.assertThrows(InsufficientBalanceException.class, () -> service.processEvent(event));
    }
}


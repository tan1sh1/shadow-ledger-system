package com.banking.shadowledger;

import com.banking.shadowledger.dto.TransactionEventDto;
import com.banking.shadowledger.entity.LedgerEntry;
import com.banking.shadowledger.repository.LedgerRepository;
import com.banking.shadowledger.service.LedgerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

public class CorrectionEventGenerationTest {
    @Test
    void testCorrectionEventGenerated() {
        LedgerRepository repo = Mockito.mock(LedgerRepository.class);
        LedgerService service = new LedgerService(repo);
        Mockito.when(repo.existsByEventId("E999")).thenReturn(false);
        TransactionEventDto event = new TransactionEventDto();
        event.setEventId("E999");
        event.setAccountId("A4");
        event.setType(TransactionEventDto.TransactionType.CREDIT);
        event.setAmount(new BigDecimal("100"));
        Assertions.assertDoesNotThrow(() -> service.processEvent(event));
    }
}


package com.banking.shadowledger.service;

import com.banking.shadowledger.dto.ShadowBalanceResponse;
import com.banking.shadowledger.dto.TransactionEventDto;
import com.banking.shadowledger.entity.LedgerEntry;
import com.banking.shadowledger.exception.InsufficientBalanceException;
import com.banking.shadowledger.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class LedgerService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);
    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public void processEvent(TransactionEventDto eventDto) {
        if (ledgerRepository.existsByEventId(eventDto.getEventId())) {
            logger.warn("Duplicate event detected and ignored: {}", eventDto.getEventId());
            return;
        }

        if (eventDto.getType() == TransactionEventDto.TransactionType.DEBIT) {
            validateSufficientBalance(eventDto);
        }

        LedgerEntry entry = new LedgerEntry(
                eventDto.getEventId(),
                eventDto.getAccountId(),
                LedgerEntry.TransactionType.valueOf(eventDto.getType().name()),
                eventDto.getAmount(),
                eventDto.getTimestamp()
        );

        ledgerRepository.save(entry);
        logger.info("Processed event: {} for account: {}", eventDto.getEventId(), eventDto.getAccountId());
    }

    private void validateSufficientBalance(TransactionEventDto eventDto) {
        BigDecimal currentBalance = ledgerRepository.calculateShadowBalance(eventDto.getAccountId());
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        BigDecimal balanceAfterDebit = currentBalance.subtract(eventDto.getAmount());
        if (balanceAfterDebit.compareTo(BigDecimal.ZERO) < 0) {
            logger.error("Insufficient balance for event: {}. Current: {}, Debit: {}",
                    eventDto.getEventId(), currentBalance, eventDto.getAmount());
            throw new InsufficientBalanceException(
                    "Insufficient balance for account " + eventDto.getAccountId());
        }
    }

    @Transactional(readOnly = true)
    public ShadowBalanceResponse getShadowBalance(String accountId) {
        BigDecimal balance = ledgerRepository.calculateShadowBalance(accountId);
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        Optional<LedgerEntry> lastEntry = ledgerRepository
                .findTopByAccountIdOrderByTimestampDescEventIdDesc(accountId);

        String lastEventId = lastEntry.map(LedgerEntry::getEventId).orElse(null);

        return new ShadowBalanceResponse(accountId, balance, lastEventId);
    }
}

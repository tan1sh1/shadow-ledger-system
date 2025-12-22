package com.banking.shadowledger.repository;

import com.banking.shadowledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    boolean existsByEventId(String eventId);

    Optional<LedgerEntry> findTopByAccountIdOrderByTimestampDescEventIdDesc(String accountId);

    @Query(value = """
        SELECT COALESCE(SUM(CASE
            WHEN type = 'CREDIT' THEN amount
            WHEN type = 'DEBIT' THEN -amount
            ELSE 0
        END), 0) as balance
        FROM ledger_entries
        WHERE account_id = :accountId
        """, nativeQuery = true)
    BigDecimal calculateShadowBalance(@Param("accountId") String accountId);

    @Query(value = """
        WITH ordered_transactions AS (
            SELECT
                event_id,
                account_id,
                type,
                amount,
                timestamp,
                CASE
                    WHEN type = 'CREDIT' THEN amount
                    WHEN type = 'DEBIT' THEN -amount
                    ELSE 0
                END as transaction_amount
            FROM ledger_entries
            WHERE account_id = :accountId
            ORDER BY timestamp, event_id
        ),
        running_balance AS (
            SELECT
                event_id,
                timestamp,
                SUM(transaction_amount) OVER (
                    ORDER BY timestamp, event_id
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                ) as running_sum
            FROM ordered_transactions
        )
        SELECT COALESCE(MIN(running_sum), 0) as min_balance
        FROM running_balance
        """, nativeQuery = true)
    BigDecimal findMinimumBalance(@Param("accountId") String accountId);
}

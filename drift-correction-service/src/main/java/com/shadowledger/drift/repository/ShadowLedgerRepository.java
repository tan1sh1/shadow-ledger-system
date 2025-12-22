package com.shadowledger.drift.repository;

import com.shadowledger.drift.model.ShadowBalanceView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class ShadowLedgerRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShadowLedgerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ShadowBalanceView> findBalance(String accountId) {
        return jdbcTemplate.query(
                """
                SELECT account_id,
                       SUM(
                         CASE
                           WHEN type = 'CREDIT' THEN amount
                           ELSE -amount
                         END
                       ) AS balance
                FROM ledger_entries
                WHERE account_id = ?
                GROUP BY account_id
                """,
                rs -> rs.next()
                        ? Optional.of(new ShadowBalanceView(
                        rs.getString("account_id"),
                        rs.getBigDecimal("balance")))
                        : Optional.empty(),
                accountId
        );
    }
}

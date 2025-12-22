package com.banking.shadowledger.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionEventDto {

    private String eventId;
    private String accountId;
    private TransactionType type;
    private BigDecimal amount;
    private Instant timestamp;

    public enum TransactionType {
        DEBIT, CREDIT;

        @JsonCreator
        public static TransactionType fromString(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Transaction type cannot be null");
            }
            return TransactionType.valueOf(value.toUpperCase());
        }

        @JsonValue
        public String toValue() {
            return this.name();
        }
    }

    public TransactionEventDto() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getTimestamp() {
        return timestamp != null ? timestamp : Instant.now();
    }
    public void setTimestamp(Instant timestamp) {
        this.timestamp = (timestamp != null ? timestamp : Instant.now());
    }
}

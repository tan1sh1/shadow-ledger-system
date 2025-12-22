package com.example.eventservice.dto;

import com.example.eventservice.entity.EventEntity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class EventRequest {

    @NotBlank
    private String eventId;

    @NotBlank
    private String accountId;

    @Pattern(regexp = "credit|debit")
    private String type;

    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private Long timestamp;

    public EventEntity toEntity() {
        EventEntity e = new EventEntity();
        e.setEventId(eventId);
        e.setAccountId(accountId);
        e.setType(type);
        e.setAmount(amount);
        e.setTimestamp(timestamp);
        return e;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

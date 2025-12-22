package com.banking.shadowledger.dto;

import java.math.BigDecimal;

public class ShadowBalanceResponse {

    private String accountId;
    private BigDecimal balance;
    private String lastEvent;

    public ShadowBalanceResponse() {}

    public ShadowBalanceResponse(String accountId, BigDecimal balance, String lastEvent) {
        this.accountId = accountId;
        this.balance = balance;
        this.lastEvent = lastEvent;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getLastEvent() { return lastEvent; }
    public void setLastEvent(String lastEvent) { this.lastEvent = lastEvent; }
}

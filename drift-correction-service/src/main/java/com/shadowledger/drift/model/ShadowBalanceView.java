package com.shadowledger.drift.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ShadowBalanceView {
    private String accountId;
    private BigDecimal balance;
}


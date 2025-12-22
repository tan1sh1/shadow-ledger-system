package com.shadowledger.drift.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CbsBalance {
    private String accountId;
    private BigDecimal reportedBalance;
}

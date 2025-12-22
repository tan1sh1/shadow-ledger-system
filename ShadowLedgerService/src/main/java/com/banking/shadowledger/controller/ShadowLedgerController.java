package com.banking.shadowledger.controller;

import com.banking.shadowledger.dto.ShadowBalanceResponse;
import com.banking.shadowledger.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class ShadowLedgerController {

    private static final Logger logger = LoggerFactory.getLogger(ShadowLedgerController.class);
    private final LedgerService ledgerService;

    public ShadowLedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/{accountId}/shadow-balance")
    public ResponseEntity<ShadowBalanceResponse> getShadowBalance(
            @PathVariable String accountId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        if (traceId != null) {
            MDC.put("X-Trace-Id", traceId);
        }

        logger.info("Fetching shadow balance for account: {}", accountId);
        ShadowBalanceResponse response = ledgerService.getShadowBalance(accountId);
        logger.info("Shadow balance retrieved: {} for account: {}", response.getBalance(), accountId);

        MDC.clear();
        return ResponseEntity.ok(response);
    }
}

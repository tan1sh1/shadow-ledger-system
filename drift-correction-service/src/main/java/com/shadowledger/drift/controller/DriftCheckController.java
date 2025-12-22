package com.shadowledger.drift.controller;

import com.shadowledger.drift.model.CbsBalance;
import com.shadowledger.drift.service.DriftDetectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drift-check")
public class DriftCheckController {

    private final DriftDetectionService service;

    public DriftCheckController(DriftDetectionService service) {
        this.service = service;
    }

    @PostMapping
    public void check(@RequestBody List<CbsBalance> balances) {
        balances.forEach(service::checkAndCorrect);
    }
}

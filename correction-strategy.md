# Correction Strategy

This document outlines the drift correction strategy for the Shadow Ledger System.

## 1. Drift Detection
- Periodically, the system compares the computed balance with the reported balance for each account.
- If the difference exceeds a configurable threshold, a drift is detected.

## 2. Correction Event Generation
- When drift is detected, a correction event is generated for the affected account and window.
- The correction event contains the accountId, window, detected drift amount, and a reference to the original events.

## 3. Correction Application
- Correction events are applied after all regular events in the window.
- The correction amount is added/subtracted to bring the computed balance in line with the reported balance.

## 4. Audit and Traceability
- All correction events are logged and auditable.
- Each correction event includes metadata for traceability (who, when, why).

## 5. Manual Corrections
- Manual corrections can be triggered via API for exceptional cases.
- Manual corrections are subject to audit and require appropriate authorization.

---

**Note:** The correction strategy ensures ledger accuracy and supports both automated and manual drift correction.

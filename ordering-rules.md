# Ordering Rules

This document describes the event and transaction ordering rules for the Shadow Ledger System.

## 1. Event Ordering
- Events are ordered by their timestamp and eventId.
- If two events have the same timestamp, eventId lexicographical order is used as a tiebreaker.
- All services must process events in this deterministic order to ensure consistency.

## 2. Transaction Ordering
- Transactions within an account are processed in the order they are received.
- Correction events are always applied after all regular events for a given window.

## 3. Windowing
- For windowed computations, events are grouped by a fixed time window (e.g., 1 hour).
- Within a window, ordering rules above apply.

## 4. Correction Event Ordering
- Correction events reference the window and account they correct.
- Correction events must be processed after all events in the window they correct.

## 5. Distributed Ordering
- When events are distributed across services, a global ordering is maintained using a combination of timestamp, eventId, and serviceId.

---

**Note:** Strict ordering is critical for ledger consistency and drift correction accuracy.

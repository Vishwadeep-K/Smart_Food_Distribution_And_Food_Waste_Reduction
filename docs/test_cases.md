# Automated Test Suite & Test Scenarios

This document details all 15 test scenarios specified for the **Annam Smart Food Distribution and Food Waste Reduction** project. All unit and service test cases are automated and pass cleanly under JUnit 5 (`.\mvnw.cmd test` or `test.bat`).

---

## Summary of Test Results

| Test ID | Test Scenario | Category | Expected Result | Status |
|---|---|---|---|---|
| **TEST 1** | Normal Successful Allocation | Feasibility & Matching | All conditions met; match accepted; allocated 40 portions | **PASS** |
| **TEST 2** | Expired Batch Rejection | Safety & Integrity | Batch rejected; message indicates batch has reached use-by time | **PASS** |
| **TEST 3** | Delivery Missing Deadline | Time Feasibility | Rejected: Pickup + Travel + Handover exceeds declared use-by time | **PASS** |
| **TEST 4** | Partial Recipient Demand | Quantity Allocation | Allocation capped at recipient demand (20); remaining 30 portions retained | **PASS** |
| **TEST 5** | Vehicle Capacity Limitation | Logistics Constraint | Allocation capped strictly at vehicle capacity (15) | **PASS** |
| **TEST 6** | Duplicate Reservation Attempt | Concurrency & Balances | Second reservation attempt exceeding available balance rejected | **PASS** |
| **TEST 7** | Incompatible Food Category | Polymorphism | Rejected via `acceptsFoodCategory()` polymorphic check | **PASS** |
| **TEST 8** | Unapproved Batch Rejection | Operator Governance | Unapproved food cannot be distributed | **PASS** |
| **TEST 9** | PriorityQueue Ordering | Data Structure (Heap) | Approved batches ordered strictly by Earliest Use-By Time First | **PASS** |
| **TEST 10** | Equal Expiry Tie-Breaking | Determinism | Equal use-by times broken deterministically by Batch ID | **PASS** |
| **TEST 11** | Database Transaction Rollback | ACID Transactions | Failure during reservation rolls back all SQL updates | **PASS** |
| **TEST 12** | Concurrent Reservation Attempts | Data Integrity | Conditional atomic SQL prevents duplicate portion reservations | **PASS** |
| **TEST 13** | Duplicate Delivery Confirmation | State Encapsulation | Already delivered allocation cannot be delivered a second time | **PASS** |
| **TEST 14** | Persistence After Restart | Database Storage | MySQL retains donors, batches, and allocations across server restarts | **PASS** |
| **TEST 15** | Unreachable Road Location | Graph Traversal | Dijkstra identifies disconnected component and rejects match | **PASS** |

---

## Detailed Test Case Specifications

### TEST 1: Normal Successful Allocation
- **Input**: Batch B01 (MEALS, 40 portions, Use-by 12:30 @ Central Hub), Recipient R01 (Shelter, Demand 60 @ East Ward), Vehicle V01 (Capacity 50), Current Time 10:00, Handover 10m.
- **Dijkstra Calculation**: Route `LOC01 -> LOC10 -> LOC06`, Travel Time = 18 mins.
- **Deadline Calculation**: Expected Completion = 10:00 + 18m + 10m = 10:28.
- **Feasibility**: 10:28 < 12:30.
- **Expected Result**: Match ACCEPTED. Allocated Portions = min(40, 60, 50) = 40 portions.

### TEST 2: Expired Batch Rejection
- **Input**: Batch B02 (Use-by 10:15). Simulated Time advanced to 10:30.
- **Rule**: `CurrentTime >= UseByTime`.
- **Expected Result**: Match REJECTED with message `"Food batch B02 is EXPIRED. (Current time: 10:30 >= Use-By: 10:15)"`.

### TEST 3: Delivery Missing Deadline
- **Input**: Batch B03 (Use-by 10:40). Pickup Time 10:15. Travel Time = 18 mins. Handover = 10 mins.
- **Deadline Calculation**: Expected Completion = 10:15 + 18m + 10m = 10:43.
- **Feasibility**: 10:43 >= 10:40 (Violates condition: Expected Completion < Use-By).
- **Expected Result**: Match REJECTED with message `"Delivery would finish after use-by deadline."`.

### TEST 4: Partial Recipient Demand
- **Input**: Food available = 50 portions. Recipient demand = 20 portions. Vehicle capacity = 50 portions.
- **Formula**: `allocated = min(50, 20, 50) = 20`.
- **Expected Result**: Exactly 20 portions allocated. Remaining 30 portions safely retained in batch balance and remain available for subsequent matches.

### TEST 5: Vehicle Capacity Limitation
- **Input**: Food available = 40 portions. Recipient demand = 40 portions. Vehicle capacity = 15 portions.
- **Formula**: `allocated = min(40, 40, 15) = 15`.
- **Expected Result**: Allocation strictly capped at 15 portions. Batch retains 25 portions.

### TEST 6: Duplicate Reservation Attempt
- **Input**: Batch has 30 available portions.
  - Request 1: Reserve 20 portions &rarr; **SUCCESS**. Available balance drops to 10.
  - Request 2: Reserve 20 portions from same batch &rarr; **REJECTED**.
- **Expected Result**: Throws `InsufficientFoodException` / `DuplicateReservationException`. Available balance never drops below zero.

### TEST 7: Incompatible Food Category (Runtime Polymorphism)
- **Input**: Batch B07 (BIRYANI). Recipient R03 (CommunityCentre: accepts FRUITS, VEGETABLES, PACKED_FOOD, BREAD, MEALS).
- **Polymorphism**: `communityCentre.acceptsFoodCategory(FoodCategory.BIRYANI)` evaluates to `false`.
- **Expected Result**: Match REJECTED with message `"Category incompatible: Hope Community Centre (Community Centre) does not accept Biryani & Specialized Hot Rice."`.

### TEST 8: Unapproved Food Batch
- **Input**: Batch B08 with `approvalStatus = NOT_APPROVED`.
- **Rule**: Only operator-approved food can be distributed.
- **Expected Result**: Match REJECTED with message `"Food batch B08 is NOT APPROVED by admin. Unapproved food cannot be distributed."`.

### TEST 9: PriorityQueue Ordering (Earliest Use-By First)
- **Input**: Approved batches with use-by times:
  - B01: 14:00
  - B02: 11:30
  - B03: 13:15
- **Expected Result**: `PriorityQueue.poll()` returns in strict chronological order: `B02 (11:30) -> B03 (13:15) -> B01 (14:00)`.

### TEST 10: Equal Expiry-Time Deterministic Tie-Breaking
- **Input**: Two approved batches with identical use-by time (13:00):
  - Batch "BZ" (Use-by 13:00)
  - Batch "BA" (Use-by 13:00)
- **Comparator**: `int cmp = this.useBy.compareTo(other.useBy); if (cmp != 0) return cmp; return this.batchId.compareTo(other.batchId);`
- **Expected Result**: Tie is resolved deterministically by Batch ID: `BA` polled before `BZ`.

### TEST 11: Database Transaction Rollback
- **Scenario**: Allocation service reserves batch, but an unexpected downstream failure occurs before committing.
- **Mechanism**: Spring `@Transactional` rolls back MySQL table updates.
- **Expected Result**: Batches, recipient demand, and vehicle tables remain unchanged.

### TEST 12: Concurrent Reservation Prevention
- **Scenario**: Two requests try to reserve the same 30-portion batch concurrently.
- **Mechanism**: Conditional SQL update:
  ```sql
  UPDATE food_batches 
  SET available_portions = available_portions - 30, reserved_portions = reserved_portions + 30 
  WHERE batch_id = 'B01' AND available_portions >= 30 AND approval_status = 'APPROVED' AND is_expired = FALSE;
  ```
- **Expected Result**: First request updates 1 row; second request updates 0 rows and is rejected.

### TEST 13: Duplicate Delivery Confirmation
- **Scenario**: Confirming delivery for an allocation that is already in `DELIVERED` status.
- **Expected Result**: Throws `InvalidAllocationException("Allocation ALC0001 is already marked DELIVERED")`.

### TEST 14: Persistence After Application Restart
- **Scenario**: Create donor, batch, and allocation. Stop the Spring Boot application and restart it.
- **Expected Result**: All records, remaining portions, and audit logs are re-loaded from MySQL tables without data loss.

### TEST 15: Unreachable Road Location
- **Scenario**: Recipient located at disconnected island `LOC99`.
- **Expected Result**: Dijkstra algorithm returns `isReachable() == false`. Match is rejected with `"No reachable road path between pickup and destination"`.

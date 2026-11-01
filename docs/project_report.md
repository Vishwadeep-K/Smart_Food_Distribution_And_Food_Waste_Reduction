# Academic Mini-Project Report

## 1. Title
**Smart Food Distribution and Food Waste Reduction**  
*A Java OOP, Data Structures, Algorithms, and Full-Stack Web Implementation for Time-Sensitive Surplus Redistribution.*

---

## 2. Abstract
Surplus food from institutional catering and hospitality venues is frequently discarded due to fragmented manual coordination, communication delays, and tight consumption deadlines. This project implements **Annam**, a full-stack, time-sensitive food distribution platform engineered with **Java 21, Spring Boot, MySQL (XAMPP), and a responsive Bootstrap 5 frontend**. 

The system leverages fundamental computer science principles:
- **Java PriorityQueue** (Earliest-Use-By-First scheduling with deterministic tie-breaking)
- **HashMaps** (O(1) record lookup for donors, batches, recipients, and vehicles)
- **Custom Adjacency-List Weighted Road Network Graph**
- **Dijkstra’s Shortest Path Algorithm** with a Min-Heap for positive travel-time computation
- **ACID Database Transactions** with conditional concurrency locking to prevent over-allocation and duplicate reservations.

The application achieves measurable impact aligned with UN Sustainable Development Goals (SDG 2, 12, 13) by ensuring that food allocations finish strictly prior to declared use-by deadlines.

---

## 3. Case Study
**Annam** is a fictional volunteer network coordinating perishable food donations from five major institutional donors (hotels, universities, commercial bakeries, corporate cafeterias) to four beneficiary organizations (children's shelters, senior homes, community welfare centres, and food pantries). 

In traditional volunteer operations, communication occurs through uncoordinated telephone exchanges, causing:
1. Food with earlier use-by deadlines remaining uncollected while fresher food is dispatched.
2. Beneficiaries receiving excess portions exceeding their meal capacity.
3. Deliveries arriving after meals have turned cold or expired.
4. Duplicate promises of the same surplus donation to multiple recipients.

---

## 4. Problem Statement
To design and build an automated, transaction-safe food distribution system that systematically resolves allocation imbalances, enforces safety deadlines through graph shortest-path algorithms, guarantees that perishable food is consumed before its declared use-by time, and prevents double reservation through transactional concurrency control.

---

## 5. Objectives
1. **Prioritize Surplus Batches**: Implement min-heap priority scheduling ordered by earliest declared use-by time.
2. **Polymorphic Recipient Acceptance**: Dynamically enforce accepted food categories across distinct organization types (`Shelter` vs `CommunityCentre`).
3. **Safe Portion Allocation**: Allocate portions based on `min(Available, Demand, Capacity)`, retaining unallocated quantities.
4. **Feasibility Verification**: Compute shortest travel times using Dijkstra's algorithm and verify:
   $$\text{Expected Completion} = \text{Pickup Time} + \text{Travel Time} + \text{Handover Allowance} < \text{Use-By Time}$$
5. **Concurrency & Atomicity**: Prevent race conditions and duplicate reservations using database transactions and conditional SQL.
6. **Simulated Clock**: Enable virtual time advancement without altering the host OS clock.

---

## 6. Existing Manual System
| Characteristic | Manual Phone Coordination | Drawback |
|---|---|---|
| **Batch Priority** | Random / First-called | Earlier expiring food spoils while newer food is taken |
| **Routing** | Driver guesswork | Traffic delays cause late arrivals past deadlines |
| **Quantity Balance** | Verbal estimates | Shelters get over-allocated; surplus wasted |
| **Double Booking** | High human error | Multiple volunteers promise the same food batch |
| **Audit Trail** | Paper registers / None | Lack of traceability and accountability |

---

## 7. Proposed System
The proposed system automates the operational workflow:
- **Centralized REST Backend**: Java Spring Boot processes matching logic, Dijkstra path calculations, and database transactions.
- **PriorityQueue Engine**: Ranks all approved food batches by earliest deadline.
- **Multi-Factor Matching**: Validates approval status, non-expiry, polymorphic category acceptance, demand, vehicle availability, and Dijkstra travel feasibility.
- **Transactional Reservation**: Executes atomic reservations in MySQL, locking portion balances and recording audit logs.
- **Persistent Data Store**: MySQL through XAMPP ensures full recovery and history preservation across application restarts.

---

## 8. System Assumptions
1. Edge weights in the road network graph represent positive travel times in minutes under average urban transit conditions.
2. The system relies on operator-declared use-by times and suitability labels; it does not perform chemical food safety certification.
3. Fixed safety handover allowance is configured to 10 minutes (configurable via admin settings).
4. Vehicles travel directly from their location to the donor and onward to the recipient.

---

## 9. Functional Requirements
1. **Donor Management**: Add, view, edit, search, and delete donors (with foreign-key integrity guards).
2. **Food Batch Ingestion**: Register surplus food batches with portion count, category, donor, pickup location, and use-by time.
3. **Admin Approval Gate**: Allow administrators to approve or reject batches; only approved batches enter the PriorityQueue.
4. **Recipient Management**: Manage shelters and community centres, configure accepted categories, and adjust remaining demand.
5. **Vehicle Fleet Management**: Track vehicle location, portion capacity, and busy/available states.
6. **Smart Matching Preview**: Display ranked batch candidates with Dijkstra route, travel time, completion time, and eligibility.
7. **Atomic Reservation**: Conditionally decrement available portions, increment reserved portions, and decrement recipient demand.
8. **Delivery Confirmation**: Transition allocations from `RESERVED` to `DELIVERED`, repositioning the vehicle at the destination.
9. **Simulated Clock**: Set virtual time, advance minutes, and trigger automated expiry sweeps.
10. **Reports & Audit Trail**: Display unallocated remainders, expired batches, allocation history, and DSA visualizations.

---

## 10. Non-Functional Requirements
- **Performance**: Dijkstra route computation and matching evaluation execute in sub-100ms.
- **Data Integrity**: Enforce foreign keys, check constraints (`portions >= 0`), and ACID transaction rollbacks.
- **Extensibility**: Clean separation into `model`, `policy`, `graph`, `service`, `repository`, and `controller` packages.
- **Usability**: Responsive dashboard using Bootstrap 5 with accessible color-coded badges.

---

## 11. System Workflow
```
[Donor Donates Batch] 
         │
         ▼
[Operator Approves Batch] ──► [Inserted into PriorityQueue (Earliest Use-By First)]
         │
         ▼
[Select Recipient & Vehicle]
         │
         ▼
[TimeSensitiveFoodMatchingPolicy]
    ├── 1. Check Approval & Expiry
    ├── 2. Check Category (Polymorphic Recipient Check)
    ├── 3. Calculate Quantity = min(Avail, Demand, Cap)
    └── 4. Run Dijkstra Algorithm on Road Network Graph
             │
             ▼
        Completion Time = Pickup + Travel + Handover (10m)
             │
             ├── Is Completion < Use-By?
             │     ├── NO  ──► REJECT (Display Reason)
             │     └── YES ──► ACCEPT MATCH
                                    │
                                    ▼
                             [Atomic Reservation in MySQL]
                                    │
                                    ▼
                             [Delivery Confirmed]
```

---

## 12. UML Class Diagram
Refer to [`docs/uml.md`](uml.md) for the complete Mermaid diagram illustrating classes, associations, inheritance, and dependency relationships.

---

## 13. Object-Oriented Programming (OOP) Concepts
1. **Encapsulation**: `FoodBatch` hides internal balances. State changes occur strictly through guarded methods: `reservePortions()`, `releaseReservation()`, `confirmDelivered()`, `markExpired()`.
2. **Inheritance**: `Shelter` and `CommunityCentre` extend the abstract base class `Recipient`, sharing identity, location, and demand state.
3. **Runtime Polymorphism**: `Recipient` defines the abstract method `acceptsFoodCategory(FoodCategory)`. Concrete subclasses override this to implement domain-specific acceptance rules without `instanceof` checks.
4. **Abstraction**: `FoodMatchingPolicy` specifies the matching contract. `TimeSensitiveFoodMatchingPolicy` implements the 12 concrete business and deadline rules.
5. **Composition**: `FoodAllocation` encapsulates `FoodBatch`, `Recipient`, `DeliveryVehicle`, and `PathResult`.

---

## 14. Data Structures Used
1. **Java PriorityQueue**: Min-heap ordering approved food batches by earliest declared use-by time. Ties resolved deterministically using Batch ID.
2. **Java HashMap**: $O(1)$ constant-time lookups for entities (`Map<String, FoodBatch>`, `Map<String, Recipient>`, `Map<String, DeliveryVehicle>`).
3. **Custom Weighted Graph (Adjacency List)**: Non-library implementation using `Map<String, List<Edge>>` to model vertices and travel times.
4. **Min-Heap in Dijkstra**: Internal priority queue storing `(Location, Distance)` tuples for greedy shortest-path extraction.
5. **Java ArrayList**: Maintains allocation history, route waypoints, and unallocated batch remainder records.

---

## 15. Algorithms Used
- **Dijkstra’s Shortest Path Algorithm**:
  Greedy algorithm using edge relaxation with a min-heap. Time complexity: $O((V + E) \log V)$.
  Computes the minimum transit time between pickup and recipient drop-off points.
- **Min-Allocation Formula**:
  $$\text{Allocated Portions} = \min(\text{Available Batch Portions}, \text{Recipient Demand}, \text{Vehicle Capacity})$$
- **Deadline Feasibility Rule**:
  $$\text{Completion Time} = \text{Pickup Time} + \text{Shortest Travel Time} + \text{Handover (10 min)} < \text{Declared Use-By Time}$$

---

## 16. Data Structure Justifications
| Data Structure | Practical Purpose | Alternative Considered | Why Chosen Structure is Superior |
|---|---|---|---|
| **PriorityQueue** | Earliest-use-by-first batch ordering | Unsorted ArrayList | Provides $O(\log N)$ insertion and $O(1)$ minimum extraction, preventing earliest expiring food from being overlooked. |
| **HashMap** | Key-based entity retrieval | Array / Linear Search | $O(1)$ average time retrieval by unique ID versus $O(N)$ linear scans. |
| **Adjacency List** | Road network graph | Adjacency Matrix | Sparse road networks ($E \ll V^2$) consume $O(V + E)$ memory rather than $O(V^2)$, optimizing neighbor iteration. |
| **Dijkstra Min-Heap** | Shortest path travel time | Bellman-Ford / BFS | All road travel times are strictly positive; Dijkstra with a min-heap runs in $O((V+E)\log V)$ versus $O(VE)$ for Bellman-Ford. |
| **ArrayList** | Audit logs & remainder history | LinkedList | Contiguous memory allocation gives superior cache locality and $O(1)$ random access for indexing. |

---

## 17. Allocation Rules
1. Only `APPROVED` batches are eligible for matching.
2. Expired batches (`Current Time >= Use-By Time`) are locked.
3. Quantity is strictly bounded by available portions, remaining recipient demand, and vehicle capacity.
4. If portions remain after an allocation, the unallocated remainder is retained and remains allocatable.
5. Duplicate reservations on the same portions are rejected.
6. A vehicle cannot be assigned to another trip while marked `BUSY`.

---

## 18. Deadline Feasibility Logic
```java
LocalTime pickupTime = currentTime;
int travelTime = pathResult.getTotalTravelTimeMinutes();
LocalTime expectedCompletion = pickupTime.plusMinutes((long) travelTime + handoverMins);

if (!expectedCompletion.isBefore(batch.getDeclaredUseByTime())) {
    return MatchResult.reject("Delivery would finish after use-by deadline.");
}
```

---

## 19. Sample Input Data Summary
- **Donors (5)**: Grand Palace Hotel (LOC01), Anna Univ Canteen (LOC02), Green Leaf Bakery (LOC03), Spice Garden Rest (LOC04), Tech Park Cafeteria (LOC05).
- **Recipients (4)**: Karuna Children Home (Shelter, Demand 60), Mother Teresa Senior Home (Shelter, Demand 40), Hope Community Centre (Centre, Demand 50), Peace Welfare Pantry (Centre, Demand 35).
- **Vehicles (3)**: Annam Van-01 (Cap 50), Annam Van-02 (Cap 30), Annam E-Loader-03 (Cap 20).
- **Road Network (12 Locations, 16 Roads)**: Positive travel times ranging from 8 to 25 minutes.
- **Food Batches (22)**: Diverse categories (Meals, Biryani, Rice, Chapati, Bread, Fruits, Vegetables, Packed Food) with varied use-by times.

---

## 20. Test Cases & Results
All 18 automated JUnit 5 tests passed with 100% success.
Refer to [`docs/test_cases.md`](test_cases.md) for exhaustive test steps and assertion logs.

---

## 21. Screenshots to Capture
When demonstrating the application, capture:
1. **System Dashboard**: Showing live metrics cards, portion stats, and Chart.js category doughnut chart.
2. **Smart Food Matching**: Displaying ranked candidate batches with Dijkstra routes and ACCEPTED/REJECTED badges.
3. **Reservation Confirmation**: Pop-up modal showing successful atomic portion reservation.
4. **Active Allocations**: Table displaying reserved allocations and the "Confirm Delivery" action.
5. **Delivery Routing**: Dijkstra calculator showing computed route `Central Hub -> Junction Alpha -> East Ward (18 mins)`.
6. **Food Batches Management**: Showing PriorityQueue ordering toggle and approval buttons.
7. **Simulated Clock**: Setting virtual time and running automated expiry checks.
8. **Data Structures Showcase**: Displaying the PriorityQueue heap order and graph adjacency list.

---

## 22. Limitations
1. Does not integrate real-time GPS tracking or live congestion telemetry.
2. Single-drop delivery model: Multi-recipient vehicle routing (Traveling Salesperson / VRP) is not simulated.
3. Food safety relies on donor and operator declaration rather than IoT sensor measurements.

---

## 23. Future Enhancements
- Integration with OpenStreetMap / GPS trackers for live driver location updates.
- Vehicle Routing Problem (VRP) optimization for multi-stop consolidated pickups.
- IoT temperature sensor logging for cold-chain compliance monitoring.
- Automated SMS/Push notifications to shelter coordinators.

---

## 24. SDG Connections
- **SDG 2: Zero Hunger**: Directly redirects usable, wholesome surplus food to vulnerable shelter residents.
- **SDG 12: Responsible Consumption & Production**: Mitigates commercial and institutional food waste through algorithmic matching.
- **SDG 13: Climate Action**: Prevents food rotting in landfills, curbing harmful greenhouse methane emissions.

---

## 25. Conclusion
The **Annam Smart Food Distribution and Food Waste Reduction** project successfully proves how foundational Object-Oriented Programming, Java Collections, and Data Structures & Algorithms can solve real-world logistical challenges. By replacing ad-hoc phone coordination with deterministic PriorityQueue scheduling, Dijkstra graph routing, and ACID transactional database management, Annam ensures that edible surplus reaches people in need safely, equitably, and on time.

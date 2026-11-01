# UML Class Diagram & System Architecture

## 1. Mermaid Class Diagram

```mermaid
classDiagram
    direction TB

    %% Models
    class FoodBatch {
        -String batchId
        -FoodCategory category
        -int totalPortions
        -int availablePortions
        -int reservedPortions
        -int deliveredPortions
        -LocalTime declaredUseByTime
        -Donor donor
        -Location pickupLocation
        -ApprovalStatus approvalStatus
        -boolean expired
        +reservePortions(int portions)
        +releaseReservation(int portions)
        +confirmDelivered(int portions)
        +markExpired()
        +compareTo(FoodBatch other) int
        +isApproved() boolean
        +isExpired() boolean
    }

    class Donor {
        -String donorId
        -String name
        -String organization
        -String phone
        -Location location
        +getDonorId() String
        +getLocation() Location
    }

    class Recipient {
        <<abstract>>
        -String recipientId
        -String name
        -Location location
        -int totalRequiredPortions
        -int remainingDemandPortions
        #Set~FoodCategory~ acceptedCategories
        +acceptsFoodCategory(FoodCategory category)* boolean
        +getRecipientType()* String
        +fulfillDemand(int portions)
        +releaseDemand(int portions)
    }

    class Shelter {
        +acceptsFoodCategory(FoodCategory category) boolean
        +getRecipientType() String
    }

    class CommunityCentre {
        +acceptsFoodCategory(FoodCategory category) boolean
        +getRecipientType() String
    }

    class DeliveryVehicle {
        -String vehicleId
        -String name
        -Location currentLocation
        -int portionCapacity
        -boolean available
        +setAvailable(boolean available)
        +setCurrentLocation(Location loc)
    }

    class FoodAllocation {
        -String allocationId
        -FoodBatch foodBatch
        -Recipient recipient
        -DeliveryVehicle vehicle
        -int allocatedPortions
        -Location pickupLocation
        -Location destinationLocation
        -PathResult routeResult
        -int travelTimeMinutes
        -LocalTime pickupTime
        -int handoverMinutes
        -LocalTime expectedCompletionTime
        -AllocationStatus status
        +confirmDelivery()
        +cancelAllocation()
    }

    %% Graph & Routing
    class Location {
        -String locationId
        -String name
        -String zoneType
        -String description
    }

    class Edge {
        -Location destination
        -int travelTimeMinutes
    }

    class RoadNetwork {
        -Map~String, Location~ locations
        -Map~String, List~Edge~~ adjacencyList
        +addLocation(Location loc)
        +addBidirectionalRoad(String a, String b, int mins)
        +getNeighbors(Location loc) List~Edge~
    }

    class DijkstraAlgorithm {
        <<utility>>
        +findShortestPath(RoadNetwork net, Location src, Location dst)$ PathResult
    }

    class PathResult {
        -Location source
        -Location destination
        -List~Location~ path
        -int totalTravelTimeMinutes
        -boolean reachable
        +getFormattedRoute() String
    }

    %% Policy & Matching
    class FoodMatchingPolicy {
        <<interface>>
        +evaluateMatch(FoodBatch batch, Recipient recipient, DeliveryVehicle vehicle, RoadNetwork net, LocalTime time, int handover)* MatchResult
    }

    class TimeSensitiveFoodMatchingPolicy {
        +evaluateMatch(...) MatchResult
    }

    class MatchResult {
        -boolean accepted
        -String reason
        -int allocatedPortions
        -PathResult routeResult
        -LocalTime expectedCompletionTime
        +isAccepted() boolean
        +getReason() String
    }

    class SimulatedClock {
        -LocalTime currentTime
        -int handoverAllowanceMinutes
        +advanceMinutes(int mins)
        +setCurrentTime(LocalTime time)
    }

    %% Relationships
    Recipient <|-- Shelter : Inheritance
    Recipient <|-- CommunityCentre : Inheritance
    FoodMatchingPolicy <|.. TimeSensitiveFoodMatchingPolicy : Implements

    FoodBatch --> Donor : references
    FoodBatch --> Location : pickup location
    Donor --> Location : located at
    Recipient --> Location : located at
    DeliveryVehicle --> Location : current location
    RoadNetwork *-- Location : contains vertices
    RoadNetwork *-- Edge : contains adjacency list
    Edge --> Location : targets vertex

    FoodAllocation *-- FoodBatch : Composition
    FoodAllocation *-- Recipient : Composition
    FoodAllocation *-- DeliveryVehicle : Composition
    FoodAllocation *-- PathResult : Composition

    TimeSensitiveFoodMatchingPolicy ..> DijkstraAlgorithm : invokes
    TimeSensitiveFoodMatchingPolicy ..> MatchResult : produces
    DijkstraAlgorithm ..> RoadNetwork : traverses
    DijkstraAlgorithm ..> PathResult : produces
```

---

## 2. Explanation of Object-Oriented Relationships

### A. Inheritance & Runtime Polymorphism
- **Base Abstraction**: `Recipient` provides the abstract template defining organizational demand, location, and the abstract method `acceptsFoodCategory(FoodCategory)`.
- **Concrete Subclasses**: `Shelter` and `CommunityCentre` override `acceptsFoodCategory()`.
  - `Shelter` prioritizes ready-to-eat cooked provisions (`MEALS`, `BIRYANI`, `RICE`, `CHAPATI`, `BREAD`).
  - `CommunityCentre` prioritizes pantry produce and dry goods (`FRUITS`, `VEGETABLES`, `PACKED_FOOD`, `BREAD`, `MEALS`).
- **Polymorphic Invocation**: The `TimeSensitiveFoodMatchingPolicy` calls `recipient.acceptsFoodCategory(batch.getCategory())` dynamically without branching on `instanceof`, adhering to the Open/Closed Principle.

### B. Strict Encapsulation
- `FoodBatch` encapsulates portion states (`availablePortions`, `reservedPortions`, `deliveredPortions`). The `reservePortions()`, `releaseReservation()`, `confirmDelivered()`, and `markExpired()` methods guarantee:
  1. No over-allocation beyond available portions.
  2. No reservations against expired batches.
  3. No reservations against unapproved batches.
  4. No negative balances.
- `FoodAllocation` encapsulates state lifecycle transitions: `confirmDelivery()` cannot confirm an unreserved or already delivered allocation.

### C. Composition
- `FoodAllocation` is composed of:
  - `FoodBatch` (surplus supply)
  - `Recipient` (beneficiary demand)
  - `DeliveryVehicle` (transit capacity)
  - `PathResult` (Dijkstra-computed itinerary)
  - Temporal fields (`pickupTime`, `travelTimeMinutes`, `handoverMinutes`, `expectedCompletionTime`).

### D. Abstraction
- `FoodMatchingPolicy` decouples matching evaluation rules from scheduling coordinators. `TimeSensitiveFoodMatchingPolicy` enforces 12 discrete domain and deadline feasibility rules.

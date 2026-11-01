# Annam: Smart Food Distribution & Food Waste Reduction
*College Mini Project &mdash; Java OOP, Data Structures & Algorithms, Spring Boot, MySQL (XAMPP), and Bootstrap 5 Dashboard*

[![Java 21](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-XAMPP-blue.svg)](https://www.apachefriends.org/)
[![Build](https://img.shields.io/badge/Build-Maven%20Passed-success.svg)]()
[![Tests](https://img.shields.io/badge/JUnit%205-29%2F29%20Passed-success.svg)]()
[![Security](https://img.shields.io/badge/Security-BCrypt%20%26%20RBAC-success.svg)]()

---

## 1. Project Overview
**Annam** is a full-stack, time-sensitive food distribution platform that coordinates perishable surplus food donations from institutions and restaurants with shelters and community centres.

The application features a secure **Role-Based Access Control (RBAC)** authentication system supporting two distinct roles:
1. **Admin**: Manages the central logistics dashboard, reviews and approves/rejects surplus food donations, tracks shortest-path delivery routing, and executes atomic portion reservations and delivery confirmations.
2. **Donor**: Features a dedicated Donor Portal to view profile details, submit surplus food donations with use-by and preparation timings, and track live allocation and delivery statuses. 

The system solves the critical pitfalls of traditional manual telephone coordination:
1. **Prevents Food Expiry**: Prioritizes batches using a **Java PriorityQueue (Min-Heap)** strictly by earliest declared use-by time.
2. **Prevents Over-Allocation**: Enforces recipient demand limits and calculates portion allocation as $\min(\text{Available}, \text{Demand}, \text{Vehicle Capacity})$.
3. **Guarantees Travel Deadline Feasibility**: Computes the shortest travel-time path using **Dijkstra's Algorithm on a custom Adjacency-List Road Network** and verifies that $\text{Pickup} + \text{Travel} + \text{Handover (10m)} < \text{Use-By Deadline}$.
4. **Prevents Duplicate Reservation**: Leverages **ACID database transactions** and conditional locking to prevent race conditions.
5. **Runtime Polymorphism**: Uses Java OOP inheritance and polymorphism (`Shelter` vs `CommunityCentre`) to dynamically validate food category acceptance.

---

## 2. Technology Stack
- **Backend**: Java 21 LTS, Spring Boot 3.2.4, Spring Web REST API, Spring JDBC (`JdbcTemplate`, `@Transactional`), Maven.
- **Database & Local Server**: XAMPP, MySQL / MariaDB, phpMyAdmin, MySQL Connector/J.
- **Frontend**: HTML5, CSS3, JavaScript (Fetch API), Bootstrap 5, Chart.js, Bootstrap Icons.
- **Testing**: JUnit 5, Mockito.

---

## 3. Project Structure
```
MINIPROJECT2/
├── pom.xml                               # Maven project descriptor
├── mvnw.cmd                              # Portable Maven wrapper
├── run.bat                               # One-click Spring Boot launch script
├── test.bat                              # One-click automated JUnit test runner
├── README.md                             # Comprehensive project documentation
├── database/
│   ├── schema.sql                        # Complete MySQL database DDL
│   └── sample_data.sql                   # 5 Donors, 4 Recipients, 3 Vehicles, 12 Locations, 22 Batches
├── docs/
│   ├── project_report.md                 # Full 26-section academic mini-project report
│   ├── uml.md                            # Mermaid UML Class Diagram & OOP relationships
│   └── test_cases.md                     # Detailed specifications of all 15 test scenarios
├── src/
│   ├── main/
│   │   ├── java/com/annam/
│   │   │   ├── AnnamApplication.java    # Spring Boot entry point
│   │   │   ├── config/                  # CORS and database initialization configs
│   │   │   ├── controller/              # REST API controllers
│   │   │   ├── exception/               # Custom business exceptions
│   │   │   ├── graph/                   # Adjacency-List RoadNetwork & DijkstraAlgorithm
│   │   │   ├── model/                   # FoodBatch, Recipient, Shelter, CommunityCentre, etc.
│   │   │   ├── policy/                  # FoodMatchingPolicy & TimeSensitiveFoodMatchingPolicy
│   │   │   ├── repository/              # Spring JDBC repositories
│   │   │   ├── service/                 # Core domain business logic services
│   │   │   └── util/                    # SimulatedClock and IDGenerator
│   │   └── resources/
│   │       ├── application.properties   # MySQL credentials and application configs
│   │       ├── schema.sql               # Packaged schema script
│   │       ├── sample_data.sql          # Packaged sample data script
│   │       └── static/                  # Responsive Frontend Dashboard
│   │           ├── index.html
│   │           ├── css/styles.css
│   │           └── js/                  # Modular JavaScript files (matching, routing, etc.)
│   └── test/
│       └── java/com/annam/              # JUnit 5 test suites (18 test methods)
```

---

## 4. Database Setup via XAMPP & phpMyAdmin

### Step 1: Start MySQL in XAMPP
1. Open the **XAMPP Control Panel**.
2. Locate the **MySQL** module and click **Start**.
3. Verify that the port displays `3306` (or your configured port) and the status indicator turns green.

### Step 2: Import Database in phpMyAdmin
1. Open your browser and navigate to: [http://localhost/phpmyadmin](http://localhost/phpmyadmin).
2. Click the **Databases** tab.
3. Enter database name: `annam_food_distribution` and click **Create**.
4. Select `annam_food_distribution` from the left sidebar.
5. Click the **Import** tab on the top menu.
6. Click **Choose File** &rarr; select [`database/schema.sql`](database/schema.sql) &rarr; click **Import**.
7. Once completed, repeat the import step for [`database/sample_data.sql`](database/sample_data.sql).

*(Note: The Spring Boot application also includes an automated `DatabaseInitConfig` that will auto-seed tables and data if they are not already present).*

### Step 3: Configure Database Credentials (if customized)
By default, XAMPP uses user `root` with no password on port `3306`.
If your local installation differs, configure [`src/main/resources/application.properties`](src/main/resources/application.properties):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/annam_food_distribution?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```
Alternatively, pass environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`.

---

## 5. How to Compile and Run the Project

### Option A: Using the Convenience Batch Script
Double-click [`run.bat`](run.bat) in the project root directory.

### Option B: Using the Command Line
```powershell
# In the project root directory:
.\mvnw.cmd spring-boot:run
```

### Application URLs & Credentials

| Page | URL | Description |
|---|---|---|
| **Login Page** | `http://localhost:8080/login.html` | Unified login interface with Admin/Donor tabs |
| **Donor Registration** | `http://localhost:8080/register.html` | Self-service registration for food donor organizations |
| **Admin Dashboard** | `http://localhost:8080/index.html` | Central administration, smart matching & routing |
| **Donor Portal** | `http://localhost:8080/donor-dashboard.html` | Donor dashboard: My Profile, Submit Donation, My Donations |

#### Demo Credentials for Testing / Viva Evaluation:
- **Admin Account**:
  - Email: `admin@annam.org`
  - Password: `Admin@123`
  - Role: `ADMIN`
- **Pre-Configured Sample Donor Account**:
  - Email: `donor@annam.org`
  - Password: `Donor@123`
  - Role: `DONOR` (Linked to Donor `D01` - Grand Palace Hotel)

---

## 6. Running the Automated Test Suite

To run all 29 automated JUnit 5 tests covering authentication, BCrypt verification, role-based authorization, PriorityQueue ordering, tie-breakers, Dijkstra routing, and atomic reservations:

### Option A: Using the Test Batch Script
Double-click [`test.bat`](test.bat).

### Option B: Using the Command Line
```powershell
.\mvnw.cmd test
```

Expected output:
```
[INFO] Results:
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 7. REST API Endpoints

| Category | Method | Endpoint | Description |
|---|---|---|---|
| **Authentication** | `POST` | `/api/auth/login` | Authenticate with email, password, and role |
| | `POST` | `/api/auth/register` | Register new donor account (`DONOR` role) |
| | `GET` | `/api/auth/me` | Get current authenticated session user |
| | `POST` | `/api/auth/logout` | Invalidate authenticated session |
| **Donor Portal** | `GET` | `/api/donor/profile` | View logged-in donor profile & statistics |
| | `PUT` | `/api/donor/profile` | Update donor contact & address details |
| | `POST` | `/api/donor/donations` | Submit surplus food donation (Pending Approval) |
| | `GET` | `/api/donor/donations` | View donation history for logged-in donor only |
| **Dashboard** | `GET` | `/api/dashboard/stats` | Aggregated metrics, portions, charts data (Admin) |
| **Donors** | `GET` | `/api/donors` | List all donors with batch statistics (Admin) |
| | `POST` | `/api/donors` | Register a new food donor (Admin) |
| | `DELETE` | `/api/donors/{id}` | Delete donor (guarded against active batches) |
| **Batches** | `GET` | `/api/batches` | Filterable food batch listings |
| | `GET` | `/api/batches/queue` | PriorityQueue ordering (Earliest Use-By First) |
| | `POST` | `/api/batches` | Register surplus batch |
| | `PUT` | `/api/batches/{id}/approve` | Admin approval & PriorityQueue insertion |
| | `PUT` | `/api/batches/{id}/reject` | Admin rejection |
| **Recipients** | `GET` | `/api/recipients` | List shelters & community centres with accepted categories |
| | `PUT` | `/api/recipients/{id}/demand` | Adjust remaining portion demand |
| **Vehicles** | `GET` | `/api/vehicles` | List delivery vehicles with capacity & locations |
| | `PUT` | `/api/vehicles/{id}/availability` | Toggle vehicle availability (Available/Busy) |
| **Matching** | `GET` | `/api/matching/candidates` | Evaluate candidate matches with Dijkstra feasibility |
| | `POST` | `/api/matching/evaluate` | Evaluate specific batch-recipient-vehicle triplet |
| **Allocations** | `GET` | `/api/allocations` | View all allocations and status lifecycle |
| | `POST` | `/api/allocations/reserve` | Atomically reserve portions with transactional locking |
| | `POST` | `/api/allocations/{id}/confirm-delivery` | Confirm physical delivery & free vehicle |
| | `GET` | `/api/allocations/history` | Audit log of all reservation and delivery events |
| **Routing** | `GET` | `/api/routing/roads` | List all road network edges with positive travel times |
| | `GET` | `/api/routing/shortest-path` | Run Dijkstra algorithm between source and destination |
| **Clock** | `GET` | `/api/clock` | View simulated current time and handover allowance |
| | `POST` | `/api/clock/set` | Set virtual time (HH:mm) |
| | `POST` | `/api/clock/advance` | Advance virtual clock by N minutes |
| | `POST` | `/api/clock/run-expiry` | Scan and invalidate batches past use-by time |
| **Reports** | `GET` | `/api/reports/data-structures` | Interactive educational view of PriorityQueue, HashMap, and Graph |

---

## 8. Data Structures & Algorithms Justification

1. **Java PriorityQueue**:
   - *Purpose*: Implements the mandatory **Earliest-Use-By-First** rule for approved food batches.
   - *Justification*: Provides $O(\log N)$ logarithmic insertion and $O(1)$ constant-time retrieval of the earliest expiring batch. Ties are broken deterministically by Batch ID.
   - *Invariant Rule*: Whenever a queued batch's ranking fields change, the element is removed from the queue, updated, and reinserted.

2. **Java HashMap**:
   - *Purpose*: Fast, $O(1)$ constant-time lookup of food batches, recipients, vehicles, and donors by primary key string.

3. **Custom Weighted Graph (Adjacency List)**:
   - *Purpose*: Models 12 road locations and 16 weighted edges representing positive travel times in minutes.
   - *Justification*: $O(V + E)$ memory efficiency over an adjacency matrix.

4. **Dijkstra's Shortest Path Algorithm (with Min-Heap)**:
   - *Purpose*: Solves the Single-Source Shortest Path (SSSP) problem to compute minimum transit times.
   - *Feasibility Check*:
     $$\text{Pickup Time} + \text{Shortest Travel Time} + \text{Handover Allowance (10 min)} < \text{Declared Use-By Time}$$

---

## 9. Troubleshooting

### Q: "Can't connect to MySQL server on 'localhost' (10061)"
- **Fix**: Open XAMPP Control Panel and verify the MySQL service is started. Ensure port 3306 is not blocked by another service.

### Q: "Table 'annam_food_distribution.food_batches' doesn't exist"
- **Fix**: Import [`database/schema.sql`](database/schema.sql) and [`database/sample_data.sql`](database/sample_data.sql) in phpMyAdmin, or restart the application to let `DatabaseInitConfig` auto-populate.

### Q: Port 8080 already in use
- **Fix**: Change `server.port=8081` in [`src/main/resources/application.properties`](src/main/resources/application.properties).

---

## 10. Academic Integrity & Viva Readiness
This project has been built strictly for academic presentation and viva evaluation:
- Demonstrates **Java 21**, **Spring Boot**, **Spring JDBC**, **MySQL (XAMPP)**, and **Bootstrap 5**.
- All business logic, algorithms, and data structures are implemented **purely in Java** without third-party graph or optimization libraries.
- Verified with 18 automated JUnit 5 tests.

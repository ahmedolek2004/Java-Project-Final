# 13. Test Strategy & Test Plan

> **Document ID:** DOC-QA-013  
> **Standard:** ISO/IEC/IEEE 29119 Software Testing  
> **Testing Frameworks:** JUnit Jupiter 5.11.4  
> **Target System:** Forge Fitness Management System  
> **Status:** Active & Implemented

---

## 1. Test Strategy Overview

The testing strategy ensures high resilience, strict business rule enforcement, and total data integrity across the desktop application. Since Forge Fitness operates as an offline financial and access control system, test automation focuses intensely on:
1. **Domain & Calculation Invariants:** Session decrements, discount boundaries, duplicate check-in blocking, payroll duplication prevention.
2. **Persistence & Constraint Integrity:** Verifying SQLite foreign key enforcement, unique phone numbers, and transactional rollbacks.
3. **Security & Cryptography:** PBKDF2 hashing verification, salt randomness, and role-based access restrictions.

---

## 2. Test Levels & Scope

```
        ┌─────────────────────────┐
        │   Manual System & UAT   │  (Physical USB Scanner, Card Printing, Dark/Light RTL)
        ├─────────────────────────┤
        │    Integration Tests    │  (DAO + In-Memory SQLite Database Transactions)
        ├─────────────────────────┤
        │       Unit Tests        │  (Service Rules, Discount Math, Password Hashing)
        └─────────────────────────┘
```

### 2.1 Unit Testing
- Scope: Independent business services (`MemberService`, `SubscriptionService`, `AttendanceService`, `PasswordService`).
- Framework: JUnit Jupiter 5.
- Isolation: Services are tested using in-memory SQLite instances (`jdbc:sqlite::memory:`) or lightweight mocks.

### 2.2 Integration Testing
- Scope: Database persistence, constraint validation, transaction rollbacks (`GymRepository`, `ClubRepository`, `DatabaseConnection`).
- Key Test Cases:
  - Verifying `PRAGMA foreign_keys = ON` rejects orphan subscriptions.
  - Verifying `one_checkin_per_member_day` unique index rejects duplicate check-ins on the same day.
  - Verifying `UNIQUE(employee_type, employee_id, pay_month)` rejects duplicate salary disbursements.

### 2.3 Hardware & Manual System Testing
- Scope: USB HID scanner character capture, barcode rendering via ZXing, Arabic RTL visual layout inspections, and 1-click database backup/restore.

---

## 3. High-Priority Automated Test Suites

| Test Suite Class | Target Component | Verifies Requirements |
| :--- | :--- | :--- |
| `MemberServiceTest` | `MemberService.java` | REQ-FR-MEM-001, REQ-FR-MEM-002 (Phone uniqueness, CRUD) |
| `SubscriptionServiceTest` | `SubscriptionService.java` | REQ-FR-SUB-001, REQ-FR-SUB-002, BR-SUB-003 (Discounts, Freeze) |
| `AttendanceServiceTest` | `AttendanceService.java` | REQ-FR-ATT-001, REQ-FR-ATT-003, BR-ATT-002 (Check-in, Session decrement) |
| `PayrollServiceTest` | `PayrollService.java` | REQ-FR-FIN-003, BR-FIN-001 (Monthly salary uniqueness) |
| `PasswordServiceTest` | `PasswordService.java` | REQ-FR-SEC-001 (PBKDF2 derivation, salt generation, constant-time match) |
| `DatabaseIntegrityTest` | `DatabaseConnection.java` | REQ-NFR-REL-002, SQLite Foreign Keys, Pragmas |

---

## 4. Acceptance Criteria & Release Quality Gates

An application release candidate is deemed acceptable for production deployment if and only if:
1. **Automated Tests:** 100% of JUnit test cases pass with zero failures:
   ```bash
   mvn clean test
   [INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```
2. **Zero Schema Collisions:** The test suite successfully boots against a clean, freshly generated database without DDL syntax errors.
3. **Backup Integrity:** A test database backup is restored and confirmed identical in byte hash and record count.

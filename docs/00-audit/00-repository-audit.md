# Phase 1: Repository & Codebase Technical Audit Report

> **Target Project:** Forge Fitness & Martial Arts Management System  
> **Source Directory:** `GYM/`  
> **Audit Date:** 2026-10-25  
> **Auditor:** Lead Software Architect & Requirements Engineer  
> **Audit Status:** COMPLETED & VERIFIED

---

## 1. Executive Findings Summary

An exhaustive audit of the `GYM` repository was conducted to inspect source code, build scripts, tests, dependencies, and database initialization logic.

### Key Audit Metrics:
- **Programming Language & Version:** Java 25 (`<maven.compiler.release>25</maven.compiler.release>` in `pom.xml`).
- **UI Framework:** OpenJFX (JavaFX Controls & FXML) version `21.0.6`.
- **Database Engine:** Embedded SQLite via `sqlite-jdbc` version `3.50.3.0`.
- **Barcode/QR Engine:** Google ZXing Core version `3.5.3`.
- **Test Framework:** JUnit Jupiter version `5.11.4`.
- **Packaging/Build Tool:** Apache Maven with `javafx-maven-plugin:0.0.8`.
- **Application Structure:** 3-tier Layered Architecture with clean separation of DAOs, Services, UI Pages/Controllers, and Models.

---

## 2. Dependency & Configuration Analysis

### 2.1 File: `GYM/pom.xml`
- **Compiler Configuration:**
  - Strict release targeting Java 25.
  - Source file encoding: `UTF-8`.
- **Verified Dependencies:**
  ```xml
  org.openjfx:javafx-controls:21.0.6
  org.openjfx:javafx-fxml:21.0.6
  org.xerial:sqlite-jdbc:3.50.3.0
  com.google.zxing:core:3.5.3
  org.junit.jupiter:junit-jupiter:5.11.4 (test scope)
  ```
- **Plugins:**
  - `maven-compiler-plugin:3.13.0`
  - `maven-surefire-plugin:3.5.2`
  - `javafx-maven-plugin:0.0.8` (configured to run `com.gym.system.Launcher` / `GymApplication`)

---

## 3. Database Schema & Persistence Layer Audit

### 3.1 Connection & Lifecycle (`DatabaseConnection.java`)
- **Storage Location:** `~/.gym-system/gym_system.db` (User home directory ensures cross-platform write permissions without UAC issues on Windows).
- **Pragmas Enforced:**
  - `PRAGMA foreign_keys = ON;` (Referential integrity enforced).
  - `PRAGMA busy_timeout = 5000;` (Prevents immediate `SQLITE_BUSY` lockups on concurrent transactions).
- **Verified Tables Created in `initialize()`:**
  1. `users` (id, username, password_hash, full_name, role [ADMIN/RECEPTIONIST], phone, salary, active, created_at)
  2. `activities` (id, name_ar, name_en, description)
  3. `coaches` (id, name, phone, activity_id, salary, is_personal_trainer)
  4. `members` (id, full_name, phone [UNIQUE], email, gender, medical_notes, created_at)
  5. `subscriptions` (id, member_id, plan_name, plan_type [TIME_BASED/SESSION_BASED], duration_months, total_sessions, remaining_sessions, start_date, end_date, price, discount_applied, status [ACTIVE/EXPIRED/FROZEN], activity_id)
  6. `class_schedule` (id, title, activity_id, coach_id, day_of_week [1-7], start_time, end_time, capacity, active)
  7. `payroll_payments` (id, employee_type [USER/COACH], employee_id, employee_name, pay_month, amount, paid_at - UNIQUE constraint on employee_type, employee_id, pay_month)
  8. `attendance` (id, member_id, subscription_id, check_in_time) - Unique index: `one_checkin_per_member_day` on `(member_id, date(check_in_time))`
  9. `payments` (id, member_id, description, amount, payment_date, status [PAID/PENDING], subscription_id)
  10. `expenses` (id, description, amount, expense_date)
  11. `app_settings` (setting_key, setting_value)

---

## 4. Source Code Architecture & Domain Model Verification

### 4.1 Domain Models (`com.gym.system.model`)
| Model Class | Code Status | Verification Notes |
| :--- | :--- | :--- |
| `User.java` | Verified | Base class with PBKDF2 hash, role validation |
| `Admin.java` | Verified | Subclass of `User` with ADMIN role |
| `Receptionist.java` | Verified | Subclass of `User` with RECEPTIONIST role |
| `Member.java` | Verified | Encapsulates full member info, contact, and medical remarks |
| `Subscription.java` | Verified | Supports dual modes: `TIME_BASED` and `SESSION_BASED` |
| `Coach.java` | Verified | Encapsulates coach attributes, specialty activity, PT flag |
| `Activity.java` | Verified | Bilingual activity record (name_ar, name_en) |

### 4.2 Services (`com.gym.system.service`)
- `MemberService.java`: Manages member records, search filtering, and duplicate phone checks.
- `SubscriptionService.java`: Enforces subscription lifecycles, freeze/unfreeze, and session deductions.
- `AttendanceService.java`: Handles fast check-in via member ID or QR code payload, daily duplicate blocking, and session counting.
- `ScheduleService.java`: Manages weekly schedule slots and classroom capacity limits.
- `CoachService.java`: Handles coach staffing and assignments.
- `ActivityService.java`: Manages academy disciplines (Boxing, Karate, Fitness, etc.).
- `FinanceService.java`: Handles club operational expenses and subscription revenue audits.
- `PayrollService.java`: Monthly salary disbursement with duplicate disbursement prevention.
- `TeamService.java`: Manages admin and receptionist accounts with secure password updates.

### 4.3 UI Controllers & Component Factory (`com.gym.system.controller`)
- Modular page architecture: View components implement isolated page controllers (`DashboardPage`, `MembersPage`, `SubscriptionsPage`, `AttendancePage`, `SchedulePage`, `FinancePage`, `PayrollPage`, `TeamPage`).
- Unified context: `PageContext.java` provides standardized access to services, translations (`LanguageManager`), and alert popups.
- Dynamic theme and localization: `AppearanceHelper.java` and `LanguageManager.java` support instant switching between English LTR and Arabic RTL, plus Dark/Light theme switching.

---

## 5. Discrepancies, Gaps & Identified Risks

1. **Dual Repository Classes:**
   - Both `GymRepository.java` (28 KB) and `ClubRepository.java` (17.6 KB) exist. Some queries overlap (e.g. member lookups vs coach/activity queries).
   - *Recommendation:* Keep them cleanly separated or establish a facade as detailed in `07-architecture.md`.
2. **Offline Hardware Dependency:**
   - Physical USB barcode/QR scanners act as HID keyboards (typing characters and sending `ENTER`). Attendance UI must maintain focus on input or handle global key captures gracefully.
3. **Database Migration Mechanism:**
   - Currently uses `ensureColumn()` programmatic DDL queries in `DatabaseConnection.java`. While functional for small additions, a formal versioned migration table is recommended for multi-station production rollouts.

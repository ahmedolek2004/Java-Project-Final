# 03. Software Requirements Specification (SRS)

> **Document ID:** DOC-PROD-003  
> **Standard:** ISO/IEC/IEEE 29148:2018 Systems and software engineering — Life cycle processes — Requirements engineering  
> **Target System:** Forge Fitness & Martial Arts Management System  
> **Version:** 1.0.0

---

## 1. Introduction

### 1.1 Purpose
This document provides complete, unambiguous, and verifiable specifications for the Forge Fitness Management System. It specifies both functional requirements (FR) and non-functional requirements (NFR) governing the desktop application.

### 1.2 System Scope
The software is a self-contained, offline-first Java 25 / JavaFX 21 desktop application utilizing an embedded SQLite database for local persistence.

---

## 2. Functional Requirements (FR)

### 2.1 User Management & Security (SEC)
- **REQ-FR-SEC-001 (Authentication):** The system shall authenticate users using a unique username and password. Passwords shall be hashed using PBKDF2 with SHA-256 and salt.
- **REQ-FR-SEC-002 (RBAC Roles):** The system shall enforce role-based access control with two roles: `ADMIN` and `RECEPTIONIST`.
- **REQ-FR-SEC-003 (Role Restrictions):** `RECEPTIONIST` users shall be restricted from viewing or modifying Financial Reports, Expenses, Staff Salaries, and Team Accounts.

### 2.2 Member Management (MEM)
- **REQ-FR-MEM-001 (Member Creation):** The system shall create a new member record with full name, unique phone number, optional email, gender, and medical notes.
- **REQ-FR-MEM-002 (Phone Uniqueness):** The system shall reject any member registration with a phone number already registered in the database.
- **REQ-FR-MEM-003 (Search & Filter):** The system shall allow searching members in real-time by full name or phone number.
- **REQ-FR-MEM-004 (QR Generation & Printing):** The system shall generate a valid QR code representing the member's ID and allow printing a physical membership card.

### 2.3 Subscription Management (SUB)
- **REQ-FR-SUB-001 (Dual Plan Types):** The system shall support both `TIME_BASED` plans (defined by start date, duration in months, and end date) and `SESSION_BASED` plans (defined by total allocated sessions and remaining sessions).
- **REQ-FR-SUB-002 (Discount Calculation):** The system shall allow applying a percentage discount (0% - 100%) and calculate the final payable price.
- **REQ-FR-SUB-003 (Subscription Lifecycle):** Subscriptions shall maintain status `ACTIVE`, `FROZEN`, or `EXPIRED`.
- **REQ-FR-SUB-004 (Subscription Freeze):** An active subscription may be frozen, pausing session usage and access until unfrozen.
- **REQ-FR-SUB-005 (Payment Association):** Every new subscription shall generate an associated payment record linked to the member and subscription ID.

### 2.4 Attendance & Check-In (ATT)
- **REQ-FR-ATT-001 (QR Check-In):** The system shall accept a QR code string scanned from a USB scanner, extract the member ID, and verify entry eligibility.
- **REQ-FR-ATT-002 (Eligibility Invariants):** Entry shall be granted if and only if:
  1. The member possesses an `ACTIVE` subscription.
  2. For `TIME_BASED`: Current date is between `start_date` and `end_date`.
  3. For `SESSION_BASED`: `remaining_sessions` > 0.
- **REQ-FR-ATT-003 (Session Consumption):** For `SESSION_BASED` subscriptions, a successful check-in shall immediately decrement `remaining_sessions` by 1. When `remaining_sessions` reaches 0, status shall automatically transition to `EXPIRED`.
- **REQ-FR-ATT-004 (Daily Limit Invariant):** The system shall strictly prevent duplicate check-ins for the same member on the same calendar day, raising an informative warning dialog.

### 2.5 Scheduling & Academy Operations (SCH)
- **REQ-FR-SCH-001 (Class Scheduling):** The system shall support scheduling classes by day of week (Monday to Sunday), start time, end time, capacity, assigned activity, and coach.
- **REQ-FR-SCH-002 (Capacity Enforcement):** Class registrations shall not exceed the specified classroom capacity.

### 2.6 Finance & Payroll (FIN)
- **REQ-FR-FIN-001 (Revenue Auditing):** The system shall compute total income from subscription payments within any selected date range.
- **REQ-FR-FIN-002 (Expense Logging):** The system shall log club expenses with description, amount, and date.
- **REQ-FR-FIN-003 (Payroll Disbursement):** The system shall calculate monthly base salaries for staff (`users`) and coaches, permitting 1 disbursement per employee per calendar month (`pay_month` e.g., `2026-10`). Duplicate payments for the same month shall be rejected by unique database constraint.
- **REQ-FR-FIN-004 (Net Profit):** Net profit shall be computed as: `Net = Total Revenue - Total Expenses - Total Payroll Paid`.

### 2.7 Backup, Restore & Export (SYS)
- **REQ-FR-SYS-001 (Database Backup):** The system shall perform a hot backup copy of `gym_system.db` to a user-selected target location.
- **REQ-FR-SYS-002 (Database Restore):** The system shall allow restoring a database file after validating SQLite header integrity.
- **REQ-FR-SYS-003 (Excel Export):** The system shall export member lists, financial records, and attendance logs to CSV files with a `UTF-8 BOM` prefix to preserve Arabic characters in Microsoft Excel.

---

## 3. Non-Functional Requirements (NFR)

### 3.1 Performance & Timing
- **REQ-NFR-PERF-001 (Scan-to-Decision Latency):** From the instant a QR code string is received until the visual verification dialog renders, elapsed time shall be `< 500 ms`.
- **REQ-NFR-PERF-002 (Cold Startup Time):** Application cold start on standard desktop hardware (Intel Core i3 / 8GB RAM / SSD) shall not exceed `3.0 seconds`.

### 3.2 Usability & Internationalization
- **REQ-NFR-I18N-001 (Bilingual Support):** The application shall support full Arabic and English localization via standard Java `ResourceBundle` files.
- **REQ-NFR-I18N-002 (Native RTL Support):** When Arabic is selected, the root JavaFX scene orientation shall dynamically invert to `NodeOrientation.RIGHT_TO_LEFT` with adjusted alignments and padding.
- **REQ-NFR-UX-001 (Dark & Light Theming):** The UI shall support high-contrast Dark and Light modes with persistent user settings.

### 3.3 Reliability & Availability
- **REQ-NFR-REL-001 (Zero Cloud Dependency):** All features (authentication, check-in, billing, printing) shall operate with 100% functionality without internet connection.
- **REQ-NFR-REL-002 (Database Resilience):** All write operations affecting multiple tables (e.g. subscription creation + payment entry) shall be executed inside atomic transactions (`conn.setAutoCommit(false)` ... `conn.commit()`).

### 3.4 Compatibility & Hardware
- **REQ-NFR-COMP-001 (Operating System):** Windows 10/11, Linux (Ubuntu 20.04+), and macOS (12+).
- **REQ-NFR-COMP-002 (Hardware Scanner):** Any standard USB HID Barcode/QR reader acting as keyboard input.

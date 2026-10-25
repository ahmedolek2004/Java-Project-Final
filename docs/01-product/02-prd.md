# 02. Product Requirements Document (PRD)

> **Document ID:** DOC-PROD-002  
> **Standard:** ISO/IEC/IEEE 29148  
> **Product Name:** Forge Fitness Management System  
> **Status:** Verified & Active  
> **Target Release:** v1.0.0-PROD

---

## 1. Product Overview & Market Positioning

**Forge Fitness Management System** addresses the critical operational demands of independent fitness centers, crossfit boxes, and martial arts academies. While enterprise cloud solutions require ongoing subscriptions and continuous high-speed internet, Forge operates as a **high-performance local desktop system** guaranteeing maximum speed, high data privacy, zero recurring cloud costs, and complete offline independence.

---

## 2. Target Personas & Primary Use Cases

### 2.1 Persona: Ahmed (Club Owner & General Manager)
- **Pain Points:** Unclear net profit calculations, untracked operational expenses, unauthorized entry by acquaintances.
- **Goals:** Total financial visibility, automated payroll calculation, secure admin-only settings, and reliable database backups.
- **Key Features Used:** Dashboard KPIs, Finance & Expenses, Payroll, Team Management, Backup & Restore.

### 2.2 Persona: Sarah (Front Desk Receptionist)
- **Pain Points:** Rush-hour queues, slow typing, difficulty tracking remaining sessions for boxing classes, awkward system English terminology.
- **Goals:** Check in incoming members within 2 seconds using a QR scanner, quickly add new members, process cash renewals, and print membership cards.
- **Key Features Used:** Live Attendance Page (USB Scanner & Manual ID Input), Members Registration, Subscription Management, Print Controller.

### 2.3 Persona: Captain Tarek (Head Coach / Personal Trainer)
- **Pain Points:** Overcrowded classes exceeding room capacity, lack of visibility into student attendance, delayed salary tracking.
- **Goals:** Clear weekly schedule, defined class capacities, assigned martial arts disciplines.
- **Key Features Used:** Schedule View, Activities Management.

---

## 3. Core Feature Specifications & Priority Matrix

| Feature Code | Feature Name | Description | Priority | Implemented in Code |
| :--- | :--- | :--- | :--- | :--- |
| **FEAT-01** | Member Profile Management | Create, edit, search, filter members; enforce unique phone numbers; record medical notes | **P0 (Must)** | Yes (`MemberService`, `MembersPage`) |
| **FEAT-02** | Dual-Model Subscriptions | Support `TIME_BASED` (monthly/yearly) and `SESSION_BASED` (e.g. 12 martial arts classes) | **P0 (Must)** | Yes (`SubscriptionService`, `SubscriptionsPage`) |
| **FEAT-03** | Automated Subscription Lifecycle | State transitions: `ACTIVE` -> `EXPIRED`, freeze and unfreeze rules | **P0 (Must)** | Yes (`SubscriptionService`) |
| **FEAT-04** | Fast Attendance Check-In | Instant check-in via hardware QR scanner or member ID lookup with sound/visual feedback | **P0 (Must)** | Yes (`AttendanceService`, `AttendancePage`) |
| **FEAT-05** | Daily Attendance Limit | Enforce maximum 1 check-in per member per calendar day | **P0 (Must)** | Yes (`attendance` unique index) |
| **FEAT-06** | Financial & Expense Tracking | Log daily club expenses and subscription payments with audit dates | **P0 (Must)** | Yes (`FinanceService`, `FinancePage`) |
| **FEAT-07** | Staff & Coach Payroll | Disburse fixed monthly salaries for users and coaches, prevent duplicate monthly disbursements | **P1 (High)** | Yes (`PayrollService`, `PayrollPage`) |
| **FEAT-08** | Weekly Class Timetable | Schedule class slots with day of week (1-7), start/end times, and capacity caps | **P1 (High)** | Yes (`ScheduleService`, `SchedulePage`) |
| **FEAT-09** | Role-Based Access Control | Distinct screens and permissions for `ADMIN` vs `RECEPTIONIST` | **P0 (Must)** | Yes (`UserRepository`, `NavigationHelper`) |
| **FEAT-10** | Bilingual UI & RTL Engine | Full real-time switching between English (LTR) and Arabic (RTL) | **P0 (Must)** | Yes (`LanguageManager`, `AppearanceHelper`) |
| **FEAT-11** | Pro Theme Engine | Dynamic Dark/Light mode with custom accent colors persisted across sessions | **P1 (High)** | Yes (`AppearanceSettings`, `AppearanceHelper`) |
| **FEAT-12** | Database Backup & Restore | 1-Click backup creation to user-specified path and clean restore | **P0 (Must)** | Yes (`DatabaseBackupHelper`) |
| **FEAT-13** | UTF-8 Excel/CSV Data Export | Export members, financial reports, and attendance records to Excel-compatible CSVs | **P1 (High)** | Yes (`DataExportHelper`) |

---

## 4. Key Performance Indicators (KPIs) & Acceptance Metrics

1. **Check-In Latency:** Check-in transaction committed and UI updated in `< 1500 ms`.
2. **Crash Rate:** Zero unhandled runtime exceptions during daily operations (all SQL/IO errors caught and surfaced via `AlertUtils`).
3. **Data Integrity:** 100% adherence to SQLite Foreign Key constraints (`PRAGMA foreign_keys = ON`).
4. **Localization Coverage:** 100% of UI labels, prompts, error dialogs, and table headers translated into Arabic.

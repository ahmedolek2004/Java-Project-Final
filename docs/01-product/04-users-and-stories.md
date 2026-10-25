# 04. Stakeholders, Personas & User Stories

> **Document ID:** DOC-PROD-004  
> **Standard:** ISO/IEC/IEEE 29148  
> **Target System:** Forge Fitness Management System  
> **Status:** Approved

---

## 1. Stakeholder Matrix

| Stakeholder Role | Primary Responsibilities | Main Concerns |
| :--- | :--- | :--- |
| **Gym Owner** | Capital investment, pricing strategy, profitability | Theft prevention, unauthorized entry, accurate financial reports |
| **Gym Manager / Admin** | Staff supervision, system configuration, payroll, audit | Security, employee account management, data backups |
| **Receptionist** | Member registration, check-ins, payment collection | System speed, simplicity, rapid QR scanning, legible Arabic text |
| **Head Coach / Trainer** | Leading classes, personal training sessions | Timetable conflicts, attendance counts, accurate payroll |
| **Member / Trainee** | Participating in workouts and martial arts classes | Fast entry without waiting, fair subscription freezing |

---

## 2. User Stories & Acceptance Criteria (Gherkin Format)

### US-01: Rapid Member Check-in via QR Code
**As a** Receptionist  
**I want to** scan a member's QR card with the USB scanner  
**So that** entry eligibility is verified and recorded in less than 2 seconds without typing.

```gherkin
Scenario: Successful check-in with active time-based subscription
  Given the member with phone "01012345678" has an "ACTIVE" time-based subscription ending tomorrow
  And the member has not checked in today
  When the receptionist scans the member's QR card
  Then the system displays a green success confirmation with the member's name
  And a new attendance record is stored with current timestamp
  And a confirmation sound is played

Scenario: Attempted check-in with expired subscription
  Given the member has a subscription with status "EXPIRED"
  When the receptionist scans the member's QR card
  Then the system rejects entry with a red error dialog stating "Subscription Expired"
  And no attendance record is saved

Scenario: Duplicate check-in attempt on the same day
  Given the member has already checked in today at 10:15 AM
  When the receptionist scans the member's QR card again at 02:30 PM
  Then the system rejects entry displaying "Member already checked in today at 10:15 AM"
  And no duplicate attendance record is created
```

---

### US-02: Session-Based Subscription Consumption
**As a** Receptionist  
**I want** the system to deduct one session upon check-in for martial arts classes  
**So that** the member cannot exceed their paid training sessions.

```gherkin
Scenario: Decrement session on check-in
  Given a member has an "ACTIVE" session-based subscription with 5 remaining sessions
  When the member checks in successfully
  Then the system decrements the remaining sessions to 4
  And displays "Check-in Approved: 4 sessions remaining"

Scenario: Auto-expiration on last session
  Given a member has an "ACTIVE" session-based subscription with 1 remaining session
  When the member checks in successfully
  Then the remaining sessions becomes 0
  And the subscription status automatically changes to "EXPIRED"
  And the receptionist is notified that this was the final session
```

---

### US-03: Registering a New Member
**As a** Receptionist  
**I want to** register a new member with their personal and contact details  
**So that** they exist in the club database and can be enrolled in plans.

```gherkin
Scenario: Valid member registration
  Given the receptionist enters full name "Omar Khaled" and phone "01122334455"
  When the receptionist submits the registration form
  Then a new member record is persisted with a generated unique ID
  And the member appears in the members directory table

Scenario: Rejecting duplicate phone number
  Given a member already exists with phone "01122334455"
  When the receptionist enters the same phone number for a new member
  Then the system blocks the submission and displays an error "Phone number already exists"
```

---

### US-04: Freezing and Unfreezing Subscriptions
**As a** Receptionist or Admin  
**I want to** freeze a member's active subscription during travel or injury  
**So that** their expiration date is preserved and check-in is temporarily halted.

```gherkin
Scenario: Freezing an active subscription
  Given a member has an "ACTIVE" subscription
  When the receptionist clicks "Freeze" on the subscription row
  Then the subscription status changes to "FROZEN"
  And check-in attempts during this state are rejected with "Subscription is Frozen"

Scenario: Unfreezing a frozen subscription
  Given a member has a "FROZEN" subscription
  When the receptionist clicks "Unfreeze"
  Then the subscription status reverts to "ACTIVE"
  And check-ins are permitted again
```

---

### US-05: Monthly Payroll Disbursement
**As an** Administrator  
**I want to** disburse monthly salaries to staff and coaches  
**So that** compensation is recorded and double payments for the same month are prevented.

```gherkin
Scenario: Successful monthly salary disbursement
  Given coach "Captain Hesham" has a base salary of 5000 EGP
  And no payroll record exists for him for month "2026-10"
  When the admin clicks "Disburse Salary" for "2026-10"
  Then a record is created in `payroll_payments` for 5000 EGP
  And the net profit metric on the dashboard immediately reflects the deduction

Scenario: Preventing duplicate monthly salary payment
  Given employee "Sarah" has already received salary for "2026-10"
  When the admin attempts to disburse salary again for "2026-10"
  Then the system blocks the transaction and alerts "Salary already paid for this period"
```

---

### US-06: Database Hot Backup
**As an** Administrator  
**I want to** export a full database backup with a single click  
**So that** business data is protected against hardware failure or OS crashes.

```gherkin
Scenario: Performing a verified database backup
  Given the database is currently open and operational
  When the admin navigates to Settings and clicks "Create Backup"
  And selects destination folder "D:\GymBackups"
  Then a copy named `gym_system_backup_YYYYMMDD_HHMMSS.db` is written
  And the system verifies that the backup file size > 0 and SQLite header is intact
  And a success alert informs the admin of successful completion
```

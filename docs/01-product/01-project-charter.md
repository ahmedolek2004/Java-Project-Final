# 01. Project Charter & Executive Summary

> **Document ID:** DOC-PROD-001  
> **Standard:** ISO/IEC/IEEE 29148:2018  
> **Project Name:** Forge Fitness & Martial Arts Management System  
> **Version:** 1.0  
> **Status:** Approved

---

## 1. Project Background & Problem Statement

Modern fitness centers and martial arts academies operate under demanding operational environments. Front-desk staff and management typically face severe operational bottlenecks:
1. **Slow Member Check-in:** Long queues form during peak hours due to manual registry lookups, leading to member frustration.
2. **Subscription Leakage & Fraud:** Unverified entry occurs when expired or frozen subscriptions are not flagged instantly in real-time.
3. **Financial Discrepancies:** Manual cash handling, untracked miscellaneous expenses, and complex monthly coach payroll calculations cause revenue leakage.
4. **Internet Dependency Risks:** Cloud-only SaaS management platforms fail during local ISP outages, bringing gym turnstiles and desk operations to a complete standstill.
5. **Language & Usability Barriers:** Many commercial systems lack native Right-To-Left (RTL) Arabic typography and intuitive dark-mode interfaces designed for dim front-desk lighting.

---

## 2. Project Vision & Purpose

**Forge Fitness Management System** provides an industrial-grade, fully offline-capable, standalone desktop management system built with **Java 25** and **JavaFX 21**. It empowers gym owners, receptionists, and head coaches with an ultra-fast, bilingual (Arabic RTL & English LTR) operational cockpit to manage members, automate access control via hardware QR scanners, track revenue and expenses, schedule training classes, and disburse staff salaries with zero cloud subscription fees.

---

## 3. High-Level Objectives & Success Criteria

| Objective | Target Metric | Verification Method |
| :--- | :--- | :--- |
| **Ultra-Fast Member Check-in** | Under 1.5 seconds per check-in (including scan & audio/visual prompt) | Performance test with USB HID QR scanner |
| **Strict Subscription Validation** | 100% prevention of unauthorized check-ins for expired/frozen plans | Automated JUnit integration tests |
| **Robust Offline Resilience** | 100% uptime regardless of internet connectivity; Zero data loss | Local SQLite ACID transactional storage |
| **Flawless Bilingual UX** | 100% dynamic UI translation (Arabic RTL / English LTR) without restarting | Manual and automated UI locale switching validation |
| **Audited Financial Reporting** | Real-time calculation of Gross Income, Expenses, Payroll, and Net Profit | Financial reconciliation reports |

---

## 4. Project Scope Summary

### 4.1 In Scope
- Member profile management with unique phone number constraint and photo/medical notes.
- Dual-mode subscription management (`TIME_BASED` and `SESSION_BASED`).
- Physical USB QR barcode scanning integration and manual check-in fallback.
- Daily check-in rule enforcement (one check-in per member per day constraint).
- Class scheduling with coach assignment and classroom capacity limits.
- Complete club financial tracking (revenues, expense entries, payment statuses).
- Automated monthly payroll calculation and disbursement for staff and coaches.
- Role-based Access Control (RBAC) separating `ADMIN` from `RECEPTIONIST`.
- 1-Click Database backup and restore to external locations.
- Data export to CSV/Excel with UTF-8 BOM encoding for correct Arabic rendering.

### 4.2 Out of Scope (Current Release)
- Cloud-hosted remote synchronization between multiple physical branches.
- Direct credit card POS merchant terminal hardware integration.
- Mobile application for end-members (members use printed QR cards or mobile screen QR).

---

## 5. Stakeholders & Governance

- **Project Sponsor / Gym Owner:** Requires financial transparency, audit logs, and business continuity.
- **Gym Manager / Head Administrator:** Requires user account management, salary configurations, and activity scheduling.
- **Receptionist / Front Desk Officer:** Requires quick check-in workflow, easy new member registration, and renewal processing.
- **Coaches & Trainers:** Rely on timetable accuracy and timely salary disbursements.
- **Gym Members:** Expect fast entry, accurate remaining session balances, and transparent receipts.

# Changelog

All notable changes to the **Forge Fitness & Martial Arts Management System** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-25

### Added
- **Core Architecture:** Clean layered desktop architecture written in Java 25 and JavaFX 21.0.6.
- **Embedded Persistence:** SQLite storage located in `~/.gym-system/gym_system.db` with enforced foreign keys and 5000ms busy timeout.
- **Member Management:** Full member registry with unique telephone constraint, photo, and medical disclosures.
- **Subscription Engine:** Dual subscription support (`TIME_BASED` and `SESSION_BASED`), custom discounts, freeze and unfreeze states.
- **Live Attendance:** Sub-second hardware QR check-in via USB keyboard emulation; daily check-in uniqueness constraint per member.
- **Financial Cockpit:** Real-time revenue audits, expense tracking, and net profit computation.
- **Payroll Disbursement:** Monthly staff and coach payroll with duplicate disbursement prevention per calendar month.
- **Internationalization (i18n):** Native Arabic with dynamic Right-To-Left (RTL) scene inversion and English LTR mode.
- **Pro Theming:** Dynamic Dark and Light modes with customizable accent palettes.
- **Disaster Recovery:** 1-Click database hot backup with SQLite header validation, and verified restore.
- **Data Export:** Excel CSV export with UTF-8 BOM encoding for Arabic character support.
- **Comprehensive Documentation Suite:** 20 verified ISO/IEC/IEEE 29148 engineering documents, C4 & UML 2.5 diagrams, traceability matrix, test plans, and operations manuals.

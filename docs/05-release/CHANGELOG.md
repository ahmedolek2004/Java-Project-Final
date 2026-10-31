# Changelog

All notable changes to the **Forge Fitness & Martial Arts Management System** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-10-31

### Fixed
- **Time zone:** SQLite `date('now')` is UTC while subscription dates are local. A subscription starting today stayed unusable until 02:00-03:00 local time and the daily check-in limit reset at the wrong hour. All queries now use local time (`SqlClock`).
- **Check-in rule:** one check-in per member per day now uses a stored local `check_in_date` column with a unique index. Existing databases migrate automatically and no attendance rows are removed.
- **Validation:** subscription status can only be set to `ACTIVE` or `FROZEN`; expenses reject blank descriptions, non-positive amounts and missing dates.

### Security
- Finance (payment history, expenses), payroll and staff-account operations are now enforced as administrator-only in the service layer through `AccessControl`.
- Medical notes are hidden from receptionists (member list and CSV export); receptionist edits can no longer overwrite stored notes.

### Changed
- Extracted `SubscriptionRules` (shared "usable subscription" SQL) and `SqlClock` (local-date policy).
- Moved the 12 result records from `GymRepository` to the `com.gym.system.dto` package.
- Split `GymRepository` and `ClubRepository` into Member, Subscription, Attendance, Finance, Settings, Activity, Coach, Schedule and Payroll repositories behind small `XxxDAO` interfaces; `GymRepository` is now a delegating facade. Added `SqlSupport` for shared JDBC helpers.

### Added
- Tests: `SubscriptionRulesTest`, `AttendanceDateMigrationTest`, `ServiceAuthorizationTest`.

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

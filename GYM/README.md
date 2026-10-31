# Forge Fitness Club Management

A Java 25 and JavaFX desktop application backed by a local SQLite database. The project follows
the `com.gym.system` package structure, separating domain models, data access, controllers,
utility services, and FXML/CSS views.

## Run

Install JDK 25 and run these commands from this directory. Adjust `JAVA_HOME` if your JDK is installed elsewhere:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
.\mvnw.cmd javafx:run
```

The first launch creates the database and these starter accounts:

| Role | Username | Initial password |
| --- | --- | --- |
| Administrator | `admin` | `admin123` |
| Receptionist | `reception` | `reception123` |

Change starter passwords from **Staff accounts** after signing in as an administrator.
Passwords are stored as salted PBKDF2 hashes. The database is created at
`%USERPROFILE%\.gym-system\gym_system.db`; it is not stored in the source tree.

## Source layout

- `com.gym.system.model`: encapsulated member, coach, subscription, and staff entities.
- `com.gym.system.dto`: read-only result records returned to the screens (`MemberRow`, `Payment`, `Dashboard`, ...).
- `com.gym.system.dao`: SQLite connection, one small `XxxDAO` contract per area, its `XxxRepository` JDBC
  implementation, and `GymRepository`, a facade that only delegates. Shared SQL rules live in
  `SubscriptionRules` (is a subscription usable today?) and `SqlClock` (what "today" means).
- `com.gym.system.service`: use cases. Every service checks the signed-in user through `AccessControl`.
- `com.gym.system.controller`: JavaFX FXML controllers, pages, forms and helpers.
- `com.gym.system.util`: localization, user session, and alert utilities.
- `com.gym.system.views.fxml` and `com.gym.system.views.css`: FXML screens and shared CSS.

Naming convention in `dao`: `XxxDAO` is the contract (interface), `XxxRepository` is its JDBC implementation.

## Access control

Permissions are enforced in the service layer, not only by hiding menu items, so a screen that forgets to hide a
button still cannot bypass them. A denied call throws `AccessDeniedException` before any data is touched.

| Area | Administrator | Receptionist |
| --- | --- | --- |
| Members, subscriptions, check-ins, schedule, coaches, activities | Full access | Full access |
| Medical notes | Read and edit | Can record notes when registering a member; cannot read them or overwrite them later |
| Payment history and expenses | Full access | Denied |
| Payroll | Full access | Denied |
| Staff accounts | Full access | Denied |

Known limits: the dashboard still shows monthly revenue and expenses to both roles, and a few screens still call the
repository directly. Both are tracked in the update plan below.

## Updates

### 1.1.0 (2026-10-31) - architecture and reliability update

**Fixed**
- "Today" was calculated in UTC while subscription dates are local, so in Egypt a subscription starting today
  stayed unusable until 02:00-03:00 and the daily check-in limit reset at 02:00-03:00 instead of midnight. All
  queries now use local time through `SqlClock`.
- The one-check-in-per-day rule now uses a stored local `check_in_date` column. Existing databases migrate
  automatically on first launch; no attendance rows are deleted.
- Subscription status can only be toggled between `ACTIVE` and `FROZEN`; expenses reject blank descriptions,
  non-positive amounts and missing dates.

**Security**
- Finance, payroll and staff-account operations are administrator-only at the service level.
- Medical notes are hidden from receptionists in the member list and CSV export, and reception edits can no
  longer overwrite stored notes.

**Changed (no behavior change)**
- The "subscription is usable" SQL condition, previously copied five times, lives in `SubscriptionRules`.
- The 12 result records moved from `GymRepository` to the `dto` package.
- `GymRepository` (505 lines) and `ClubRepository` were split into focused repositories behind small interfaces;
  `GymRepository` is now a delegating facade.

**Tests added**
- `SubscriptionRulesTest` (inclusive end date, start date, session packs), `AttendanceDateMigrationTest`
  (migration, idempotence, same-day rejection) and `ServiceAuthorizationTest` (roles, notes, validation).

**Next updates**
1. Route the remaining direct repository calls in pages through services; replace broad `catch (Exception)`.
2. Shared base class for the CRUD pages.
3. Role model and encapsulation clean-up, then integration tests and documentation refresh.

## Included workflows

- English/Arabic interface with right-to-left layout for Arabic.
- Saved light/dark appearance and a user-selectable application accent color.
- Role-aware sign-in and staff account creation.
- Member registration, search, and editing.
- Time-based subscriptions and session packs, including discounts.
- Subscription validation at check-in, session deduction, and one check-in per member per day.
- Generated `MEM-0001` member QR identifiers and USB keyboard-emulation scanner check-in with the active plan and remaining session balance.
- Printable member QR cards, subscription invoices, dashboard summaries and finance reports through the Windows-installed printer and print dialog.
- Recurring weekly class schedules, with activity/coach assignments, edit and remove actions.
- Staff accounts with roles, monthly salary, password changes and account deactivation.
- Coach and activity registration, editing and removal.
- Monthly salary payments for staff/coaches, with duplicate-payment protection and automatic expense recording.
- Pending charges recorded when subscriptions are sold; collected revenue includes only payments explicitly confirmed as received.
- Expense entry, monthly dashboard totals and activity-demand reports based on paid subscription charges.

Run the automated repository tests with:

```powershell
.\mvnw.cmd clean test
```

For QR check-in, use a USB QR scanner configured as a keyboard (HID) and set it to
send Enter after each scanned value. Member codes are printed on the member card.
Printing uses the printers installed in Windows; select the connected receipt or
office printer in the standard print dialog.

## Payment recording

Adding a subscription creates a **Pending** charge; it does not claim that money was collected. After staff have
actually received an offline payment, select the subscription and choose **Confirm payment received**. This records
the payment as **Paid** and includes it in collected revenue. The confirmation is one-way in the current app; refunds,
adjustments, card processing, recurring billing, and payment-provider callbacks are not implemented. Do not use this
manual action as a substitute for a payment-provider integration or as proof of an electronic transaction.

Pending charges do not currently block membership access. Freeze only changes the membership status and does not
extend its end date. Confirm the gym's access, freeze, cancellation, and refund policies before using these workflows
as a production financial or access-control system.

## Database backup and restore

The local SQLite database is `%USERPROFILE%\.gym-system\gym_system.db`. To make a consistent file-copy backup, exit
the application first, then copy the database file to a protected backup location. Restore only while the application
is closed, and keep a separate copy of the database being replaced. The app does not currently schedule backups,
encrypt backup files, or provide an export/restore screen. Never use a live production database as a test fixture.

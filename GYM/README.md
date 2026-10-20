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
- `com.gym.system.dao`: SQLite connection, DAO contract, and JDBC repository.
- `com.gym.system.controller`: JavaFX FXML controllers.
- `com.gym.system.util`: localization, user session, and alert utilities.
- `com.gym.system.views.fxml` and `com.gym.system.views.css`: FXML screens and shared CSS.

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

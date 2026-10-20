# GYM Feature Gap Analysis

## Scope and Evidence

Reviewed the project README, Maven configuration, JavaFX application/controller, FXML views, SQLite schema and repositories, localization resources, printing utilities, domain models, and the repository test. The project is a local Java 25/JavaFX desktop application backed by SQLite; it has no web service or configured external payment/notification provider.

Status meanings: **Implemented and verified** means there is implementation plus direct automated test evidence; **Partially implemented** means functionality exists but lacks important workflow, policy, protection, or test coverage; **Missing** means no implementation was found; **Blocked** means validation requires unavailable hardware, provider configuration, or business decisions.

The source initially failed a clean Maven compile because the application and test used APIs that the `LanguageManager` and `AppearanceSettings` utilities do not expose. Those call sites were aligned with the existing singleton/factory API. After the repair, `mvnw.cmd clean test` completed successfully: 2 tests, 0 failures, 0 errors. The single test class exercises repository-level flows, not JavaFX screens or printer/scanner hardware.

## Feature Inventory

| Area | Status | Evidence and gap |
| --- | --- | --- |
| Member profile create, search, edit | Implemented and verified | [GymApplication.java](GYM/src/main/java/com/gym/system/GymApplication.java) provides registration, name/phone/email/plan search and editing; [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) persists and updates member details. Phone uniqueness is enforced by the database. No duplicate-resolution workflow or member status filters. |
| Membership history | Partially implemented | [GymRepository.java](GYM/src/main/java/com/gym/system/dao/GymRepository.java) retains subscription rows and lists them, but there is no member-specific history view or immutable change history. Adding a subscription expires every prior ACTIVE row for that member, regardless of whether its dates have started or whether it is frozen. |
| Plans, prices, discounts | Partially implemented | Staff enter a plan name, price, discount, duration/session count, date, and optional activity when selling in [GymApplication.java](GYM/src/main/java/com/gym/system/GymApplication.java). There is no reusable plan catalog, recurring plan, trial plan, or price/version history. |
| Time and session memberships | Implemented and verified | [Subscription.java](GYM/src/main/java/com/gym/system/model/Subscription.java) models time/session plans. [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) verifies discounted session-pack creation, time-plan eligibility, QR check-in, and session deduction. No recurring billing. |
| Membership lifecycle | Partially implemented | ACTIVE/FROZEN/EXPIRED statuses and freeze/unfreeze exist in [GymRepository.java](GYM/src/main/java/com/gym/system/dao/GymRepository.java). Tests now verify freeze blocks access, reactivation restores eligibility, and renewal preserves the old subscription row. Cancellation remains absent; renewal still expires previous ACTIVE memberships without a configurable overlap policy. Freezing does not extend the end date, and time access treats the end date as inclusive. These policies need owner confirmation before being changed. |
| Family/linked memberships | Missing | No linked-member model, schema relation, or workflow found in [DatabaseConnection.java](GYM/src/main/java/com/gym/system/dao/DatabaseConnection.java). |
| Member check-in and access validation | Implemented and verified | Manual selection and USB HID QR flows are in [GymApplication.java](GYM/src/main/java/com/gym/system/GymApplication.java); [GymRepository.java](GYM/src/main/java/com/gym/system/dao/GymRepository.java) validates ACTIVE/date/session eligibility and consumes sessions. [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) verifies valid/invalid QR and check-in behavior. QR represents a member ID, not an access credential. |
| Duplicate check-in prevention | Partially implemented | [DatabaseConnection.java](GYM/src/main/java/com/gym/system/dao/DatabaseConnection.java) creates a unique index for one check-in per member per SQLite date, and [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) now verifies duplicate rejection. There is no authorized override, and the UI displays a generic check-in error for database rejection. |
| Attendance metadata and hardware | Partially implemented | Attendance stores member, subscription, and timestamp in [DatabaseConnection.java](GYM/src/main/java/com/gym/system/dao/DatabaseConnection.java). Location, operator identity, and method are not stored. A compatible USB keyboard-emulation scanner is documented in [README.md](GYM/README.md); no physical door/access-control integration exists. |
| Billing, invoices, balances | Partially implemented | Subscription sales now create PENDING charges. Staff can explicitly confirm an actually received offline payment; only PAID amounts count as collected revenue, and invoice/payment history distinguishes status. [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) verifies pending-to-paid behavior, no premature revenue, payment-date handling, and repeat-confirmation rejection. There is no payment-method field, balance aging, refund, adjustment, or payment-attempt history. No payment provider is configured. |
| Revenue and expense reporting | Partially implemented | Dashboard and activity-demand revenue now count only PAID subscription charges; expenses and payroll are included in the expense total. Payment status is included in invoice and financial payment-history output. Refund accounting, date-range reports, and transaction reconciliation remain absent. [ClubRepository.java](GYM/src/main/java/com/gym/system/dao/ClubRepository.java), [GymRepository.java](GYM/src/main/java/com/gym/system/dao/GymRepository.java), and [PrintController.java](GYM/src/main/java/com/gym/system/controller/PrintController.java) contain the report queries/rendering. |
| Leads, inquiries, retention | Missing | No lead/inquiry entity, conversion stages, follow-up owner/history, inactivity queue, or retention task was found in the inspected source/schema. |
| Notifications | Missing | No email/SMS/push provider, delivery log, retry policy, or notification workflow found. |
| Coaches and activities | Partially implemented | Coach/activity create, edit, delete, assignment, salary, and personal-trainer flag exist in [ClubRepository.java](GYM/src/main/java/com/gym/system/dao/ClubRepository.java) and the dashboard. Availability, trainer assignments to members, and training plans/exercise library are absent. |
| Classes and bookings | Partially implemented | Recurring weekly schedules have activity/coach/time/capacity and CRUD; [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) tests schedule CRUD. Capacity is only a stored/displayed value: no member bookings, cancellation, waitlist, or per-class attendance exists. |
| Measurements and health privacy | Partially implemented | Member medical notes are collected and stored, but the member table query supplies them to both ADMIN and RECEPTIONIST views. There is no field-level permission or dated measurement/progress record. |
| Staff roles and sensitive permissions | Partially implemented | Passwords use salted PBKDF2, roles are ADMIN/RECEPTIONIST, and finance/payroll/staff navigation is hidden from reception in [GymApplication.java](GYM/src/main/java/com/gym/system/GymApplication.java); repository tests verify authentication and account deactivation. Authorization is primarily UI navigation rather than enforced by a service boundary, and medical notes remain visible to reception. No audit log for sensitive changes. |
| Payroll and expenses | Implemented and verified | Monthly staff/coach salary recording, duplicate-month protection, and automatic expense entry are implemented in [ClubRepository.java](GYM/src/main/java/com/gym/system/dao/ClubRepository.java) and covered by [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java). General expense entry has no categories or approval trail. |
| Retail and inventory | Missing | No product, stock movement, sale, or inventory schema/workflow found. |
| Multi-location | Missing | No location entity or location-scoped data. Not justified by requirements currently documented in [README.md](GYM/README.md). |
| Export and backup | Partially implemented | The database is stored at `%USERPROFILE%\.gym-system\gym_system.db`; [README.md](GYM/README.md) now documents a closed-app manual backup/restore procedure. There is no automated schedule, in-app restore, or data export. |
| Member portal and mobile | Missing | This is a JavaFX desktop app, not a web/mobile portal. No member self-service for profile, payments, attendance, or bookings. |
| Localization and accessibility | Partially implemented | English/Arabic resource bundles and RTL orientation exist in [messages_en.properties](GYM/src/main/resources/com/gym/system/messages_en.properties), [messages_ar.properties](GYM/src/main/resources/com/gym/system/messages_ar.properties), and [GymApplication.java](GYM/src/main/java/com/gym/system/GymApplication.java). The UI is desktop-only and sets a 900px minimum width; end-to-end accessibility, responsive mobile use, and complete translation coverage are not tested. |
| Setup and automated tests | Partially implemented | [README.md](GYM/README.md) documents Java 25, Maven wrapper, starter credentials, SQLite location, QR scanner, printer setup, payment confirmation, and manual backup/restore guidance. `mvnw.cmd clean test` passed 2 repository test methods after the API compatibility fix and billing enhancement. JavaFX interaction, print output, backup/restore execution, and external hardware were not exercised. |

## Current Rules and Explicit Assumptions

Observed rules:

- A time-based membership is eligible when ACTIVE and `start_date <= today <= end_date`; the end date is inclusive.
- A session-based membership is eligible when ACTIVE with remaining sessions greater than zero; a successful check-in consumes one session, and the final session expires the membership.
- The SQLite unique index limits a member to one attendance row per database-local date.
- Adding a membership currently expires all ACTIVE memberships for that member. Freeze/unfreeze changes status only and does not pause the calendar dates.
- Finance dashboard recognizes only PAID payments, while the activity-demand report currently includes all subscription prices.
- Finance, payroll, and staff-account navigation is admin-only; other operational pages are available to both current roles.

Assumption adopted for the incremental billing repair (not a payment-provider integration): creating a subscription records an amount due as PENDING; a signed-in staff member may mark it PAID only after confirming that offline payment was actually received. The app will not claim to charge a card or verify a provider callback. Existing records will not be rewritten because their true collection state cannot be inferred. Refunds, failed attempts, payment methods, and audited adjustments remain out of scope.

Owner decisions still needed before altering policy: freeze duration/extension rules; whether end dates are inclusive; overlap/early renewal behavior; cancellation and refund rules; overdue-balance access policy; manual check-in override authority; class cancellation/booking windows; and whether medical notes should be hidden from reception.

## Highest-Priority Remaining Gaps

1. Enforce sensitive-operation permissions below UI navigation and restrict medical-note access.
2. Define membership freeze, renewal, cancellation, refund, and overdue-access policies with the gym owner before expanding lifecycle behavior.
3. Add booking/attendance, lead follow-up, retention reminders, and member self-service only after confirming operational requirements and provider choices.
4. Add refund accounting, payment adjustments, and reconciliation only after a documented finance policy exists.

## Post-Audit Changes

- Fixed source compilation by updating [GymApplication.java](GYM/src/main/java/com/gym/system/GymApplication.java) and [GymRepositoryTest.java](GYM/src/test/java/com/gym/system/dao/GymRepositoryTest.java) to use the existing `LanguageManager` singleton and `AppearanceSettings` factory/accessor API.
- Changed new subscription charges to PENDING, added manual confirmation with a one-way PENDING-to-PAID transition, and showed payment status in the subscription table, invoice, and payment history. No database migration was needed because the existing status constraint already permits PENDING; existing payment rows were not rewritten.
- Changed activity-demand revenue to count paid subscription amounts only.
- Expanded the repository test assertions for same-day duplicate check-ins, payment collection/date/idempotency, renewal history, and freeze/reactivation access.
- Updated [README.md](GYM/README.md) with clean test instructions, the manual offline-payment workflow, and closed-app database backup/restore steps.

# Clean-code refactor notes

## What was cleaned in this delivery

- Removed generated Maven `target/` output from the source package.
- Removed IDE-local project/workspace metadata.
- Removed the repository's embedded `.git` directory from the distributable archive.
- Added a root `.gitignore` for build output, IDE files, local SQLite data, and logs.
- Kept the existing source package names and application workflows intact to avoid silently breaking the JavaFX/FXML wiring or SQLite data compatibility.

## Main refactoring opportunities identified

1. **`GymApplication` is a god class (~1,274 lines).** It currently owns application startup, navigation, page construction, member/subscription workflows, check-in, schedules, staff, finance, payroll, localization, theming, and formatting. Extract one controller per feature (Members, Subscriptions, Attendance, Schedule, Coaches/Activities, Finance/Payroll, Staff) and keep `GymApplication` as the composition root.
2. **Persistence concerns are mixed.** `GymRepository` contains many unrelated workflows and nested row/result types. Split it into focused repositories (members, subscriptions/payments, attendance, scheduling, staff, finance) behind small interfaces, with a service layer enforcing business rules.
3. **DAO/repository naming overlaps.** `MemberDAO`, `SubscriptionDAO`, `UserDAO`, `UserRepository`, `ClubRepository`, and `GymRepository` need a single consistent persistence convention. Remove duplicate abstractions only after confirming every call site and preserving database behavior.
4. **Authorization is primarily navigation-level.** Role checks should be enforced in service methods, not only by hiding navigation items. Add tests proving receptionist access is denied for finance, payroll, staff management, and sensitive member fields.
5. **Test coverage is concentrated in one repository test.** Add unit tests for services and validation, plus JavaFX controller smoke tests where practical. Keep tests isolated from a real user database.
6. **Business policies need explicit domain rules.** Subscription renewal, frozen membership end dates, inclusive expiry boundaries, refunds, and cancellation should be specified before changing code.
7. **Configuration should be centralized.** Keep database paths, starter-account behavior, and runtime options in a small configuration class; never commit local database files or credentials.

## Safe refactoring order

1. Establish baseline behavior with `./mvnw clean test` (Windows: `mvnw.cmd clean test`).
2. Extract domain services without changing UI or schema.
3. Extract feature controllers from `GymApplication`, one workflow at a time.
4. Split repositories behind interfaces and preserve existing SQL behavior.
5. Add authorization tests and migration/backup checks.
6. Run regression tests and manually smoke-test Arabic RTL, printing, and QR scanner input.

## Verification limitation

This delivery does not claim that the full application has been rebuilt or tested in this environment. The project declares Java 25, while the available runtime here is Java 21 and Maven is not installed. The source and declared Java target were intentionally left unchanged rather than lowering the required Java version without validating the project's compatibility.

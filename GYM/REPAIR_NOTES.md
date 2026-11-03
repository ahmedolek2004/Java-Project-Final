# Repair notes — November 3, 2026

## Login screen
- Reworked the FXML card to remain centered and content-height, with a 500 px preferred/max width and responsive side padding.
- Kept the existing `LoginController` field IDs and action handlers intact.
- Kept the existing shared `style.css` classes and scene-level stylesheet loading.

## Members screen
- Restored an obvious **Edit** action in the members toolbar, enabled when a member is selected.
- Added an obvious **Delete** action in the toolbar for administrators, also enabled only when a member is selected.
- Kept the Edit action in the member detail panel and kept the Delete action there for administrators.
- Kept deletion administrator-only in both the UI and `MemberService`, matching the existing service authorization test and the application's security model.
- Deletion asks for confirmation, refreshes the member list, deletes the member's subscriptions and attendance in one database transaction, and preserves payment history with its `member_id` set to NULL.

## Validation
- Login FXML parsed successfully as XML.
- All Java source files passed a `javac` syntax parse check (no syntax diagnostics).
- The member deletion SQL transaction was exercised against a temporary copy of the SQLite database and passed.
- Full Maven compilation/tests could not be run in this environment because Maven Wrapper could not download Maven from `repo.maven.apache.org` (DNS/network unavailable). Run `mvnw.cmd clean test` on a machine with internet access to complete dependency resolution and tests.

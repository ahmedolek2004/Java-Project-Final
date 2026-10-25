# 15. Bug Log & Risk Register

> **Document ID:** DOC-QA-015  
> **Standard:** ISO 31000 Risk Management  
> **Target System:** Forge Fitness Management System  
> **Status:** Active & Monitored

---

## 1. Project Risk Register

| Risk ID | Description | Probability | Impact | Severity | Mitigation Strategy | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **RSK-01** | Sudden power cut or OS crash during active member check-in causing SQLite file corruption. | Low | Critical | **High** | SQLite WAL (Write-Ahead Logging) + automatic daily hot backup prompts + immediate header checks on boot. | Lead Architect |
| **RSK-02** | Front-desk staff losing window focus on the scanner text box, causing scans to be ignored. | Medium | Medium | **Medium** | Prominent visual scanner focus indicator (glowing border) + auto-refocus listener when the Attendance page is active. | UI/UX Lead |
| **RSK-03** | Receptionist double-clicking payment button creating duplicate cash revenue records. | Medium | High | **High** | UI button throttling (disabling submit button on first click) + SQL unique constraints on transaction IDs. | Dev Team |
| **RSK-04** | Accidental database file deletion from the user profile directory (`~/.gym-system`). | Low | Critical | **High** | 1-Click backup helper defaults to external drive; directory path created with restricted user permissions. | DevOps |
| **RSK-05** | Arabic character corruption when opening exported CSVs in localized Microsoft Excel. | High | Medium | **Medium** | Prepended standard `UTF-8 BOM` (`\uFEFF`) to all exported CSV byte streams (`DataExportHelper.java`). | Dev Team |

---

## 2. Active & Resolved Defect Log (Bug Tracker)

### 2.1 Resolved Defects
- **BUG-001 (Resolved):** `one_checkin_per_member_day` index was originally matching the entire ISO timestamp rather than the date component, allowing multiple check-ins on the same day.  
  *Fix:* Altered index to use SQLite expression index: `ON attendance(member_id, date(check_in_time))`.
- **BUG-002 (Resolved):** Arabic text appeared truncated or left-aligned on modal form dialogs.  
  *Fix:* Enforced explicit `NodeOrientation.RIGHT_TO_LEFT` propagation to newly spawned dialog stages in `FormDialog.java`.
- **BUG-003 (Resolved):** Duplicate salary disbursement could be created if an admin repeatedly clicked "Disburse" during month-end payroll.  
  *Fix:* Added composite unique constraint `UNIQUE(employee_type, employee_id, pay_month)` in `DatabaseConnection.java`.

### 2.2 Monitored Defects / Watchlist
- **BUG-004 (Monitoring):** Some third-party wireless 2.4GHz barcode scanners send an initial carriage return (`\r`) without newline (`\n`), causing intermittent premature event firing.  
  *Status:* Investigating generic string sanitizer in `AttendancePage.java`.

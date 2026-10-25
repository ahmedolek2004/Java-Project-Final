# 20. Maintenance & Troubleshooting Guide

> **Document ID:** DOC-OPS-020  
> **Audience:** System Administrators & Support Technicians  
> **Target System:** Forge Fitness Management System  
> **Status:** Production-Ready

---

## 1. Routine Preventative Maintenance

To maintain optimal performance across years of daily front-desk operations:

### 1.1 SQLite Vacuuming & Space Defragmentation
Over time, frequent insertions and deletions (such as temporary logs or expired sessions) cause SQLite file fragmentation. Run an annual or bi-annual vacuum:
```sql
VACUUM;
PRAGMA optimize;
```
This reclaims unused disk blocks and rebuilds database B-Trees, speeding up table scans and member lookups.

### 1.2 Database Integrity Check
To verify that no disk sector corruption has affected the database file:
```sql
PRAGMA integrity_check;
```
Expected output: `ok`. If any corruption is reported, immediately restore the most recent verified backup file.

---

## 2. Common Operational Issues & Troubleshooting Matrix

| Symptom | Probable Root Cause | Resolution Procedure |
| :--- | :--- | :--- |
| **Scanner beeps but system does not record check-in.** | Input field on `AttendancePage` has lost window focus. | Click on the attendance input box. Ensure the border turns glowing green/accent color. Check scanner USB cable connection. |
| **"Database is locked" (`SQLITE_BUSY`) error alert.** | Another background thread or external backup process is holding an exclusive lock on the database file. | The application has `PRAGMA busy_timeout = 5000` ms configured. If the error persists, ensure no external backup utility (e.g. Dropbox/OneDrive sync) is scanning `gym_system.db` during operating hours. Exclude `~/.gym-system` from cloud sync tools. |
| **Application fails to boot on a new computer.** | Missing Java 25 runtime environment or wrong bitness (32-bit vs 64-bit). | Install 64-bit JDK 25 or run the packaged `.msi` native installer which bundles its own self-contained runtime. |
| **Arabic text displays as question marks (`????`) in exported Excel CSV.** | CSV file opened in an older third-party spreadsheet tool that does not parse the UTF-8 BOM. | Open file in Microsoft Excel 2016+ or modern Google Sheets. Verify file is opened with UTF-8 encoding. |
| **Member QR card barcode does not scan from phone screen.** | Phone screen brightness is too low, or reflective screen protector is scattering the laser/LED scanner light. | Increase member's phone brightness to maximum, or print a standard physical plastic/laminated QR card via `PrintController`. |

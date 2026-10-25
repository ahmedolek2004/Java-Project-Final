# 18. Administrator & Club Manager Guide

> **Document ID:** DOC-OPS-018  
> **Audience:** Gym Owners, Club General Managers, IT Administrators  
> **Classification:** Confidential / Administrative Operations

---

## 1. System Administration & RBAC Management

Administrators possess exclusive access to financial records, expenses, payroll disbursements, staff accounts, and database maintenance tools.

### 1.1 Managing Staff Accounts (`TeamPage.java`)
1. Navigate to **Team Management** on the sidebar.
2. Click **"+ Add User"** to provision a new receptionist account.
3. Assign a secure initial password (minimum 8 characters). The system hashes the password with **PBKDF2-HMAC-SHA256** and a 16-byte cryptographic salt before writing to disk.
4. If a staff member leaves the gym, toggle their status to **Disabled** (`active = 0`) to immediately revoke their login permissions without breaking historical audit trails.

---

## 2. Monthly Payroll Disbursement Operations (`PayrollPage.java`)

1. At the end of each calendar month, navigate to **Payroll**.
2. Select the current accounting month (e.g. `2026-10`).
3. The payroll table renders every active employee (staff users and coaches) along with their configured base salary.
4. **Disbursing Salaries:**
   - Click **"Disburse"** next to each employee after handing over cash or bank transfer.
   - The status immediately updates from `UNPAID` to `PAID`.
   - **Protection Rule:** The system strictly rejects accidental double-disbursements for the same month via database unique constraints.
5. Total paid salaries are automatically deducted from the monthly net profit calculation.

---

## 3. Financial Auditing & Club Expenses (`FinancePage.java`)

1. Navigate to **Finance**.
2. **Review High-Level KPIs:**
   - **Total Revenue (إجمالي الإيرادات):** Sum of all subscription fees collected.
   - **Operational Expenses (المصروفات التشغيلية):** Rent, water/electricity bills, equipment maintenance, and supplements restock.
   - **Payroll Expenses (إجمالي الرواتب):** Sum of all disbursed monthly staff and coach salaries.
   - **Net Profit (صافي الربح):** Total Revenue minus (Expenses + Payroll).
3. **Logging Club Expenses:**
   - Click **"+ Add Expense"**.
   - Input description (e.g. "فاتورة الكهرباء لشهر أكتوبر" or "صيانة أجهزة المشي"), amount in EGP, and date.
   - Click **"Save Expense"**.

---

## 4. Disaster Recovery & Backup Procedures

### 4.1 Creating a Hot Database Backup
1. Navigate to **Settings** -> **Database Management**.
2. Click **"Create Backup Now"**.
3. Select an external storage medium (e.g., a dedicated USB flash drive or secondary hard drive partition).
4. The system validates write permissions and creates a timestamped copy:
   ```
   gym_system_backup_20261025_214500.db
   ```
5. A confirmation dialog displays upon successful SQLite header verification.

### 4.2 Restoring from Backup
> ⚠️ **CAUTION:** Restoring a database file replaces all existing records with the backup file's contents. Only perform this operation during system maintenance hours.
1. Click **"Restore Database"**.
2. Browse and select the verified `.db` backup file.
3. Confirm the warning dialog.
4. The system safely swaps the SQLite file and reloads all application caches and views.

# 08. Database Design & Data Dictionary

> **Document ID:** DOC-ARCH-008  
> **Database Engine:** SQLite 3.50+ (via `org.xerial:sqlite-jdbc:3.50.3.0`)  
> **Target File Location:** `~/.gym-system/gym_system.db`  
> **Integrity Enforcement:** `PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 5000;`  
> **Status:** Verified against `DatabaseConnection.java`

---

## 1. Schema Invariants & Global Policies

1. **Foreign Key Integrity:** All relationships enforce referential integrity (`ON DELETE RESTRICT` or `ON DELETE SET NULL`).
2. **Timestamps:** System audit dates stored as ISO-8601 strings (`TEXT`) using `CURRENT_TIMESTAMP` or `YYYY-MM-DD`.
3. **Monetary Precision:** All currency amounts stored as `REAL` with non-negative constraints (`CHECK(amount >= 0)`).

---

## 2. Table Specifications & Data Dictionary

### 2.1 Table: `users`
Stores administrative and front-desk staff accounts.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique internal user ID |
| `username` | `TEXT` | `NOT NULL UNIQUE` | System login handle |
| `password_hash` | `TEXT` | `NOT NULL` | PBKDF2 hash with embedded salt |
| `full_name` | `TEXT` | `NOT NULL` | Employee full legal name |
| `role` | `TEXT` | `NOT NULL CHECK(role IN ('ADMIN', 'RECEPTIONIST'))` | Access permission tier |
| `phone` | `TEXT` | None | Contact telephone |
| `salary` | `REAL` | `NOT NULL DEFAULT 0 CHECK(salary >= 0)` | Monthly base salary in EGP |
| `active` | `INTEGER` | `NOT NULL DEFAULT 1` | Soft-delete flag (1=active, 0=disabled) |
| `created_at` | `TEXT` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Account creation timestamp |

---

### 2.2 Table: `members`
Stores registered gym members and academy trainees.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique member ID (also encoded in QR card) |
| `full_name` | `TEXT` | `NOT NULL` | Member full name |
| `phone` | `TEXT` | `NOT NULL UNIQUE` | Unique phone identifier (enforces BR-MEM-001) |
| `email` | `TEXT` | None | Optional contact email |
| `gender` | `TEXT` | None | Gender identifier |
| `medical_notes` | `TEXT` | None | Critical health considerations or physical injuries |
| `created_at` | `TEXT` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Registration timestamp |

---

### 2.3 Table: `activities`
Stores sports and martial arts disciplines offered by the academy.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique activity identifier |
| `name_ar` | `TEXT` | `NOT NULL` | Arabic discipline name (e.g. كيك بوكسينغ) |
| `name_en` | `TEXT` | `NOT NULL` | English discipline name (e.g. Kickboxing) |
| `description` | `TEXT` | None | Activity description & equipment details |

---

### 2.4 Table: `coaches`
Stores academy trainers and personal fitness instructors.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique coach ID |
| `name` | `TEXT` | `NOT NULL` | Coach full name |
| `phone` | `TEXT` | None | Contact phone |
| `activity_id` | `INTEGER` | `REFERENCES activities(id) ON DELETE SET NULL` | Specialty martial art / sport |
| `salary` | `REAL` | `NOT NULL DEFAULT 0 CHECK(salary >= 0)` | Fixed monthly base salary |
| `is_personal_trainer`| `INTEGER` | `NOT NULL DEFAULT 0` | Flag: 1 for PT, 0 for group coach |

---

### 2.5 Table: `subscriptions`
Stores membership plans purchased by gym members.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique subscription plan record |
| `member_id` | `INTEGER` | `NOT NULL REFERENCES members(id)` | Foreign key to member |
| `plan_name` | `TEXT` | `NOT NULL` | Plan title (e.g. Gold 3-Months, 12 Boxing Sessions) |
| `plan_type` | `TEXT` | `NOT NULL CHECK(plan_type IN ('TIME_BASED', 'SESSION_BASED'))` | Invariant plan classification |
| `duration_months` | `INTEGER` | None | Duration in months for `TIME_BASED` plans |
| `total_sessions` | `INTEGER` | None | Total initial sessions for `SESSION_BASED` plans |
| `remaining_sessions`| `INTEGER`| None | Remaining session pool |
| `start_date` | `TEXT` | `NOT NULL` | Activation start date (`YYYY-MM-DD`) |
| `end_date` | `TEXT` | None | Expiration end date for `TIME_BASED` plans |
| `price` | `REAL` | `NOT NULL CHECK(price >= 0)` | Base package price before discount |
| `discount_applied` | `REAL` | `NOT NULL DEFAULT 0 CHECK(discount_applied >= 0 AND <= 100)` | Percentage discount |
| `status` | `TEXT` | `NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'EXPIRED', 'FROZEN'))` | Current lifecycle state |
| `activity_id` | `INTEGER` | `REFERENCES activities(id) ON DELETE SET NULL` | Linked sports discipline |

---

### 2.6 Table: `attendance`
Stores verified entry check-in events.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique check-in event ID |
| `member_id` | `INTEGER` | `NOT NULL REFERENCES members(id)` | Foreign key to member |
| `subscription_id` | `INTEGER` | `NOT NULL REFERENCES subscriptions(id)` | Foreign key to active subscription used |
| `check_in_time` | `TEXT` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Exact timestamp of entry |

#### Critical Index:
```sql
CREATE UNIQUE INDEX IF NOT EXISTS one_checkin_per_member_day 
ON attendance(member_id, date(check_in_time));
```
*Purpose:* Enforces business rule **BR-ATT-002**, mechanically guaranteeing that a member cannot check in twice on the same calendar day.

---

### 2.7 Table: `class_schedule`
Stores weekly training timetable slots.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Schedule slot ID |
| `title` | `TEXT` | `NOT NULL` | Session title |
| `activity_id` | `INTEGER` | `NOT NULL REFERENCES activities(id)` | Sport discipline |
| `coach_id` | `INTEGER` | `REFERENCES coaches(id) ON DELETE SET NULL` | Instructor |
| `day_of_week` | `INTEGER` | `NOT NULL CHECK(day_of_week BETWEEN 1 AND 7)` | 1 = Monday ... 7 = Sunday |
| `start_time` | `TEXT` | `NOT NULL` | Session start time (`HH:mm`) |
| `end_time` | `TEXT` | `NOT NULL` | Session end time (`HH:mm`) |
| `capacity` | `INTEGER` | `NOT NULL CHECK(capacity > 0)` | Maximum allowed attendees |
| `active` | `INTEGER` | `NOT NULL DEFAULT 1` | Active timetable flag |

*Table Constraint:* `CHECK(start_time < end_time)`

---

### 2.8 Table: `payments` & `expenses`
Stores revenue items and outgoing operational expenses.

#### `payments`:
- `id` (PK), `member_id` (FK), `description`, `amount` (`CHECK >= 0`), `payment_date`, `status` (`CHECK IN ('PAID', 'PENDING')`), `subscription_id` (FK).

#### `expenses`:
- `id` (PK), `description`, `amount` (`CHECK >= 0`), `expense_date`.

---

### 2.9 Table: `payroll_payments`
Stores staff and coach monthly salary disbursements.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Payment voucher ID |
| `employee_type` | `TEXT` | `NOT NULL CHECK(employee_type IN ('USER', 'COACH'))` | Discriminator |
| `employee_id` | `INTEGER` | `NOT NULL` | ID in `users` or `coaches` |
| `employee_name` | `TEXT` | `NOT NULL` | Snapshot of name at payment time |
| `pay_month` | `TEXT` | `NOT NULL` | Accounting period (`YYYY-MM`) |
| `amount` | `REAL` | `NOT NULL CHECK(amount > 0)` | Disbursed amount |
| `paid_at` | `TEXT` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Transaction timestamp |

*Unique Constraint:*
```sql
UNIQUE(employee_type, employee_id, pay_month)
```
*Purpose:* Enforces business rule **BR-FIN-001**, guaranteeing no employee is paid their monthly salary twice for the same calendar month.

---

## 3. Migration & Evolution Strategy

Database schema evolution is managed through programmatic idempotent initialization in `DatabaseConnection.java`:
1. Core tables created with `CREATE TABLE IF NOT EXISTS`.
2. Incremental column migrations checked using SQLite's `PRAGMA table_info(tableName)` before issuing `ALTER TABLE ADD COLUMN`.
3. Strict foreign key validation enabled upon every connection.

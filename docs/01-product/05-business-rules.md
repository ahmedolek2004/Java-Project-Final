# 05. Business Rules & Domain Glossary

> **Document ID:** DOC-PROD-005  
> **Standard:** ISO/IEC/IEEE 29148  
> **Target System:** Forge Fitness Management System  
> **Status:** Active & Invariant

---

## 1. Domain Glossary & Terminology

| Term | English Definition | مصطلح وتعريف بالعربية |
| :--- | :--- | :--- |
| **Member** | An individual registered in the system with personal and contact details. | **العضو / المشترك:** العميل المسجل في النظام ببياناته الشخصية ورقم هاتفه. |
| **Time-Based Subscription** | A plan bounded by calendar start and end dates (e.g., 1 Month, 3 Months, Annual). | **اشتراك محدد بالمدة:** اشتراك يبدأ في تاريخ وينتهي في تاريخ بغض النظر عن عدد مرات الحضور. |
| **Session-Based Subscription** | A plan bounded by a fixed pool of training sessions (e.g., 12 Kickboxing sessions). | **اشتراك محدد بالحلقات/الجلسات:** اشتراك يتناقص رصيده مع كل تسجيل حضور فعلي حتى ينفد. |
| **Check-in / Attendance** | A single verified entry event for a member on a specific date. | **تسجيل الحضور:** إثبات دخول العضو إلى الصالة الرياضية بواسطة بطاقة الـ QR أو البحث اليدوي. |
| **Freeze** | Temporary suspension of subscription rights preserving remaining days or sessions. | **تجميد الاشتراك:** إيقاف مؤقت للاشتراك بناءً على رغبة العضو لحفظ رصيد الأيام أو الجلسات. |
| **Payroll Disbursement** | Fixed monthly remuneration paid to an employee or coach. | **صرف الراتب:** دفع الاستحقاق المالي الشهري للموظف أو المدرب وتوثيقه محاسبياً. |
| **Net Profit** | Operating income minus total expenses and payroll. | **صافي الدخل / الربح:** الفارق بين إجمالي اشتراكات الأعضاء ومجموع المصروفات والرواتب. |

---

## 2. Invariant Business Rules (BR)

### 2.1 Membership Rules (BR-MEM)
- **BR-MEM-001 (Phone Uniqueness):** No two members can share the same telephone number. The phone number serves as a primary search index and operational identifier.
- **BR-MEM-002 (Medical Disclosure):** If medical notes are provided during registration, a visual alert indicator must appear on the receptionist's screen during check-in.

### 2.2 Subscription & Pricing Rules (BR-SUB)
- **BR-SUB-001 (Dual Classification):** A subscription must be either `TIME_BASED` or `SESSION_BASED`. It cannot be untyped.
- **BR-SUB-002 (Discount Boundary):** Any applied discount must be within the range `[0.0, 100.0]%`. A discount outside this interval is invalid.
- **BR-SUB-003 (Net Price Invariant):**  
  $$\text{Final Price} = \text{Base Price} \times \left(1.0 - \frac{\text{Discount Applied}}{100.0}\right)$$  
  The resulting final price must be $\ge 0.0$.
- **BR-SUB-004 (Active Exclusivity per Sport):** A member may have only one `ACTIVE` subscription per activity discipline simultaneously.
- **BR-SUB-005 (Session Exhaustion):** When a `SESSION_BASED` plan reaches `remaining_sessions == 0`, its status immediately becomes `EXPIRED`.

### 2.3 Attendance & Access Rules (BR-ATT)
- **BR-ATT-001 (Access Condition):** Access is granted if and only if the member possesses a subscription whose status is `ACTIVE` and within valid date/session bounds.
- **BR-ATT-002 (Daily Check-in Cap):** A member is permitted exactly **one check-in per calendar day** across the facility. Secondary attempts on the same calendar day are blocked by database integrity constraints:
  ```sql
  CREATE UNIQUE INDEX one_checkin_per_member_day ON attendance(member_id, date(check_in_time));
  ```
- **BR-ATT-003 (Session Decrement):** Every valid check-in on a `SESSION_BASED` plan decrements `remaining_sessions` by 1 within the same atomic database transaction.

### 2.4 Payroll & Accounting Rules (BR-FIN)
- **BR-FIN-001 (Monthly Payroll Uniqueness):** An employee or coach can receive only **one base salary payment per calendar month** (`pay_month` e.g., `"2026-10"`). Duplicate disbursement transactions are rejected by database constraint:
  ```sql
  UNIQUE(employee_type, employee_id, pay_month)
  ```
- **BR-FIN-002 (Financial Reconciliation Formula):**
  $$\text{Gross Revenue} = \sum \text{Paid Payments}$$
  $$\text{Total Expenses} = \sum \text{Operating Expenses}$$
  $$\text{Total Payroll} = \sum \text{Paid Payroll Disbursements}$$
  $$\text{Net Operating Profit} = \text{Gross Revenue} - (\text{Total Expenses} + \text{Total Payroll})$$
- **BR-FIN-003 (Immutability of Historical Transactions):** Once a payment or payroll record is marked `PAID`, it cannot be altered without an administrative audit log.

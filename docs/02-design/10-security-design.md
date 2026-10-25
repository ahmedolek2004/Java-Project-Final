# 10. Security & Access Control Design

> **Document ID:** DOC-SEC-010  
> **Security Baseline:** OWASP Desktop Application Security Standards  
> **Target System:** Forge Fitness & Martial Arts Management System  
> **Status:** Active & Implemented

---

## 1. Threat Modeling & Threat Landscape (STRIDE Analysis)

| Threat Category | Potential Risk in Gym Environment | Applied Mitigation in Forge Fitness |
| :--- | :--- | :--- |
| **Spoofing** | Receptionist pretending to be Admin to view revenues | Distinct user sessions; PBKDF2 credential verification |
| **Tampering** | Modifying SQLite database files directly | Database path stored in protected user profile (`~/.gym-system`); SQLite file permissions |
| **Repudiation** | Receptionist denying having registered an unpaid member | Payment transactions permanently linked to member and subscription IDs with timestamps |
| **Information Disclosure** | Unauthorized staff viewing salaries and club profits | Strict Role-Based Access Control (RBAC); UI routes hidden for `RECEPTIONIST` |
| **Denial of Service** | Corrupt database file crashing reception during morning rush | Automated 1-click verified hot backups; SQLite busy timeout configuration |
| **Elevation of Privilege** | Receptionist crafting malicious SQL queries in search box | 100% Parameterized queries via `PreparedStatement` (Zero SQL injection) |

---

## 2. Authentication & Cryptographic Architecture

### 2.1 Password Hashing Specification (`PasswordService.java`)
Plaintext passwords are never persisted to disk or kept in heap memory longer than required for authentication.
- **Algorithm:** PBKDF2 with HMAC-SHA256 (`PBKDF2WithHmacSHA256`)
- **Key Derivation Iterations:** 65,536 iterations (computationally prohibitive against offline GPU dictionary attacks).
- **Salt Generation:** 16 bytes (128 bits) of cryptographically secure pseudo-random bytes generated via `java.security.SecureRandom`.
- **Derived Key Length:** 256 bits (32 bytes).
- **Storage Format:** Stored in `users.password_hash` as a self-contained string:
  ```
  <salt_hex>$<hash_hex>
  ```
- **Constant-Time Verification:** Hash comparison uses constant-time byte-array comparison to prevent timing attack side-channels.

---

## 3. Role-Based Access Control (RBAC) Specification

The system recognizes two distinct system roles:

```
┌─────────────────────────────────────────────────────────────┐
│                      ADMINISTRATOR                          │
│  Full Access: Dashboard, Members, Subscriptions,            │
│  Attendance, Class Schedule, Coaches, Activities,           │
│  Finance (Revenues & Expenses), Payroll, Team & Settings    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼ Super-set of
┌─────────────────────────────────────────────────────────────┐
│                      RECEPTIONIST                           │
│  Operational Access Only:                                   │
│  - Members (Register, Edit, Search, Print QR Card)          │
│  - Subscriptions (Sell, Renew, Freeze, Unfreeze)            │
│  - Attendance (Scan QR, Manual Check-In, Daily Log)         │
│  - Class Schedule (Read-Only timetable view)                │
│                                                             │
│  RESTRICTED (Hidden & Blocked):                             │
│  ✗ Financial Reports & Profit Margins                       │
│  ✗ Expense Logging                                          │
│  ✗ Staff & Coach Payroll Management                         │
│  ✗ User Account & Password Provisioning                     │
│  ✗ Database Direct Restore Operations                       │
└─────────────────────────────────────────────────────────────┘
```

### 3.1 Enforcement Mechanism (`NavigationHelper.java`)
- Upon login, the active `User` model is stored in the application session context.
- During navigation bar construction, `NavigationHelper` filters menu items:
  ```java
  if (currentUser.getRole() == Role.RECEPTIONIST) {
      navItems.removeIf(item -> item.requiresAdminRole());
  }
  ```
- Any unauthorized direct view invocation triggers an immediate access-denied security alert.

---

## 4. Data Protection & Backup Security

1. **Local File System Security:** The SQLite database is created in the operating system's user home directory (`~/.gym-system/gym_system.db`), isolating it from other non-administrative OS user accounts.
2. **Safe Backup Procedure:** The hot backup helper performs an orderly file copy, ensuring SQLite transactions are committed before copying bytes to the destination file.
3. **Integrity Verification:** During restore operations, the file's first 16 bytes are validated against the standard SQLite magic string (`SQLite format 3\000`) before overwriting operational data.

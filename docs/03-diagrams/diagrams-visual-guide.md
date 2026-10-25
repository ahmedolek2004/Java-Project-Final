# 03. Architecture & Engineering Diagrams Visual Guide

> **Standard Compliance:** C4 Model & OMG UML 2.5  
> **Source Directory:** `docs/03-diagrams/`  
> **Target System:** Forge Fitness & Martial Arts Management System  
> **Status:** All 15 Diagrams Verified & Aligned with Codebase

---

## Table of Diagrams

| # | Diagram Name | Category | File Reference | Primary Focus |
| :- | :--- | :--- | :--- | :--- |
| **01** | System Context Diagram | C4 Level 1 | `01-system-context.mmd` | System boundaries, users, and external peripherals |
| **02** | Container Diagram | C4 Level 2 | `02-container.mmd` | Desktop container, SQLite, filesystem, and OS |
| **03** | Component Diagram | C4 Level 3 | `03-component.mmd` | Internal pages, services, DAOs, and interactions |
| **04** | Package Diagram | Architecture | `04-package.mmd` | Java package modularity and dependencies |
| **05** | UML Use Case Diagram | Requirements | `05-use-case.mmd` | Admin vs Receptionist functional interactions |
| **06** | Subscription Activity Diagram | UML Behavioral | `06-subscription-activity.mmd` | Workflow of plan selection, discount, and payment |
| **07** | Attendance Sequence Diagram | UML Behavioral | `07-attendance-sequence.mmd` | Message flow for rapid QR check-in & verification |
| **08** | Subscription State Machine | UML Behavioral | `08-subscription-state-machine.mmd` | Status transitions: Active, Frozen, Expired |
| **09** | Entity-Relationship Diagram | Data Model | `09-erd.mmd` | Complete SQLite tables, columns, keys, and cardinality |
| **10** | Domain & Service Class Diagram | UML Structural | `10-class-diagram.mmd` | OO hierarchy: User, Member, Subscription, Services |
| **11** | Runtime Object Diagram | UML Structural | `11-object-diagram.mmd` | Memory instance snapshot during a check-in event |
| **12** | Physical Deployment Diagram | Operations | `12-deployment.mmd` | Physical front-desk PC, USB ports, and peripherals |
| **13** | Backup & Recovery Flowchart | Operations | `13-backup-recovery-flow.mmd` | Flowchart for verified hot backups and restores |
| **14** | Build & Release Pipeline | DevOps / CI | `14-build-pipeline.mmd` | Maven compile, JUnit 5 test, and JPackage installer |
| **15** | Dependency Diagram | Configuration | `15-dependency-diagram.mmd` | External libraries (OpenJFX, SQLite JDBC, ZXing) |

---

## 1. System Context Diagram (C4 Level 1)
```mermaid
flowchart TB
    Admin["Gym Administrator / Owner\n[Person]"]
    Receptionist["Front-Desk Receptionist\n[Person]"]
    Member["Gym Member / Trainee\n[Person]"]

    System["Forge Fitness Management System\n[Software System: Desktop Java/JavaFX]\nManages members, plans, attendance, financials, and class schedules"]

    Scanner["USB Barcode / QR Scanner\n[External Hardware HID Device]"]
    Printer["Desktop Printer\n[External Hardware Device]"]
    Storage["Local Filesystem / SQLite DB\n[Embedded File: ~/.gym-system/gym_system.db]"]

    Admin -->|"Manages users, views financials, disburses payroll, configures system"| System
    Receptionist -->|"Registers members, sells subscriptions, initiates check-in"| System
    Member -->|"Presents QR card for facility access"| Scanner

    Scanner -->|"Emulates keyboard input (Member ID + Enter)"| System
    System -->|"Sends print jobs (QR Cards, Invoices, Reports)"| Printer
    System <-->|"Reads and writes transactional data via JDBC"| Storage
```

---

## 2. Container Diagram (C4 Level 2)
```mermaid
flowchart TB
    subgraph ForgeDesktop["Forge Fitness Desktop Application [Java 25 / OpenJFX 21]"]
        Launcher["Launcher & Composition Root\n(Launcher.java, GymApplication.java)"]
        UIContainer["UI Component Container\n(FXML Controllers, Page Builders, Helpers)"]
        ServiceContainer["Core Domain Services\n(MemberService, SubscriptionService, AttendanceService, etc.)"]
        DAOContainer["Persistence & Data Access\n(GymRepository, ClubRepository, DatabaseConnection)"]
    end

    subgraph LocalOS["Operating System Environment (Windows / Linux / macOS)"]
        SQLiteDB[("SQLite Embedded Database\nFile: ~/.gym-system/gym_system.db\nACID compliant")]
        Backups[("Backup Storage Folder\nVerified .db Archive")]
        Exports["Export Directory\nUTF-8 BOM CSV / Excel"]
    end

    subgraph Peripherals["Physical Peripherals"]
        Scanner["USB HID QR Scanner"]
        Printer["System Printer Service"]
    end

    Scanner -->|"Keyboard Strokes"| UIContainer
    Launcher --> UIContainer
    UIContainer -->|"Calls methods via PageContext"| ServiceContainer
    ServiceContainer -->|"Domain Operations"| DAOContainer
    DAOContainer <-->|"JDBC SQL Queries"| SQLiteDB
    UIContainer -->|"Triggers Document Print"| Printer
    ServiceContainer -->|"1-Click Hot Backup"| Backups
    ServiceContainer -->|"Generates CSV Reports"| Exports
```

---

## 3. Attendance Sequence Diagram (UML Sequence)
```mermaid
sequenceDiagram
    autonumber
    actor Member
    actor Receptionist
    participant Scanner as USB QR Scanner (HID)
    participant UI as AttendancePage (JavaFX)
    participant Svc as AttendanceService
    participant Repo as GymRepository
    participant DB as SQLite (gym_system.db)

    Member->>Scanner: Presents QR Card
    Scanner->>UI: Keystrokes (Member ID + Enter)
    UI->>Svc: processCheckIn(rawIdentifier)
    
    Svc->>Repo: findMemberById(id)
    Repo->>DB: SELECT * FROM members WHERE id = ?
    DB-->>Repo: Member Record
    Repo-->>Svc: Member Entity

    Svc->>Repo: hasCheckedInToday(memberId, currentDate)
    Repo->>DB: SELECT COUNT(*) FROM attendance WHERE member_id = ? AND date(check_in_time) = date('now')
    DB-->>Repo: Count
    Repo-->>Svc: boolean (alreadyCheckedIn)

    alt Already Checked In Today
        Svc-->>UI: CheckInResult(REJECTED: ALREADY_CHECKED_IN_TODAY)
        UI-->>Receptionist: Play Warning Buzz + Red Banner "Already Checked In"
    else Not Yet Checked In
        Svc->>Repo: getActiveSubscription(memberId)
        Repo->>DB: SELECT * FROM subscriptions WHERE member_id = ? AND status = 'ACTIVE'
        DB-->>Repo: Subscription Record
        Repo-->>Svc: Subscription Entity

        alt No Active Plan or Plan Expired
            Svc-->>UI: CheckInResult(REJECTED: EXPIRED_OR_FROZEN)
            UI-->>Receptionist: Play Error Buzz + Red Banner "No Active Plan"
        else Valid Active Plan
            opt Plan is SESSION_BASED
                Svc->>Repo: decrementRemainingSessions(subId)
                Repo->>DB: UPDATE subscriptions SET remaining_sessions = remaining_sessions - 1
            end
            Svc->>Repo: insertAttendance(memberId, subId, timestamp)
            Repo->>DB: INSERT INTO attendance (member_id, subscription_id, check_in_time) VALUES (?, ?, CURRENT_TIMESTAMP)
            DB-->>Repo: Success
            Svc-->>UI: CheckInResult(APPROVED, Member, RemainingSessions)
            UI-->>Receptionist: Play Green Chime + Display Member Details & Sessions Left
        end
    end
```

---

## 4. Entity-Relationship Diagram (ERD)
```mermaid
erDiagram
    users {
        INTEGER id PK
        TEXT username UK
        TEXT password_hash
        TEXT full_name
        TEXT role
        TEXT phone
        REAL salary
        INTEGER active
        TEXT created_at
    }

    activities {
        INTEGER id PK
        TEXT name_ar
        TEXT name_en
        TEXT description
    }

    coaches {
        INTEGER id PK
        TEXT name
        TEXT phone
        INTEGER activity_id FK
        REAL salary
        INTEGER is_personal_trainer
    }

    members {
        INTEGER id PK
        TEXT full_name
        TEXT phone UK
        TEXT email
        TEXT gender
        TEXT medical_notes
        TEXT created_at
    }

    subscriptions {
        INTEGER id PK
        INTEGER member_id FK
        TEXT plan_name
        TEXT plan_type
        INTEGER duration_months
        INTEGER total_sessions
        INTEGER remaining_sessions
        TEXT start_date
        TEXT end_date
        REAL price
        REAL discount_applied
        TEXT status
        INTEGER activity_id FK
    }

    attendance {
        INTEGER id PK
        INTEGER member_id FK
        INTEGER subscription_id FK
        TEXT check_in_time
    }

    payments {
        INTEGER id PK
        INTEGER member_id FK
        INTEGER subscription_id FK
        TEXT description
        REAL amount
        TEXT payment_date
        TEXT status
    }

    expenses {
        INTEGER id PK
        TEXT description
        REAL amount
        TEXT expense_date
    }

    payroll_payments {
        INTEGER id PK
        TEXT employee_type
        INTEGER employee_id
        TEXT employee_name
        TEXT pay_month
        REAL amount
        TEXT paid_at
    }

    class_schedule {
        INTEGER id PK
        TEXT title
        INTEGER activity_id FK
        INTEGER coach_id FK
        INTEGER day_of_week
        TEXT start_time
        TEXT end_time
        INTEGER capacity
        INTEGER active
    }

    members ||--o{ subscriptions : "purchases"
    members ||--o{ attendance : "logs"
    members ||--o{ payments : "makes"
    subscriptions ||--o{ attendance : "validates"
    subscriptions ||--o{ payments : "generates"
    activities ||--o{ coaches : "specializes in"
    activities ||--o{ subscriptions : "applies to"
    activities ||--o{ class_schedule : "schedules"
    coaches ||--o{ class_schedule : "instructs"
```

---

## 5. Subscription Lifecycle State Machine
```mermaid
stateDiagram-v2
    [*] --> ACTIVE : Subscription Sold / Created

    state ACTIVE {
        [*] --> InGoodStanding
        InGoodStanding --> SessionDecremented : Check-in Event
        SessionDecremented --> InGoodStanding : Sessions > 0
    }

    ACTIVE --> FROZEN : Receptionist triggers Freeze\n(Halts entry rights, preserves balance)
    FROZEN --> ACTIVE : Receptionist triggers Unfreeze\n(Restores entry rights)

    ACTIVE --> EXPIRED : Calendar End Date Reached (TIME_BASED)\nOR Remaining Sessions == 0 (SESSION_BASED)
    FROZEN --> EXPIRED : Administrative Expiration
    
    EXPIRED --> ACTIVE : Plan Renewed / Re-purchased
    EXPIRED --> [*] : Terminated / Inactive
```

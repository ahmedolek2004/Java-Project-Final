# 07. Software Architecture Document (SAD)

> **Document ID:** DOC-ARCH-007  
> **Standard:** IEEE 42010 / C4 Model  
> **Target System:** Forge Fitness & Martial Arts Management System  
> **Architecture Pattern:** Clean Layered Architecture (Offline Desktop)  
> **Version:** 1.0.0

---

## 1. Architectural Drivers & Principles

The architectural design of Forge Fitness is governed by four core architectural drivers:
1. **Zero Operational Latency:** Check-in and search operations must be instantaneous without network serialization delays.
2. **Total Offline Independence:** The application must function 100% autonomously without requiring an active internet connection.
3. **Decoupled Business Logic:** UI components must never execute direct SQL queries; all operations flow through strongly-typed domain services.
4. **Resilient Persistence:** Embedded SQLite with foreign keys enabled, WAL mode support, and strict transaction rollback guarantees.

---

## 2. Layered Architecture Overview

The system strictly enforces a unidirectional top-to-bottom dependency hierarchy:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   (FXML Views, Modular Page Builders, UI Factories)    │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
┌───────────────────────────▼────────────────────────────┐
│                  Business Service Layer                │
│    (MemberService, SubscriptionService, Finance, etc.) │
└───────────────────────────┬────────────────────────────┘
                            │ depends on
┌───────────────────────────▼────────────────────────────┐
│                 Persistence / DAO Layer                │
│ (GymRepository, ClubRepository, DatabaseConnection)    │
└───────────────────────────┬────────────────────────────┘
                            │ persists to
┌───────────────────────────▼────────────────────────────┐
│                    SQLite Database                     │
│                (~/.gym-system/gym_system.db)           │
└────────────────────────────────────────────────────────┘
```

---

## 3. Package Responsibilities & Boundaries

### 3.1 Presentation Layer (`com.gym.system.controller`)
- **Composition Root (`GymApplication.java`):** Initializes services, repositories, and mounts the primary UI stage.
- **Top-Level Controllers:** `DashboardController.java` manages the main navigation shell, top bar, and active view switching.
- **Modular Page Builders (`com.gym.system.controller.page`):** Each screen is self-contained (e.g. `AttendancePage.java`, `MembersPage.java`, `FinancePage.java`), receiving dependencies via `PageContext.java`.
- **UI Component Factories (`com.gym.system.controller.helper`):** Standardizes UI generation via `TableFactory.java` and `MetricCardFactory.java`, preventing UI code duplication.

### 3.2 Service Layer (`com.gym.system.service`)
- Enforces domain invariants, calculation rules, discount limits, and status state transitions.
- Acts as transaction orchestrator (e.g., creating a subscription and recording its initial payment within one atomic unit of work).
- Completely decoupled from JavaFX UI controls (zero `javafx.scene.*` dependencies in services, ensuring 100% headless testability).

### 3.3 Data Access Layer (`com.gym.system.dao`)
- **`DatabaseConnection.java`:** Provides pooled connections with `PRAGMA foreign_keys = ON` and `PRAGMA busy_timeout = 5000`.
- **`GymRepository.java` & `ClubRepository.java`:** Encapsulates raw JDBC `PreparedStatement` executions, parameter binding, and `ResultSet` mapping into domain models.
- **`UserRepository.java`:** Manages credential validation, PBKDF2 hashing with salt, and user account provisioning.

### 3.4 Domain Model Layer (`com.gym.system.model`)
- Pure Java objects representing domain concepts: `Member`, `Subscription`, `Activity`, `Coach`, `ScheduledClass`, `User`, `Admin`, `Receptionist`.
- Immutable or encapsulated state with business validation helpers.

---

## 4. Cross-Cutting Concerns

### 4.1 Internationalization & RTL Management
Handled by `LanguageManager.java` and `AppearanceHelper.java`. When switching language:
1. `ResourceBundle` is reloaded (`messages_ar.properties` or `messages_en.properties`).
2. Root scene `nodeOrientation` is set to `NodeOrientation.RIGHT_TO_LEFT` or `LEFT_TO_RIGHT`.
3. Scene stylesheet is updated with appropriate font stacks (Cairo/Almarai for Arabic, Inter/Segoe UI for English).

### 4.2 Security & Authentication
Handled by `PasswordService.java` using **PBKDF2WithHmacSHA256** with 65,536 iterations and cryptographically random 16-byte salts. Plaintext passwords never touch persistence.

### 4.3 Database Concurrency & File Locking
SQLite handles single-writer multiple-reader concurrency. The application sets `busy_timeout = 5000` ms to allow background threads or concurrent UI actions to wait gracefully rather than throwing immediate database locked exceptions.

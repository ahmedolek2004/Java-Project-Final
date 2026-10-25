# FORGE FITNESS & MARTIAL ARTS MANAGEMENT SYSTEM
## Engineering Documentation Hub & Master Index

> **Standard Compliance:** ISO/IEC/IEEE 29148 (Requirements Engineering) | C4 Model & UML 2.5 (Architecture & Design) | Clean Architecture
> **Current Version:** v1.0.0-SNAPSHOT
> **Last Verified Against Codebase:** 2026-10-25

---

### 📚 Structure Overview

This documentation suite establishes a verified, single-source-of-truth technical blueprint for the **Forge Fitness & Martial Arts Management System**. Every requirement, architectural choice, data schema, and workflow documented here is strictly cross-referenced against the active Java 25 / JavaFX 21 codebase.

```
docs/
├── README.md (This Master Index)
│
├── 00-audit/
│   └── 00-repository-audit.md           # Evidence-based code, schema, and dependency audit
│
├── 01-product/                          # Phase 1: Product Definition & Requirements
│   ├── 01-project-charter.md            # Vision, business justification, objectives & constraints
│   ├── 02-prd.md                        # Product Requirements Document (features, personas, metrics)
│   ├── 03-srs.md                        # Software Requirements Specification (functional/non-functional)
│   ├── 04-users-and-stories.md          # Personas, User Stories, and Acceptance Criteria (Gherkin)
│   ├── 05-business-rules.md             # Invariant domain rules, calculations & domain glossary
│   └── 06-scope-and-roadmap.md          # MVP baseline, phased releases, and out-of-scope boundaries
│
├── 02-design/                           # Phase 2: System Design & Architecture
│   ├── 07-architecture.md               # Software Architecture Document (SAD), layers, responsibilities
│   ├── 08-database-design.md            # SQLite schema, tables, constraints, indexes & migration rules
│   ├── 09-ui-ux-spec.md                 # Design system, themes (Dark/Light), RTL Arabic typography, screens
│   ├── 10-security-design.md            # RBAC model, PBKDF2 cryptography, session security, threat mitigation
│   ├── 11-detailed-design.md            # Interface contracts, service signatures, transaction patterns
│   └── adr/                             # Architecture Decision Records
│       ├── ADR-001-embedded-sqlite-storage.md
│       ├── ADR-002-layered-desktop-architecture.md
│       ├── ADR-003-hybrid-qr-scanning.md
│       └── ADR-004-pbkdf2-credential-security.md
│
├── 03-diagrams/                         # Visual & Textual Diagrams (Mermaid format)
│   ├── diagrams-visual-guide.md         # Embedded Markdown previews & explanatory notes
│   ├── 01-system-context.mmd            # C4 Level 1: System Context Diagram
│   ├── 02-container.mmd                 # C4 Level 2: Container Diagram
│   ├── 03-component.mmd                 # C4 Level 3: Component Diagram
│   ├── 04-package.mmd                   # Java Package Dependency Diagram
│   ├── 05-use-case.mmd                  # UML Use Case Diagram (Admin vs. Receptionist)
│   ├── 06-subscription-activity.mmd     # UML Activity Diagram: Subscription Lifecycle
│   ├── 07-attendance-sequence.mmd       # UML Sequence Diagram: Fast QR Check-in
│   ├── 08-subscription-state-machine.mmd# UML State Machine: Subscription Statuses
│   ├── 09-erd.mmd                       # Entity-Relationship Diagram (Crow's Foot)
│   ├── 10-class-diagram.mmd             # Domain & Service Class Diagram
│   ├── 11-object-diagram.mmd            # Runtime Object Snapshot
│   ├── 12-deployment.mmd                # Physical Deployment Architecture
│   ├── 13-backup-recovery-flow.mmd      # Backup & Restore Operational Flowchart
│   ├── 14-build-pipeline.mmd            # Maven / JPackage Build & Release Pipeline
│   └── 15-dependency-diagram.mmd        # External Library & Runtime Dependencies
│
├── 04-quality/                          # Phase 3: Verification, Validation & Risk
│   ├── 13-test-plan.md                  # Test Strategy, Unit/Integration/UI Test Plan
│   ├── 14-requirements-traceability.csv # Traceability Matrix (Req ID ↔ Code ↔ Test)
│   └── 15-bugs-and-risks.md             # Defect Log, Risk Register & Mitigation
│
└── 05-release/                          # Phase 4: Operations, Manuals & Maintenance
    ├── 16-build-and-release.md          # Compilation, testing, JPackage packaging & installation
    ├── 17-user-manual.md                # Day-to-day receptionist & operational manual
    ├── 18-admin-guide.md                # Administrator manual, system configuration & backups
    ├── 19-localization.md               # I18n Resource bundles, Arabic RTL rules, formatting
    ├── 20-maintenance.md                # Maintenance procedures, SQLite vacuum, data recovery
    └── CHANGELOG.md                     # Release history & breaking changes log
```

---

### 🎯 Documentation Verification Matrix

| Document Category | Target Audience | Primary Focus | Code Alignment Status |
| :--- | :--- | :--- | :--- |
| **01-product/** | Product Owner, Stakeholders, Devs | What & Why | Verified against implemented models & services |
| **02-design/** | Architects, Senior Engineers | How & Where | Verified against `dao/`, `service/`, `controller/` |
| **03-diagrams/** | All Engineering & Review Teams | Visual Structure & Workflows | Validated Mermaid syntax matching real schema |
| **04-quality/** | QA Leads, Developers | Correctness & Verification | Verified against existing JUnit 5 test suites |
| **05-release/** | Operators, Admins, End Users | Deployment & Daily Use | Verified against `Launcher.java` and `pom.xml` |

---

### 🛠️ Maintenance & Evolution Rules

1. **No Phantom Features:** If a capability is not present in code, it is clearly tagged with `[PROPOSED]` or `[ROADMAP]`.
2. **Schema Invariants:** Database design changes must first be documented in `08-database-design.md` and an ADR recorded in `adr/` before touching Java/SQL code.
3. **Traceability:** Any new functional requirement added to `03-srs.md` must be assigned a unique ID (`REQ-XXX`) and tracked in `14-requirements-traceability.csv`.

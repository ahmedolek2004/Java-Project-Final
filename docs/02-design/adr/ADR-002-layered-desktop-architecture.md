# ADR-002: Modular Clean Layered Desktop Architecture

> **Status:** Accepted  
> **Date:** 2026-10-25  
> **Decision Maker:** System Architect

---

## Context & Problem Statement
Early prototypes merged SQL queries, JavaFX FXML event handlers, and business logic into monolithic controller classes, resulting in fragile code that could not be unit tested without launching a full JavaFX UI thread.

## Decision
Refactor and enforce a strict **Layered Architecture** adhering to Clean Architecture principles:
1. **`com.gym.system.controller.page`:** Self-contained UI page builders responsible solely for rendering, event capture, and dispatching to services via `PageContext`.
2. **`com.gym.system.service`:** Plain Java domain services carrying business logic and invariants with zero JavaFX UI dependencies.
3. **`com.gym.system.dao`:** Strongly typed data access classes handling JDBC statements and result-set mapping.
4. **`com.gym.system.model`:** Lightweight domain models.

## Consequences
- **Positive:** Business services and calculation logic can be unit-tested headlessly in CI/CD without JavaFX toolkit initialization; UI screens can be redesigned or themed without altering database transactions.
- **Negative:** Requires disciplined passing of `PageContext` and additional mapping code between layers.

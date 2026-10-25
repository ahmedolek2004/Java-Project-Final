# ADR-001: Embedded SQLite Storage Engine

> **Status:** Accepted  
> **Date:** 2026-10-25  
> **Decision Maker:** System Architect

---

## Context & Problem Statement
Fitness centers require an exceptionally fast and resilient storage engine that functions with zero system administration overhead. If a gym's internet connection fails, check-in turnstiles and front-desk registers cannot halt. The system must operate reliably on a single desktop computer without requiring users to install or configure separate database server daemons (such as PostgreSQL, MySQL, or SQL Server).

## Considered Alternatives
1. **Cloud SaaS (PostgreSQL / MySQL on AWS/Heroku):** Rejected due to strict offline independence requirements and ongoing monthly hosting fees for the club owner.
2. **Local Client-Server (Local MySQL/PostgreSQL):** Rejected due to complex installation wizards, port conflicts, service crash liabilities, and maintenance burden on non-technical gym staff.
3. **Embedded SQLite (`org.xerial:sqlite-jdbc`):** Selected.

## Decision
Adopt **SQLite** as the primary storage engine via the official `org.xerial:sqlite-jdbc` library, configuring it with:
- Dedicated user directory storage (`~/.gym-system/gym_system.db`).
- `PRAGMA foreign_keys = ON;` to enforce strict relational integrity.
- `PRAGMA busy_timeout = 5000;` to eliminate locking collisions during concurrent UI updates.

## Consequences
- **Positive:** Zero installation wizard; single-file database enables trivial 1-click backups and restores; extreme read speed; 100% offline uptime.
- **Negative:** Concurrency is limited to single-writer; however, for a front-desk desktop station processing 1–2 check-ins per second, SQLite's write throughput exceeds requirements by orders of magnitude.

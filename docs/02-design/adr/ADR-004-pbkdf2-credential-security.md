# ADR-004: PBKDF2 Password Cryptography & Salt Derivation

> **Status:** Accepted  
> **Date:** 2026-10-25  
> **Decision Maker:** Security Lead & System Architect

---

## Context & Problem Statement
Legacy desktop applications often store credentials in plaintext, simple base64, or obsolete MD5/SHA-1 hashes vulnerable to immediate rainbow-table lookups. Even for an offline system, database backups may be exported or transferred, requiring industrial-grade protection for administrative and staff credentials.

## Decision
Implement cryptographic key derivation via standard Java Cryptography Architecture (JCA):
- **Algorithm:** `PBKDF2WithHmacSHA256`
- **Work Factor (Iterations):** 65,536
- **Salt:** 16 bytes generated per-user via `SecureRandom`
- **Hash Output:** 256 bits (32 bytes)
- **Comparison:** Constant-time `MessageDigest.isEqual()` to prevent side-channel timing attacks.

## Consequences
- **Positive:** Zero third-party native C dependencies (unlike BCrypt/Argon2 which require native DLLs that complicate cross-platform packaging); robust protection against GPU/ASIC brute forcing.
- **Negative:** Negligible CPU cost on login (~50ms), entirely imperceptible to human operators.

# ADR-003: Hybrid USB HID & Manual Check-in Strategy

> **Status:** Accepted  
> **Date:** 2026-10-25  
> **Decision Maker:** System Architect

---

## Context & Problem Statement
Fast member entry during peak hours requires automated barcode/QR scanning. However, front desks use a variety of hardware scanners (USB wired, 2.4GHz wireless dongles, or Bluetooth). Some gyms also face scenarios where a member forgot their card or phone.

## Decision
Adopt a **Hybrid Check-in Strategy**:
1. Treat USB QR/Barcode scanners as standard **Human Interface Devices (HID)** that emulate keyboard input followed by an `ENTER` key stroke.
2. Maintain auto-focus on the scanner input field within `AttendancePage.java`.
3. Support manual entry: if a member does not have their card, the receptionist can type the member's ID or phone number into the same input field or use the manual member lookup dialog (`CheckInForm.java`).
4. Generate QR codes using `com.google.zxing:core:3.5.3` encoding the member ID directly for rapid decoding.

## Consequences
- **Positive:** Zero specialized proprietary drivers needed (works out of the box on Windows, macOS, and Linux with any plug-and-play USB scanner); graceful fallback for forgotten cards.
- **Negative:** If another desktop application steals window focus, the scanner input might be lost. (Mitigated by automatic window focus management and prominent status indicator).

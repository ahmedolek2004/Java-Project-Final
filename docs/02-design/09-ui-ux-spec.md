# 09. UI/UX Specification & Design System

> **Document ID:** DOC-DESIGN-009  
> **UI Technology:** JavaFX 21 + FXML + Modular CSS  
> **Target Displays:** Desktop (1920x1080 Full HD & 1366x768 Standard HD)  
> **Status:** Active & Implemented

---

## 1. Visual Design Philosophy & Design Tokens

Forge Fitness is engineered with a **Pro-tier modern aesthetic** specifically crafted for fitness centers and martial arts clubs. Front-desk environments often feature subdued ambient lighting; hence, the UI offers deep, low-eye-strain dark modes paired with crisp high-contrast accents, alongside a clean light mode for bright administrative offices.

### 1.1 Color Tokens

| Token Name | Dark Mode Value | Light Mode Value | Purpose |
| :--- | :--- | :--- | :--- |
| `--bg-main` | `#121418` | `#F8F9FA` | Main application background |
| `--bg-surface` | `#1E222B` | `#FFFFFF` | Cards, tables, modal dialog surfaces |
| `--border-subtle` | `#2D323F` | `#E2E8F0` | Dividers, card borders, table grid lines |
| `--accent-primary` | `#FF5722` (Flame Orange) | `#EA580C` | Primary actions, check-in buttons, active states |
| `--accent-secondary`| `#00B0FF` (Electric Cyan) | `#0284C7` | Informational badges, secondary buttons |
| `--success` | `#00E676` (Neon Green) | `#16A34A` | Valid check-in, active subscription status |
| `--danger` | `#FF1744` (Vibrant Red) | `#DC2626` | Expired plans, rejection dialogs, delete actions |
| `--warning` | `#FFD600` (Gold Amber) | `#CA8A04` | Frozen status, expiring soon warnings |

---

## 2. Bilingual Typography & RTL Adaptation Rules

### 2.1 Font Hierarchy
- **Arabic Mode:** `Cairo`, `Almarai`, `Segoe UI`, `System` fallback. Provides high readability for Arabic diacritics and bold gym headlines.
- **English Mode:** `Inter`, `Segoe UI`, `Roboto`, `Helvetica Neue`. Clean geometric sans-serif for numbers, IDs, and financial tables.

### 2.2 Right-To-Left (RTL) Layout Rules
The system dynamically toggles `NodeOrientation.RIGHT_TO_LEFT` on the root scene graph when Arabic is activated:
1. **Sidebar Navigation:** Automatically shifts from the left edge to the right edge.
2. **Table Columns:** Text-aligned to the right; numeric and currency columns align with RTL standard reading order.
3. **Form Fields:** Labels placed right-aligned above text inputs with matching right-aligned placeholders.
4. **Icons & Chevrons:** Directional arrows (`←` vs `→`) mirror automatically to preserve spatial intuition.

---

## 3. Screen Inventory & Layout Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│ Top Bar: Logo | Active Page Title | Quick Search | Lang/Theme | User   │
├─────────────────┬──────────────────────────────────────────────────────┤
│ Navigation Bar  │ Main Content Viewport                                │
│ (Dashboard,     │                                                      │
│  Members,       │  [ Metric Cards: Active Members, Today Check-ins ]   │
│  Subscriptions, │                                                      │
│  Attendance,    │  [ Search / Action Toolbar ]                         │
│  Schedule,      │                                                      │
│  Coaches,       │  [ Standardized Responsive TableView with Pagination]│
│  Finance,       │                                                      │
│  Payroll,       │                                                      │
│  Settings)      │                                                      │
└─────────────────┴──────────────────────────────────────────────────────┘
```

### 3.1 Primary Screen Details

#### 1. Attendance & Check-In Screen (`AttendancePage.java`)
- **Focal Element:** Auto-focused input field configured to capture USB scanner keystrokes and `ENTER` signals.
- **Large Verification Banner:** Displays full-width green/red verification feedback with the member's photo/name, subscription status, and remaining sessions in 32pt bold font for instant visibility across the reception desk.
- **Audible Chimes:** Short high-frequency chime on approved check-in; low-frequency alert buzz on expired/blocked check-in.

#### 2. Members Management Screen (`MembersPage.java`)
- Table with columns: ID, Full Name, Phone, Gender, Subscriptions Count, Actions.
- Real-time search bar filtering across name and phone without requiring explicit submit clicks.
- Action buttons: "Edit Profile", "New Subscription", "Print QR Card".

#### 3. Subscriptions Management Screen (`SubscriptionsPage.java`)
- Filter tabs: `All`, `Active`, `Expiring This Week`, `Frozen`, `Expired`.
- One-click "Freeze" / "Unfreeze" action with duration calculation.
- Plan renewal modal pre-populating member details.

#### 4. Financial Audit & Payroll Screens (`FinancePage.java`, `PayrollPage.java`)
- KPI Summary tiles: Total Revenue, Total Expenses, Disbursed Payroll, Net Operating Margin.
- Monthly payroll disbursement matrix displaying employee name, position, salary, status (`PAID` / `UNPAID`), and one-click "Disburse" action.

---

## 4. UI Component Standardization & Factories

To maintain 100% UI consistency across all screens:
- **`TableFactory.java`:** Builds standardized JavaFX `TableView` components with sortable columns, consistent row height, and zebra striping.
- **`MetricCardFactory.java`:** Produces polished KPI tiles featuring icon, title, formatted numerical value, and secondary trend badge.
- **`FormDialog.java`:** Generates uniform modal dialogs with validation styling (red border on invalid fields) and localized OK/Cancel buttons.

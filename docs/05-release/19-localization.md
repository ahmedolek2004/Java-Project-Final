# 19. Localization & Translation Guide (دليل التعريب واللغات)

> **Document ID:** DOC-OPS-019  
> **Supported Languages:** Arabic (`ar_EG` - Primary RTL) & English (`en_US` - LTR)  
> **Architecture:** Java standard `ResourceBundle` + JavaFX Dynamic RTL Reorientation

---

## 1. Resource Bundle Architecture

Localization strings are managed in UTF-8 `.properties` files located in:
```
GYM/src/main/resources/com/gym/system/i18n/
├── messages_ar.properties    # Complete Arabic translations
└── messages_en.properties    # Complete English translations
```

### 1.1 Managing Translation Keys (`LanguageManager.java`)
All UI labels, button text, table headers, and error dialogs reference static keys via `LanguageManager.getString("key")`.

Example translation entry:
```properties
# Navigation Keys
nav.dashboard=لوحة التحكم / Dashboard
nav.members=الأعضاء / Members
nav.attendance=تسجيل الحضور / Attendance
nav.subscriptions=الاشتراكات / Subscriptions
nav.schedule=جدول التدريب / Class Schedule
nav.finance=المالية والمصروفات / Finance
nav.payroll=الرواتب / Payroll

# Business Messages
msg.checkin.success=تم تسجيل الدخول بنجاح! / Check-in approved successfully!
msg.checkin.expired=عفواً، الاشتراك منتهي الصلاحية! / Subscription has expired!
msg.checkin.already=تم تسجيل الحضور اليوم بالفعل! / Member already checked in today!
```

---

## 2. Right-To-Left (RTL) Layout Implementation

JavaFX natively supports RTL node reorientation. In Forge Fitness, the scene graph dynamically reacts to language switching:

```java
// AppearanceHelper.java
public static void applyLanguage(Scene scene, Locale locale) {
    if ("ar".equalsIgnoreCase(locale.getLanguage())) {
        scene.getRoot().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        scene.getRoot().setStyle("-fx-font-family: 'Cairo', 'Almarai', 'Segoe UI';");
    } else {
        scene.getRoot().setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        scene.getRoot().setStyle("-fx-font-family: 'Inter', 'Segoe UI', 'Roboto';");
    }
}
```

---

## 3. Date, Currency & Numeric Formatting

1. **Currency Display:** Prices are formatted using the local gym currency standard (EGP / Egyptian Pound):
   - Arabic display: `1,200.00 ج.م`
   - English display: `1,200.00 EGP`
2. **Date Format:** ISO standard `YYYY-MM-DD` (e.g. `2026-10-25`) or localized textual date (`25 أكتوبر 2026`).
3. **Arabic Excel Export Integrity (UTF-8 BOM):**
   When exporting tabular reports to CSV, `DataExportHelper` automatically injects the 3-byte UTF-8 Byte Order Mark (`0xEF, 0xBB, 0xBF`) at the very beginning of the byte stream:
   ```java
   outputStream.write(0xEF);
   outputStream.write(0xBB);
   outputStream.write(0xBF);
   ```
   *Rationale:* Without this BOM, Windows Excel opens Arabic CSV files as scrambled Latin-1 characters. With the BOM, Excel immediately recognizes UTF-8 and renders Arabic names and comments crisply.

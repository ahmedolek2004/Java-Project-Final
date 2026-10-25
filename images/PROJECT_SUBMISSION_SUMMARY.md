# FORGE FITNESS & MARTIAL ARTS MANAGEMENT SYSTEM
## ملخص فكرة المشروع وتسليمه الأكاديمي (Project Submission Document)

> 💡 **ملاحظة:** تم تصميم هذا المستند ليكون جاهزاً للنسخ مباشرة إلى برنامج **Microsoft Word**، مع إمكانية تحويله إلى **PDF** بعد ملء بيانات الفريق والدكتور.
> 
> Designed to be easily copied to Word, customized with team details, and printed to PDF for academic submission.

---

### 🎓 بيانات المشروع والفريق (Project & Team Information)

| البيان | التفاصيل / Details |
| :--- | :--- |
| **اسم المشروع / Project Name:** | Forge Fitness & Martial Arts Academy Management System |
| **المادة / Course:** | [اكتب اسم المادة هنا - e.g., Java Programming / Software Engineering] |
| **إشراف الدكتور / Supervised by:** | د. [اكتب اسم الدكتور هنا / Dr. Name] |
| **الجامعة / الكلية:** | جامعة برج العرب التكنولوجية / كلية تكنولوجيا العلوم الصحية والصناعية |
| **تاريخ التسليم / Date:** | أكتوبر 2026 / October 2026 |

#### 👥 أعضاء فريق العمل (Team Members):
| # | اسم الطالب / Student Name | كود الطالب / Student ID | الدور في المشروع / Role |
| :-: | :--- | :--- | :--- |
| 1 | [اسم العضو الأول] | [الكود / ID] | Team Leader / Backend Developer |
| 2 | [اسم العضو الثاني] | [الكود / ID] | Frontend & UI/UX Developer |
| 3 | [اسم العضو الثالث] | [الكود / ID] | Database & Persistence Engineer |
| 4 | [اسم العضو الرابع] | [الكود / ID] | QA & Technical Documentation |
| 5 | [اسم العضو الخامس] | [الكود / ID] | Business Logic & System Analyst |

---

## 📌 أولاً: نبذة مختصرة عن المشروع (باللغة العربية)

### 1. المشكلة التي يعالجها النظام (The Problem)
تعاني معظم الصالات الرياضية وأكاديميات الفنون القتالية من:
1. **طوابير وبطء تسجيل الحضور** في أوقات الذروة بسبب البحث اليدوي في الدفاتر أو شيتات الإكسيل.
2. **تسريب الإيرادات ودخول غير المستحقين** بسبب عدم اكتشاف الاشتراكات المنتهية أو المجمدة فوراً.
3. **توقف العمل عند انقطاع الإنترنت** في الأنظمة السحابية (Cloud Systems) مما يعطل الاستقبال تماماً.
4. **صعوبة الحسابات والرواتب** واحتساب صافي الأرباح وخصم المصروفات اليومية ورواتب المدربين.

### 2. الحل المقترح (The Solution)
**نظام Forge Fitness** هو تطبيق مكتبي (Desktop Application) احترافي، متكامل، وسريع جداً، تم بناؤه بأحدث تقنيات **Java 25** و **JavaFX 21**، ويعمل بالكامل **بدون إنترنت (Offline-First)** مع قاعدة بيانات مدمجة **SQLite**.

### 3. أهم مميزات النظام (Key Features)
- **تسجيل حضور لحظي بالـ QR Code:** دخول العضو في أقل من ثانيتين بمجرد مسح بطاقة العضوية بقارئ الباركود، مع تنبيه صوتي وشاشة خضراء/حمراء.
- **إدارة اشتراكات مرنة نوعين:**
  - اشتراك بالمدة (شهر، 3 أشهر، سنة).
  - اشتراك بالجلسات (مثل: 12 تمرين كيك بوكسينغ يتم خصم جلسة مع كل حضور تلقائياً حتى ينتهي).
- **قواعد عمل دقيقة:** منع تكرار الدخول في نفس اليوم، وتجميد الاشتراك وحفظ رصيد الأيام للمشترك.
- **محاسبة ورواتب كاملة:** تسجيل الإيرادات، المصروفات، صرف رواتب الموظفين والمدربين شهرياً، وحساب صافي الربح التلقائي.
- **واجهة عصرية ثنائية اللغة:** دعم كامل للغة العربية (بمحاذاة كاملة من اليمين لليسار RTL) والإنجليزية، مع وضع ليلي احترافي (Dark Mode).
- **أمان ونسخ احتياطي:** تشفير كلمات المرور بأقوى معايير التشفير (PBKDF2)، مع إمكانية أخذ نسخة احتياطية من قاعدة البيانات بضغطة زر واحدة، وتصدير البيانات إلى Excel.

---

## 📌 Second: Project Executive Summary (In English)

### 1. Problem Statement
Fitness clubs and martial arts academies face major operational bottlenecks: long reception queues during peak hours, revenue loss due to undetected expired subscriptions, dependency on unstable internet connections for cloud software, and chaotic manual payroll and expense calculations.

### 2. Proposed Solution
**Forge Fitness & Martial Arts Management System** is an industrial-grade, offline-first desktop application developed in modern **Java 25** and **JavaFX 21**, backed by an embedded **SQLite** database. It provides front-desk receptionists and club owners with an ultra-fast, robust, and zero-subscription platform to manage all daily club operations autonomously.

### 3. Core Capabilities
- **Sub-Second QR Check-In:** Validates member entry in `< 1.5 seconds` using standard USB QR/Barcode scanners, complete with visual and audible approval/rejection feedback.
- **Dual Subscription Models:**
  - *Time-Based:* Traditional duration plans (Monthly, Quarterly, Annual).
  - *Session-Based:* Training package plans (e.g., 12 boxing sessions decremented automatically upon check-in).
- **Strict Business Integrity:** Enforces a maximum of one check-in per member per calendar day and supports subscription freezing/unfreezing.
- **Financial & Payroll Automation:** Logs income, operational expenses, monthly staff/coach payroll disbursements, and computes real-time net operating margin.
- **Bilingual & Modern UI/UX:** Instant switching between English (LTR) and Arabic with native Right-To-Left (RTL) typography, accompanied by Dark & Light theme options.
- **Data Protection & 1-Click Backup:** Passwords hashed with PBKDF2-HMAC-SHA256, 1-click SQLite database backup/restore, and UTF-8 BOM CSV exports for Excel compatibility.

---

## 🛠️ التقنيات المستخدمة والمعمارية (Technology Stack & Architecture)

| الطبقة / Component | التقنية المستخدمة / Technology | الفائدة التقنية / Technical Benefit |
| :--- | :--- | :--- |
| **لغة البرمجة / Language** | **Java 25 (OpenJDK)** | أعلى أداء مع أحدث مزايا لغة جافا الحديثة |
| **الواجهات الرسومية / GUI** | **OpenJFX (JavaFX) 21.0.6** | واجهات تفاعلية سريعة، حديثة، وتدعم الـ CSS و الـ RTL |
| **قاعدة البيانات / Database** | **Embedded SQLite 3.50** | مدمجة بدون سيرفر، سرعة فائقة، حماية كاملة، نسخ احتياطي سهل |
| **محرك الباركود / QR Engine** | **Google ZXing 3.5.3** | توليد وقراءة رموز الـ QR للبطاقات والتحقق الفوري |
| **التشفير / Cryptography** | **PBKDF2 with HMAC-SHA256** | تشفير كلمات المرور بـ 65,536 تكرار مع Salt عشوائي |
| **المعمارية / Architecture** | **Clean Layered Architecture** | فصل تام بين واجهات العرض (UI)، المنطق (Service)، والبيانات (DAO) |

---

## 📊 تدفق عملية تسجيل الحضور بالـ QR (Attendance Flowchart)

```text
  [ ماسح الـ QR أو البحث اليدوي ] 
                 │
                 ▼
  [ التحقق من وجود العضو في النظام ] 
                 │
                 ├── (غير مسجل) ────► 🔴 رفض الدخول
                 │
                 ▼
  [ فحص الحضور: هل سجل دخول اليوم بالفعل؟ ] 
                 │
                 ├── (سجل اليوم مسبقاً) ──► 🟡 تنبيه: تم تسجيل الدخول اليوم
                 │
                 ▼
  [ فحص الاشتراك: هل يوجد اشتراك نشط؟ ] 
                 │
                 ├── (منتهي / مجمد) ────► 🔴 رفض: الاشتراك منتهي أو مجمد
                 │
                 ▼
  [ خصم جلسة (لو كان اشتراك جلسات) + تسجيل الدخول ] 
                 │
                 ▼
  [ 🟢 قبول الدخول + إشعار صوتي + عرض اسم العضو والرصيد المتبقي ]
```

---

## 🌟 لماذا يعتبر هذا المشروع متميزاً وقوياً؟ (Why It Stands Out)
1. **مشروع واقعي وعملي 100%:** ليس مجرد شاشات إدخال وهمية، بل نظام متكامل قابل للتشغيل الفعلي في أي جيم غداً.
2. **استقلالية تامة عن الإنترنت:** لا يتوقف العمل إطلاقاً حتى لو انقطع الإنترنت أو السيرفر الخارجي.
3. **سرعة استجابة فائقة:** تسجيل الحضور في جزء من الثانية يمنع أي تكدس في صالة الاستقبال.
4. **كود احترافي (Clean Code):** معمارية منظمة ومقسمة تتبع معايير هندسة البرمجيات القياسية، مع وجود توثيق هندسي كامل يضم أكثر من 20 مستنداً ومخططاً معمارياً.

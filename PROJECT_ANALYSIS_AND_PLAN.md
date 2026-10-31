# تحليل مشروع GYM وخطة التنفيذ (مهمة بمهمة)

## 1. حالة المشروع الحالية
- Java + JavaFX + SQLite، حوالي 5,650 سطر Java، والطبقات موجودة: `model / dao / service / controller(page, form, helper) / util`.
- المشروع **اتعمل له ريفاكتور جزئي بالفعل**: `GymApplication` بقى 291 سطر (كان ~1,274)، والصفحات اتفصلت في `controller/page`.
- الـ Services موجودة لكنها **مجرد تمرير (pass-through)** للـ DAO، ومفيهاش validation ولا صلاحيات.

## 2. اللي لقيته (مرتب بالأهمية)
| # | المشكلة | المكان | النوع |
|---|---------|--------|-------|
| 1 | شرط "الاشتراك صالح الآن" متكرر 5 مرات في SQL | `GymRepository` | تكرار (**اتحل في المهمة 1**) |
| 2 | `date('now')` في SQLite بتحسب UTC، بينما تواريخ الاشتراك بتتخزن بتوقيت الجهاز. في مصر ممكن اشتراك يبدأ النهارده يتقفل لحد 2–3 الصبح، واليوم الجديد للـ check-in بيبدأ الساعة 2–3 ص مش 12 | `GymRepository`, `DatabaseConnection` (الـ unique index) | **Bug** |
| 3 | `GymRepository` كلاس ضخم (550 سطر) فيه members + subscriptions + attendance + finance + settings + 12 record متداخل | `dao` | God class / SRP |
| 4 | تسمية متضاربة: `MemberDAO` / `UserDAO` / `UserRepository` / `ClubRepository` / `GymRepository` | `dao` | تصميم |
| 5 | الـ records (`GymRepository.MemberRow` …) مربوطة باسم الريبو، وكل الصفحات والخدمات بتعتمد عليه | كل المشروع | Coupling |
| 6 | الصلاحيات بتتحجب من القائمة بس، مش من الـ Service. والملاحظات الطبية ظاهرة للريسبشن | `service`, `GymApplication` | أمان |
| 7 | `catch (Exception ex)` في 17 مكان في الصفحات | `controller/page` | Clean code |
| 8 | كود CRUD مكرر بين الصفحات (Activities / Coaches / Schedule / Team) | `controller/page` | تكرار |
| 9 | `User / Admin / Receptionist` موجودين لكن الصلاحيات مش polymorphic | `model` | OOP |
| 10 | التستات: اختبار DAO واحد + اختبار services بـ Proxy، ومفيش اختبار لقواعد الاشتراك والتوقيت | `src/test` | جودة |

## 3. خطة المهام (مهمة لكل يوم، كل مهمة تخلص كاملة وتتسلم بملفاتها)
| اليوم | المهمة | الحجم |
|-------|--------|-------|
| **1 ✅** | استخراج `SubscriptionRules` وإزالة تكرار شرط الصلاحية من الـ SQL (بدون تغيير سلوك) | ملفين |
| **2 ✅** | إصلاح مشكلة التوقيت (UTC مقابل المحلي) + migration للـ unique index + تستات حدودية | `SqlClock`, `AttendanceDateMigration`, `DatabaseConnection`, `GymRepository` + تستين |
| **3 ✅** | نقل 12 record من `GymRepository` لحزمة `com.gym.system.dto` وتحديث كل الاستخدامات (32 ملف) | تغيير ميكانيكي |
| **4 ✅** | تقسيم `GymRepository` (505 → 237 سطر) إلى Member / Subscription / Attendance / Finance / Settings repositories خلف interfaces، و`GymRepository` بقى facade بدون SQL | dao |
| **5 ✅** | تقسيم `ClubRepository` إلى Activity / Coach / Schedule / Payroll + `SqlSupport` للدوال المشتركة وتوحيد التسمية (`XxxDAO` عقد، `XxxRepository` تنفيذ) | dao |
| **6 ✅** | فرض الصلاحيات في الـ Services عبر `AccessControl` + validation + إخفاء الملاحظات الطبية عن الريسبشن + تحديث README وCHANGELOG | service, test, docs |
| 7 | إزالة استدعاءات الـ repository المباشرة من الصفحات (Dashboard/Finance/Print) + استبدال `catch (Exception)` بمعالجة محددة + معالج أخطاء مركزي يعرض `AccessDeniedException` | controller/page |
| 8 | استخراج `CrudPage` أساسي (abstract) يقلل التكرار بين الصفحات | controller/page |
| 9 | OOP للأدوار (`Role` + صلاحيات polymorphic) وتحسين encapsulation للـ models | model |
| 10 | تستات الخدمات والقواعد على قاعدة بيانات مؤقتة، وتحديث التوثيق | test + docs |

## 4. المهمة 1: اللي اتعمل
- ملف جديد `dao/SubscriptionRules.java`: فيه شرط "الاشتراك صالح الآن" وتعريف `TODAY` في مكان واحد.
- `GymRepository.java`: الاستعلامات الأربعة (`members`, `setSubscriptionStatus`, `checkIn`, `dashboard`) بقت تستخدم `{USABLE}` بدل النسخ المكررة.
- **التحقق:** شغلت الاستعلامات القديمة والجديدة على SQLite بنفس البيانات (اشتراك ساري، آخر يوم، منتهي، لسه ما بدأش، باقة جلسات بجلسات وبدون، مجمّد) والنتائج متطابقة 100%.
- **ما اتعملش عمدًا:** تغيير سلوك التوقيت (ده المهمة 2) عشان كل مهمة تبقى لغرض واحد.

## 5. حدود التحقق (مهم)
البيئة هنا فيها JRE فقط (Java 21) بدون JDK ولا Maven ولا JavaFX، فمقدرتش أعمل compile أو أشغل التستات. اتأكدت من الـ SQL بالاختبار المباشر، والتغيير في Java بسيط (استدعاء دالة static). شغّل عندك:
```
mvnw.cmd clean test
```

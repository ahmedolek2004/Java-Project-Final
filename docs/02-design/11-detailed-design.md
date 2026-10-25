# 11. Detailed Design & Interface Contracts

> **Document ID:** DOC-DESIGN-011  
> **Target System:** Forge Fitness Management System  
> **Status:** Active & Implemented

---

## 1. Core Service Interface Contracts

### 1.1 `MemberService` Contract
Manages member lifecycle, phone uniqueness invariants, and search queries.

```java
public class MemberService {
    // Validates inputs, checks phone uniqueness, and persists new member
    public Member createMember(String fullName, String phone, String email, 
                               String gender, String medicalNotes) throws ValidationException;

    // Updates profile details; validates phone uniqueness if phone is altered
    public void updateMember(Member member) throws ValidationException;

    // Returns all members or filters by name/phone substring
    public List<Member> searchMembers(String query);

    // Retrieves single member by unique ID
    public Optional<Member> getMemberById(int id);
}
```

### 1.2 `SubscriptionService` Contract
Manages plan sales, discounts, status transitions, and session decrements.

```java
public class SubscriptionService {
    // Creates a new plan, applies percentage discount, and logs an initial payment atomically
    public Subscription createSubscription(int memberId, String planName, PlanType planType,
                                          Integer durationMonths, Integer totalSessions,
                                          LocalDate startDate, double basePrice, double discountPercent,
                                          Integer activityId) throws ValidationException;

    // Freezes an active subscription, halting check-in rights
    public void freezeSubscription(int subscriptionId) throws BusinessRuleException;

    // Unfreezes a frozen subscription, restoring active rights
    public void unfreezeSubscription(int subscriptionId) throws BusinessRuleException;

    // Retrieves the active subscription for a member, if one exists
    public Optional<Subscription> getActiveSubscription(int memberId);
}
```

### 1.3 `AttendanceService` Contract
Manages the critical path check-in logic, QR payload parsing, and session consumption.

```java
public class AttendanceService {
    // Processes a check-in attempt from a raw scanned string or manual ID entry
    public CheckInResult processCheckIn(String rawIdentifier);

    // Records verified check-in, decrements session if SESSION_BASED, commits atomically
    public AttendanceRecord recordCheckIn(int memberId, int subscriptionId);

    // Queries daily attendance list for the reception dashboard
    public List<AttendanceRecord> getTodayAttendance();
}
```

#### Check-in Result Type Contract:
```java
public record CheckInResult(
    boolean approved,
    String message,
    Member member,
    Subscription subscription,
    int remainingSessions,
    RejectReason rejectReason // NONE, NO_ACTIVE_PLAN, EXPIRED, FROZEN, ALREADY_CHECKED_IN_TODAY
) {}
```

---

## 2. Transaction Handling & Persistence Patterns

All multi-table write operations follow a strict transactional pattern to prevent orphaned records:

```java
try (Connection conn = DatabaseConnection.open()) {
    conn.setAutoCommit(false); // Begin ACID Transaction
    try {
        // 1. Insert Subscription Record
        int subscriptionId = subscriptionDao.insert(conn, subscription);
        
        // 2. Insert Associated Payment Record
        paymentDao.insert(conn, new Payment(memberId, subscriptionId, finalPrice, "PAID"));
        
        conn.commit(); // Atomic Commit
    } catch (SQLException ex) {
        conn.rollback(); // Rollback on any failure
        throw new PersistenceException("Failed to commit subscription sale", ex);
    }
}
```

---

## 3. Standardized Error Handling Pattern

The application categorizes exceptions into three clean tiers:
1. **`ValidationException` (Checked/Domain):** Input formatting errors (e.g. invalid phone number, negative price, discount > 100%). Displayed directly to the user as a gentle warning dialog.
2. **`BusinessRuleException` (Checked/Domain):** Violation of domain invariants (e.g., duplicate check-in today, duplicate monthly salary). Surfaced with explicit contextual guidance.
3. **`PersistenceException` (Unchecked/System):** Disk full, SQLite locking, or hardware IO failures. Caught at the `PageContext` / Controller boundary, logged, and surfaced via `AlertUtils.showError()`.

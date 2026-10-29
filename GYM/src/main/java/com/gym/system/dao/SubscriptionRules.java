package com.gym.system.dao;

/**
 * Single source of truth for the SQL that decides whether a subscription row is
 * currently usable. Every query in the DAO layer that needs this rule goes through
 * {@link #apply(String, String)} instead of repeating the condition by hand.
 */
final class SubscriptionRules {

    /** Placeholder that is replaced by the "usable now" condition. */
    static final String USABLE_PLACEHOLDER = "{USABLE}";

    private SubscriptionRules() {
    }

    /**
     * Returns the SQL condition that is true when a subscription is inside its validity
     * window: a session pack with sessions left, or a time plan that has started and
     * has not passed its (inclusive) end date. Status checks stay at the call site.
     *
     * @param alias column prefix such as {@code "s."}, or an empty string when unqualified
     */
    static String usableCondition(String alias) {
        return "(" + alias + "plan_type='SESSION_BASED' AND " + alias + "remaining_sessions > 0"
                + " OR " + alias + "plan_type='TIME_BASED' AND " + alias + "start_date <= " + SqlClock.TODAY
                + " AND " + alias + "end_date >= " + SqlClock.TODAY + ")";
    }

    /**
     * Replaces {@link #USABLE_PLACEHOLDER} in {@code sql} with the condition for {@code alias}, and
     * also resolves the {@link SqlClock} placeholders.
     */
    static String apply(String sql, String alias) {
        return SqlClock.apply(sql.replace(USABLE_PLACEHOLDER, usableCondition(alias)));
    }
}

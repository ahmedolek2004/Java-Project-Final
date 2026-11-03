package com.gym.system.service;

import com.gym.system.model.User;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Decides what the currently signed-in user may do. Services call it before touching data, so the
 * rules hold even if a screen forgets to hide a button.
 */
public final class AccessControl {

    private static final String ADMIN_ROLE = "ADMIN";

    private final Supplier<Optional<User>> currentUser;

    public AccessControl(Supplier<Optional<User>> currentUser) {
        this.currentUser = currentUser;
    }

    /** True when an administrator is signed in. */
    public boolean isAdmin() {
        return currentUser.get().map(user -> ADMIN_ROLE.equals(user.role())).orElse(false);
    }

    /** Requires any signed-in staff member. */
    public void requireSignedIn() {
        if (currentUser.get().isEmpty()) {
            throw new AccessDeniedException("Sign in to continue.");
        }
    }

    /** Requires a signed-in administrator. */
    public void requireAdmin() {
        requireSignedIn();
        if (!isAdmin()) {
            throw new AccessDeniedException("This action is available to administrators only.");
        }
    }
}

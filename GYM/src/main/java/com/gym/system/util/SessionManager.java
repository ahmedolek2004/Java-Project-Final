package com.gym.system.util;

import com.gym.system.model.User;

import java.util.Optional;

public final class SessionManager {
    private User currentUser;

    public void signIn(User user) {
        if (user == null) throw new IllegalArgumentException("A signed-in user is required.");
        currentUser = user;
    }

    public void signOut() {
        currentUser = null;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.role());
    }
}

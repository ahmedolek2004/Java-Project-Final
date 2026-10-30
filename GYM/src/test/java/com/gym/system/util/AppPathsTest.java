package com.gym.system.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPathsTest {
    @Test
    void databaseLivesInsideTheDataDirectory() {
        assertEquals("gym_system.db", AppPaths.databasePath().getFileName().toString());
        assertEquals(AppPaths.dataDirectory(), AppPaths.databasePath().getParent());
    }

    @Test
    void legacyLocationStaysInTheUserFolder() {
        assertTrue(AppPaths.legacyDatabasePath().toString().contains(".gym-system"));
        assertTrue(AppPaths.legacyDatabasePath().startsWith(System.getProperty("user.home")));
    }
}

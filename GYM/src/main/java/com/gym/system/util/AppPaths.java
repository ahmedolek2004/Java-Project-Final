package com.gym.system.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

public final class AppPaths {
    public static final String DATA_DIRECTORY_PROPERTY = "gym.data.dir";
    private static final String DATA_FOLDER = "data";
    private static final String DATABASE_FILE = "gym_system.db";

    private static Path dataDirectory;

    private AppPaths() {
    }

    public static synchronized Path dataDirectory() {
        if (dataDirectory == null) {
            dataDirectory = resolveDataDirectory();
        }
        return dataDirectory;
    }

    public static synchronized void useDirectory(Path directory) {
        dataDirectory = directory.toAbsolutePath();
    }

    public static Path databasePath() {
        return dataDirectory().resolve(DATABASE_FILE);
    }

    public static Path legacyDatabasePath() {
        return Path.of(System.getProperty("user.home"), ".gym-system", DATABASE_FILE);
    }

    private static Path resolveDataDirectory() {
        String override = System.getProperty(DATA_DIRECTORY_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath();
        }
        Path candidate = applicationDirectory().resolve(DATA_FOLDER);
        if (isUsable(candidate)) {
            return candidate;
        }
        return legacyDatabasePath().getParent();
    }

    private static Path applicationDirectory() {
        try {
            CodeSource source = AppPaths.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Path location = Path.of(source.getLocation().toURI()).toAbsolutePath();
                if (Files.isRegularFile(location)) {
                    return location.getParent();
                }
                Path parent = location.getParent();
                if (parent != null && "classes".equals(String.valueOf(location.getFileName()))
                        && "target".equals(String.valueOf(parent.getFileName()))
                        && parent.getParent() != null) {
                    return parent.getParent();
                }
                return location;
            }
        } catch (URISyntaxException | RuntimeException ignored) {
        }
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    private static boolean isUsable(Path directory) {
        try {
            Files.createDirectories(directory);
            return Files.isWritable(directory);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }
}

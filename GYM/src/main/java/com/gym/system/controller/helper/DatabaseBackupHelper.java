package com.gym.system.controller.helper;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.function.Consumer;

public final class DatabaseBackupHelper {
    private static final Path DATABASE_PATH = Path.of(
            System.getProperty("user.home"), ".gym-system", "gym_system.db");

    private DatabaseBackupHelper() {}

    public static void backup(Window window, Consumer<String> onSuccess, Consumer<String> onError) {
        if (!Files.exists(DATABASE_PATH)) {
            onError.accept("Database file does not exist yet.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Backup Database");
        chooser.setInitialFileName("gym_system_backup_" + LocalDate.now() + ".db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));
        File dest = chooser.showSaveDialog(window);

        if (dest != null) {
            try {
                Files.copy(DATABASE_PATH, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                onSuccess.accept(dest.getAbsolutePath());
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        }
    }

    public static void restore(Window window, Consumer<String> onSuccess, Consumer<String> onError) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Restore Database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database (*.db)", "*.db"));
        File source = chooser.showOpenDialog(window);

        if (source != null) {
            try {
                Files.createDirectories(DATABASE_PATH.getParent());
                Files.copy(source.toPath(), DATABASE_PATH, StandardCopyOption.REPLACE_EXISTING);
                onSuccess.accept(source.getAbsolutePath());
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        }
    }
}

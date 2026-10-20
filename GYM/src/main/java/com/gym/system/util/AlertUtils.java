package com.gym.system.util;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.net.URL;

public final class AlertUtils {
    private static final URL STYLESHEET = AlertUtils.class.getResource(
            "/com/gym/system/views/css/style.css");

    private AlertUtils() {}

    public static void show(Alert alert, String title, String header, boolean arabic, boolean darkMode) {
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().getStylesheets().add(STYLESHEET.toExternalForm());
        if (darkMode) {
            alert.getDialogPane().getStyleClass().add("dark-dialog");
        }
        alert.getDialogPane().setNodeOrientation(arabic
                ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message, boolean arabic, boolean darkMode) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        show(alert, title, null, arabic, darkMode);
        return alert.getResult() == ButtonType.YES;
    }

    public static void showError(String title, String message, Exception exception,
                                 boolean arabic, boolean darkMode) {
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        Alert alert = new Alert(Alert.AlertType.ERROR, message + System.lineSeparator() + detail, ButtonType.OK);
        show(alert, title, null, arabic, darkMode);
    }
}

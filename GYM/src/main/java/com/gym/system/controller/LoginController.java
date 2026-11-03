package com.gym.system.controller;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.util.function.BiConsumer;

public final class LoginController {
    @FXML private StackPane root;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    @FXML private Label errorLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button languageButton;

    private BiConsumer<String, char[]> onAuthenticate;
    private Runnable onLanguage;

    public void configure(String title, String subtitle, String username, String password,
                          String login, String language, NodeOrientation orientation,
                          BiConsumer<String, char[]> authenticate, Runnable toggleLanguage) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
        usernameLabel.setText(username);
        passwordLabel.setText(password);
        loginButton.setText(login);
        languageButton.setText(language);
        root.setNodeOrientation(orientation);
        onAuthenticate = authenticate;
        onLanguage = toggleLanguage;
    }

    @FXML
    private void submit() {
        errorLabel.setText("");
        onAuthenticate.accept(usernameField.getText(), passwordField.getText().toCharArray());
        passwordField.clear();
    }

    @FXML
    private void changeLanguage() {
        onLanguage.run();
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}

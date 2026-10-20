package com.gym.system.controller;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DashboardController {
    public record NavigationItem(String section, String label, String icon) {}

    @FXML private BorderPane root;
    @FXML private VBox navigation;
    @FXML private BorderPane pageHost;
    @FXML private Label clubLabel;
    @FXML private Label workspaceLabel;
    @FXML private Label initialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private Button languageButton;
    @FXML private Button themeButton;
    @FXML private Button logoutButton;
    @FXML private Label accentLabel;
    @FXML private ColorPicker accentPicker;

    private final List<Button> navigationButtons = new ArrayList<>();

    public void configure(String club, String workspace, String initials, String userName, String role,
                          String language, String theme, String accent, String logout,
                          boolean darkMode, String accentColor, NodeOrientation orientation,
                          List<NavigationItem> items, Consumer<String> onNavigate,
                          Runnable onLanguage, Consumer<Boolean> onTheme,
                          Consumer<Color> onAccent, Runnable onLogout) {
        clubLabel.setText(club);
        workspaceLabel.setText(workspace);
        initialsLabel.setText(initials);
        userNameLabel.setText(userName);
        userRoleLabel.setText(role);
        languageButton.setText(language);
        themeButton.setText(theme);
        accentLabel.setText(accent);
        logoutButton.setText(logout);
        root.setNodeOrientation(orientation);
        root.getStyleClass().setAll("app-shell");
        if (darkMode) {
            root.getStyleClass().add("dark-theme");
        }
        accentPicker.setAccessibleText(accent);
        accentPicker.setValue(Color.web(accentColor));
        languageButton.setOnAction(event -> onLanguage.run());
        themeButton.setOnAction(event -> onTheme.accept(!darkMode));
        accentPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) onAccent.accept(newValue);
        });
        logoutButton.setOnAction(event -> onLogout.run());
        navigation.getChildren().clear();
        navigationButtons.clear();
        for (NavigationItem item : items) {
            Button button = new Button(item.label());
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(orientation == NodeOrientation.RIGHT_TO_LEFT
                    ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            button.setGraphic(new Label(item.icon()));
            button.setOnAction(event -> onNavigate.accept(item.section()));
            navigation.getChildren().add(button);
            navigationButtons.add(button);
        }
    }

    public void showPage(String title, String subtitle, Node content, String activeSection,
                         List<NavigationItem> items) {
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
        pageHost.setCenter(content);
        for (int index = 0; index < navigationButtons.size(); index++) {
            Button button = navigationButtons.get(index);
            button.getStyleClass().remove("selected");
            if (items.get(index).section().equals(activeSection)) {
                button.getStyleClass().add("selected");
            }
        }
    }
}

package com.gym.system.controller;

import com.gym.system.controller.helper.AppearanceHelper;
import com.gym.system.controller.helper.WindowChrome;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class WizardDialog {
    private static final double CONTENT_WIDTH = 590;

    private WizardDialog() {}

    public static final class Step {
        private final String title;
        private final Node content;
        private final Supplier<String> validator;
        private Runnable onEnter = () -> {};

        public Step(String title, Node content, Supplier<String> validator) {
            this.title = title;
            this.content = content;
            this.validator = validator;
        }

        public Step onEnter(Runnable action) {
            this.onEnter = action;
            return this;
        }
    }

    public static boolean show(String title, String finishText, boolean darkMode, boolean arabic,
                               Function<String, String> t, List<Step> steps) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(title);
        WindowChrome.decorateDialog(dialog, title);
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("wizard-dialog");

        ButtonType cancelType = new ButtonType(t.apply("wizard.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType backType = new ButtonType(t.apply("wizard.back"), ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType nextType = new ButtonType(t.apply("wizard.next"), ButtonBar.ButtonData.NEXT_FORWARD);
        pane.getButtonTypes().addAll(cancelType, backType, nextType);
        Button cancel = (Button) pane.lookupButton(cancelType);
        Button back = (Button) pane.lookupButton(backType);
        Button next = (Button) pane.lookupButton(nextType);
        next.getStyleClass().add("primary-button");
        next.setDefaultButton(true);
        next.setMinWidth(Region.USE_PREF_SIZE);
        ButtonBar.setButtonUniformSize(next, false);
        cancel.setCancelButton(true);

        HBox indicator = new HBox(8);
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.getStyleClass().add("wizard-steps");
        List<Label> badges = new ArrayList<>();
        List<Label> captions = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            Label badge = new Label(String.valueOf(index + 1));
            badge.getStyleClass().add("wizard-badge");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            Label caption = new Label(steps.get(index).title);
            caption.getStyleClass().add("wizard-caption");
            caption.setMinWidth(Region.USE_PREF_SIZE);
            badges.add(badge);
            captions.add(caption);
            indicator.getChildren().addAll(badge, caption);
            if (index < steps.size() - 1) {
                Region line = new Region();
                line.getStyleClass().add("wizard-line");
                HBox.setHgrow(line, Priority.ALWAYS);
                line.setMinWidth(12);
                line.setMaxHeight(1);
                line.setPrefHeight(1);
                indicator.getChildren().add(line);
            }
        }

        StackPane stack = new StackPane();
        stack.setAlignment(Pos.TOP_LEFT);
        double tallest = 0;
        for (Step step : steps) {
            tallest = Math.max(tallest, step.content.prefHeight(CONTENT_WIDTH));
        }
        stack.setMinHeight(tallest);

        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("validation-message");
        error.setManaged(false);
        error.setVisible(false);

        VBox body = new VBox(14, indicator, stack, error);
        body.setPadding(new Insets(2, 0, 0, 0));
        body.setPrefWidth(CONTENT_WIDTH);
        body.setMinWidth(CONTENT_WIDTH);
        pane.setContent(body);

        int[] current = {0};
        Runnable render = () -> {
            Step step = steps.get(current[0]);
            stack.getChildren().setAll(step.content);
            for (int index = 0; index < steps.size(); index++) {
                setState(badges.get(index), index == current[0], index < current[0]);
                setState(captions.get(index), index == current[0], index < current[0]);
            }
            boolean last = current[0] == steps.size() - 1;
            next.setText(last ? finishText : t.apply("wizard.next"));
            back.setDisable(current[0] == 0);
            error.setVisible(false);
            error.setManaged(false);
            step.onEnter.run();
            if (pane.getScene() != null && pane.getScene().getWindow() != null
                    && pane.getScene().getWindow().isShowing()) {
                pane.getScene().getWindow().sizeToScene();
            }
        };

        back.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            if (current[0] > 0) {
                current[0]--;
                render.run();
            }
        });
        next.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String problem = steps.get(current[0]).validator.get();
            if (problem != null && !problem.isBlank()) {
                error.setText(problem);
                error.setVisible(true);
                error.setManaged(true);
                return;
            }
            if (current[0] < steps.size() - 1) {
                current[0]++;
                render.run();
            } else {
                dialog.setResult(true);
                dialog.close();
            }
        });
        pane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                dialog.close();
            }
        });

        dialog.setOnShowing(event -> {
            Node bar = pane.lookup(".button-bar");
            if (bar instanceof ButtonBar buttonBar) {
                buttonBar.setButtonOrder(ButtonBar.ButtonData.CANCEL_CLOSE.getTypeCode() + "_"
                        + ButtonBar.ButtonData.BACK_PREVIOUS.getTypeCode()
                        + ButtonBar.ButtonData.NEXT_FORWARD.getTypeCode());
            }
        });

        pane.getStylesheets().add(
                WizardDialog.class.getResource("/com/gym/system/views/css/style.css").toExternalForm());
        if (darkMode) {
            pane.getStyleClass().add("dark-dialog");
        }
        AppearanceHelper.applyAccent(pane, darkMode);
        pane.setNodeOrientation(arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        render.run();
        return dialog.showAndWait().orElse(false);
    }

    private static void setState(Label label, boolean active, boolean done) {
        label.getStyleClass().removeAll("wizard-active", "wizard-done");
        if (active) {
            label.getStyleClass().add("wizard-active");
        } else if (done) {
            label.getStyleClass().add("wizard-done");
        }
    }
}

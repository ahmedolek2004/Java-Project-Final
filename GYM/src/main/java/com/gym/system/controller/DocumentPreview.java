package com.gym.system.controller;

import com.gym.system.controller.helper.AppearanceHelper;
import com.gym.system.controller.helper.DocumentFactory;
import com.gym.system.controller.helper.WindowChrome;
import com.gym.system.util.ImageFiles;
import com.gym.system.util.PrintService;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class DocumentPreview {
    private DocumentPreview() {}

    public static void show(String title, Image image, String fileName, boolean darkMode, boolean arabic,
                            Function<String, String> t, BiConsumer<String, Exception> onError) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(
                new ButtonType(t.apply("common.close"), ButtonBar.ButtonData.CANCEL_CLOSE));
        WindowChrome.decorateDialog(dialog, title);

        double documentWidth = image.getWidth() / DocumentFactory.SCALE;
        double documentHeight = image.getHeight() / DocumentFactory.SCALE;
        double available = Screen.getPrimary().getVisualBounds().getHeight() - 250;
        double zoom = Math.max(0.75, Math.min(1.0, available / documentHeight));
        double naturalWidth = documentWidth * zoom;
        double naturalHeight = documentHeight * zoom;
        ImageView view = new ImageView(image);
        view.setFitWidth(naturalWidth);
        view.setFitHeight(naturalHeight);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.getStyleClass().add("document-image");

        StackPane holder = new StackPane(view);
        holder.setPadding(new Insets(6));
        ScrollPane scroll = new ScrollPane(holder);
        scroll.getStyleClass().addAll("form-scroll", "document-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(Math.min(naturalHeight + 14, Math.max(available, 300)));
        scroll.setPrefViewportWidth(naturalWidth + 30);

        Label status = new Label();
        status.setWrapText(true);
        status.setMaxWidth(naturalWidth + 12);

        Button save = new Button(t.apply("doc.save"));
        save.getStyleClass().add("primary-button");
        Button print = new Button(t.apply("doc.print"));
        HBox actions = new HBox(10, save, print);
        actions.setAlignment(Pos.CENTER);

        save.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(fileName + ".png");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(t.apply("doc.png"), "*.png"));
            Window owner = dialog.getDialogPane().getScene().getWindow();
            File target = chooser.showSaveDialog(owner);
            if (target == null) {
                return;
            }
            try {
                ImageFiles.savePng(image, target);
                status.getStyleClass().remove("scan-error");
                status.getStyleClass().setAll("label", "scan-result", "scan-success");
                status.setText(t.apply("doc.saved").replace("{0}", target.getAbsolutePath()));
            } catch (IOException | RuntimeException exception) {
                status.getStyleClass().setAll("label", "scan-result", "scan-error");
                status.setText(t.apply("doc.saveFailed"));
            }
        });
        print.setOnAction(event -> {
            try {
                PrintService.printImage(dialog.getDialogPane().getScene().getWindow(), image, DocumentFactory.SCALE);
            } catch (IllegalStateException exception) {
                onError.accept(t.apply("print.failed"), exception);
            }
        });

        VBox content = new VBox(12, scroll, actions, status);
        content.setAlignment(Pos.TOP_CENTER);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(
                DocumentPreview.class.getResource("/com/gym/system/views/css/style.css").toExternalForm());
        if (darkMode) {
            dialog.getDialogPane().getStyleClass().add("dark-dialog");
        }
        AppearanceHelper.applyAccent(dialog.getDialogPane(), darkMode);
        dialog.getDialogPane().setNodeOrientation(
                arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        dialog.showAndWait();
    }
}

package com.gym.system.controller.helper;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MetricCardFactory {
    public static VBox createCard(String heading, String value, String caption) {
        Label title = new Label(heading);
        title.getStyleClass().add("section-action");
        Label amount = new Label(value);
        amount.getStyleClass().add("metric-value");
        Label detail = new Label(caption);
        detail.getStyleClass().add("muted-label");
        
        VBox card = new VBox(9, title, amount, detail);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(150);
        card.setPrefWidth(220);
        return card;
    }
}
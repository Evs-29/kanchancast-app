package com.kanchancast.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Lightweight toast using a Popup so it works regardless of root layout type.
 * Style with .toast (label) / .toast-box (container) in your CSS.
 */
public final class Toast {

    public static void show(Scene scene, String message) {
        if (scene == null) return;
        show(scene.getWindow(), message);
    }

    public static void show(Window owner, String message) {
        if (owner == null || message == null || message.isBlank()) return;

        Label label = new Label(message);
        label.getStyleClass().add("toast");

        StackPane box = new StackPane(label);
        box.getStyleClass().add("toast-box");
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(box);
        popup.show(owner);

        // Center horizontally, float near bottom
        double x = owner.getX() + (owner.getWidth() - box.getWidth()) / 2.0;
        double y = owner.getY() + owner.getHeight() - 100;
        popup.setX(x);
        popup.setY(y);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), box);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.8));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(280), box);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    private Toast() {}
}

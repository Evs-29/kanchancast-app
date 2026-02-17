package com.kanchancast.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Central helper to ensure all popups appear on the same screen as the dashboard.
 * Fixes macOS "popup appears on another monitor/space" issues by enforcing:
 * - initOwner(owner)
 * - WINDOW_MODAL
 * - centering over owner
 */
public final class PopupUtil {

    private PopupUtil() {}

    // ----------------------------
    // Alerts
    // ----------------------------

    public static void showAlert(Stage owner, Alert.AlertType type, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        applyOwnerAndModal(owner, a);
        a.showAndWait();
    }

    public static void showInfo(Stage owner, String message) {
        showAlert(owner, Alert.AlertType.INFORMATION, message);
    }

    public static void showWarn(Stage owner, String message) {
        showAlert(owner, Alert.AlertType.WARNING, message);
    }

    public static void showError(Stage owner, String message) {
        showAlert(owner, Alert.AlertType.ERROR, message);
    }

    // ----------------------------
    // Dialogs
    // ----------------------------

    public static <T> void prepareDialog(Stage owner, Dialog<T> dialog) {
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.setOnShown(e -> centerDialogOnOwner(owner, dialog));
    }

    // ----------------------------
    // Internals
    // ----------------------------

    private static void applyOwnerAndModal(Stage owner, Alert alert) {
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.setOnShown(e -> centerWindowOnOwner(owner, alert.getDialogPane().getScene().getWindow()));
    }

    private static <T> void centerDialogOnOwner(Stage owner, Dialog<T> dialog) {
        Window w = dialog.getDialogPane().getScene().getWindow();
        centerWindowOnOwner(owner, w);
    }

    private static void centerWindowOnOwner(Stage owner, Window child) {
        if (owner == null || child == null) return;

        // Run later to ensure sizes are computed
        Platform.runLater(() -> {
            try {
                Bounds b = owner.getScene().getRoot().localToScreen(owner.getScene().getRoot().getBoundsInLocal());
                if (b == null) return;

                double cx = b.getMinX() + (b.getWidth() / 2.0);
                double cy = b.getMinY() + (b.getHeight() / 2.0);

                double w = child.getWidth();
                double h = child.getHeight();

                // If width/height still 0, fallback to owner center roughly
                if (w <= 0) w = 600;
                if (h <= 0) h = 400;

                child.setX(cx - (w / 2.0));
                child.setY(cy - (h / 2.0));
            } catch (Exception ignored) {}
        });
    }
    public static boolean confirm(Stage owner, String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.NO, ButtonType.YES);
        a.setHeaderText(title);
        applyOwnerAndModal(owner, a);   // this applies owner + modal + centering
        a.showAndWait();
        return a.getResult() == ButtonType.YES;
    }
}

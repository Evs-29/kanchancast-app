package com.kanchancast.ui;

import javafx.scene.Scene;
import javafx.scene.control.Alert;

public final class UIKit {
    private UIKit(){}

    /** Apply global stylesheet safely */
    public static void apply(Scene scene) {
        if (scene == null) return;
        var url = UIKit.class.getResource("/styles.css");
        if (url != null && !scene.getStylesheets().contains(url.toExternalForm())) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    public static void toastInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    public static void toastWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}

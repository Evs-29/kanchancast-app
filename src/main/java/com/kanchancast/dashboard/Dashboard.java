package com.kanchancast.dashboard;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Dashboard {

    public static void show(Stage stage, String userName) {
        Label label = new Label("Welcome to your com.kanchancast.dashboard, " + userName + "!");
        VBox layout = new VBox(label);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 600, 400);
        stage.setScene(scene);
        stage.show();
    }
}

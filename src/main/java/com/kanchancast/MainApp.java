package com.kanchancast;

import com.kanchancast.auth.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        System.out.println("MainApp.start()"); // marker
        LoginScreen.show(stage);
    }

    public static void main(String[] args) {
        launch(args); // REQUIRED for JavaFX
    }
}

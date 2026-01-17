package com.kanchancast.auth;

import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.User;
import com.kanchancast.nav.ScreenRouter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginScreen {

    public static void show(Stage stage) {
        // ---------- TITLE ----------
        Label title = new Label("💎 Welcome to Kanchan Cast 💎");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#b8860b"));

        Label subtitle = new Label("Please sign in with your credentials");
        subtitle.setFont(Font.font("Arial", 16));
        subtitle.setTextFill(Color.web("#555"));

        // ---------- FIELDS ----------
        Label lblUserCode = new Label("User Code:");
        TextField tfUserCode = new TextField();
        tfUserCode.setPromptText("Enter your unique User Code");
        tfUserCode.setMaxWidth(250);

        Label lblUsername = new Label("Username:");
        TextField tfUsername = new TextField();
        tfUsername.setPromptText("Enter your username");
        tfUsername.setMaxWidth(250);

        Label lblPass = new Label("Password:");
        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Enter password");
        tfPass.setMaxWidth(250);

        // ---------- BUTTONS ----------
        Button btnLogin = new Button("Login");
        btnLogin.setDefaultButton(true);
        btnLogin.setPrefWidth(250);
        btnLogin.setStyle("-fx-background-color: #b8860b; -fx-text-fill: white; -fx-font-size: 15px;");

        Button btnSignup = new Button("Create Account");
        btnSignup.setPrefWidth(250);
        btnSignup.setStyle("-fx-background-color: transparent; -fx-border-color: #b8860b; -fx-text-fill: #b8860b;");

        // ---------- ACTIONS ----------
        btnLogin.setOnAction(e -> {
            String userCode = tfUserCode.getText().trim();
            String username = tfUsername.getText().trim();
            String password = tfPass.getText().trim();

            if (userCode.isEmpty() || username.isEmpty() || password.isEmpty()) {
                showAlert("Error", "Please fill in all fields — User Code, Username, and Password.");
                return;
            }

            UserDAO dao = new UserDAO();
            Optional<User> userOpt = dao.authenticateFull(userCode, username, password); // ✅ new method

            if (userOpt.isEmpty()) {
                showAlert("Login Failed", "Invalid credentials. Please try again.");
            } else {
                User user = userOpt.get();
                ScreenRouter.showDashboard(stage, user);
            }
        });

        btnSignup.setOnAction(e -> ScreenRouter.goToSignup(stage));

        // ---------- LAYOUT CARD ----------
        VBox loginBox = new VBox(12, title, subtitle,
                lblUserCode, tfUserCode,
                lblUsername, tfUsername,
                lblPass, tfPass,
                btnLogin, btnSignup);

        loginBox.setPadding(new Insets(30));
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 20; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);");

        // ---------- ROOT BACKGROUND ----------
        StackPane root = new StackPane(loginBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #fff8dc, #f5deb3);");
        StackPane.setAlignment(loginBox, Pos.CENTER);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Login");
        stage.show();
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

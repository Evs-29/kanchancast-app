package com.kanchancast.auth;

import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.User;
import com.kanchancast.nav.ScreenRouter;
import com.kanchancast.ui.PopupUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginScreen {

    public static void show(Stage stage) {
        // ✅ Fix: Clear any previous stylesheets (e.g. from Dashboard) to ensure Login
        // looks clean
        if (stage.getScene() != null) {
            stage.getScene().getStylesheets().clear();
        }

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
            String userCode = tfUserCode.getText() == null ? "" : tfUserCode.getText().trim();
            String password = tfPass.getText() == null ? "" : tfPass.getText().trim();

            if (userCode.isEmpty() || password.isEmpty()) {
                // ✅ owned popup (stays on same screen)
                PopupUtil.showWarn(stage, "Please fill in all fields — User Code and Password.");
                return;
            }

            UserDAO dao = new UserDAO();
            Optional<User> userOpt = dao.authenticateByCode(userCode, password);

            if (userOpt.isEmpty()) {
                // ✅ owned popup (stays on same screen)
                PopupUtil.showError(stage, "Invalid credentials. Please try again.");
            } else {
                User user = userOpt.get();
                ScreenRouter.showDashboard(stage, user);
            }
        });

        btnSignup.setOnAction(e -> ScreenRouter.goToSignup(stage));

        // ---------- LAYOUT CARD ----------
        VBox loginBox = new VBox(12, title, subtitle,
                lblUserCode, tfUserCode,
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

        ScreenRouter.replaceSceneContent(stage, root, 900, 600);
        stage.setTitle("Kanchan Cast — Login");
        stage.show();
    }
}

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

public class SignupScreen {

    public static void show(Stage stage) {
        // ---------- TITLE ----------
        Label title = new Label("💎 Create Your Kanchan Cast Account 💎");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#b8860b"));

        Label subtitle = new Label("Please fill in your details to register as a Customer");
        subtitle.setFont(Font.font("Arial", 15));
        subtitle.setTextFill(Color.web("#555"));

        // ---------- FIELDS ----------
        Label lblName = new Label("Full Name:");
        TextField tfName = new TextField();
        tfName.setPromptText("Enter your name");
        tfName.setMaxWidth(280);

        Label lblPass = new Label("Password:");
        PasswordField tfPass = new PasswordField();
        tfPass.setPromptText("Enter password");
        tfPass.setMaxWidth(280);

        Label lblGender = new Label("Gender:");
        ComboBox<String> cbGender = new ComboBox<>();
        cbGender.getItems().addAll("Male", "Female", "Other");
        cbGender.setPromptText("Select Gender");
        cbGender.setMaxWidth(280);

        Label lblAddress = new Label("Address:");
        TextField tfAddress = new TextField();
        tfAddress.setPromptText("Enter your address");
        tfAddress.setMaxWidth(280);

        Button btnSignup = new Button("Create Account");
        btnSignup.setStyle("-fx-background-color: #b8860b; -fx-text-fill: white; -fx-font-size: 15px;");
        btnSignup.setPrefWidth(280);

        Button btnBack = new Button("Back to Login");
        btnBack.setStyle("-fx-background-color: transparent; -fx-border-color: #b8860b; -fx-text-fill: #b8860b;");
        btnBack.setPrefWidth(280);

        // ---------- ACTIONS ----------
        btnSignup.setOnAction(e -> {
            String name = tfName.getText() == null ? "" : tfName.getText().trim();
            String password = tfPass.getText() == null ? "" : tfPass.getText().trim();
            String gender = cbGender.getValue();
            String address = tfAddress.getText() == null ? "" : tfAddress.getText().trim();

            if (name.isEmpty() || password.isEmpty() || gender == null || address.isEmpty()) {
                // ✅ owned popup (stays on same screen)
                PopupUtil.showWarn(stage, "Please fill in all fields.");
                return;
            }

            // Create customer user
            UserDAO dao = new UserDAO();
            String userCode = dao.generateUserCode("C"); // e.g. C123456
            User user = new User();
            user.setUserName(name);
            user.setPassword(password);
            user.setUserType("customer");
            user.setGender(gender);
            user.setAddress(address);
            user.setUserCode(userCode);

            boolean created = dao.insertUser(user);

            if (created) {
                // ✅ owned popup (stays on same screen)
                PopupUtil.showInfo(stage,
                        "✅ Account Created Successfully\n\n" +
                                "Your account has been created!\n\n" +
                                "Your User Code is: " + userCode + "\n\n" +
                                "Please use it to log in.");

                ScreenRouter.goToLogin(stage);
            } else {
                // ✅ owned popup (stays on same screen)
                PopupUtil.showError(stage, "Failed to create account. Try again later.");
            }
        });

        btnBack.setOnAction(e -> ScreenRouter.goToLogin(stage));

        // ---------- LAYOUT ----------
        VBox form = new VBox(12,
                title, subtitle,
                lblName, tfName,
                lblPass, tfPass,
                lblGender, cbGender,
                lblAddress, tfAddress,
                btnSignup, btnBack);

        form.setPadding(new Insets(30));
        form.setAlignment(Pos.CENTER);
        form.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 20; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);");

        StackPane root = new StackPane(form);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #fff8dc, #f5deb3);");
        StackPane.setAlignment(form, Pos.CENTER);

        ScreenRouter.replaceSceneContent(stage, root, 900, 600);
        stage.setTitle("Kanchan Cast — Sign Up");
        stage.show();
    }
}

package com.kanchancast.auth;

import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.dashboard.AdminDashboard;
import com.kanchancast.dashboard.CustomerDashboard;
import com.kanchancast.dashboard.EmployeeDashboard;
import com.kanchancast.dashboard.OwnerDashboard;
import com.kanchancast.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginScreen {

    private static final UserDAO userDAO = new UserDAO();

    public static void show(Stage stage) {
        System.out.println("LoginScreen.show()"); // marker

        Label title = new Label("Kanchan Cast – Login");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField code = new TextField();
        code.setPromptText("User Code (KC-..., optional)");

        TextField username = new TextField();
        username.setPromptText("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Button loginBtn = new Button("Log in");
        loginBtn.setDefaultButton(true);
        // Force visible style no matter what CSS does:
        loginBtn.setStyle("-fx-background-color: #2d7; -fx-text-fill: white; -fx-font-weight: bold;");

        Button signupBtn = new Button("Sign up");
        signupBtn.setStyle("-fx-background-color: #eee; -fx-text-fill: #333;");

        Label message = new Label();
        message.setStyle("-fx-text-fill: #b00;");

        HBox row = new HBox(10, loginBtn, signupBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, title, code, username, password, row, message);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 12; -fx-background-radius: 12;");

        BorderPane root = new BorderPane(box);
        BorderPane.setMargin(box, new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(#f7f7f7,#ececec);");

        Scene scene = new Scene(root, 520, 340);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Login");
        stage.show();

        // Actions
        loginBtn.setOnAction(e -> {
            message.setText("");
            String c  = code.getText() == null ? "" : code.getText().trim();
            String u  = username.getText() == null ? "" : username.getText().trim();
            String pw = password.getText() == null ? "" : password.getText();

            if (u.isEmpty() || pw.isEmpty()) {
                message.setText("Enter username and password (user code optional).");
                return;
            }

            try {
                Optional<User> maybe = c.isEmpty()
                        ? userDAO.authenticate(u, pw)
                        : userDAO.authenticateByCodeAndUsername(c, u, pw);

                if (maybe.isEmpty()) {
                    message.setText("Invalid credentials.");
                    System.out.println("[LOGIN FAIL] user=" + u + " code=" + c);
                    return;
                }

                User user = maybe.get();
                System.out.println("[LOGIN OK] id=" + user.getUserId() + " code=" + user.getUserCode() + " type=" + user.getUserType());

                switch (user.getUserType().toLowerCase()) {
                    case "customer" -> CustomerDashboard.show(stage, user);
                    case "employee" -> EmployeeDashboard.show(stage, user);
                    case "admin" -> AdminDashboard.show(stage, user);
                    case "owner"    -> OwnerDashboard.show(stage, user);
                    default -> message.setText("Unknown role: " + user.getUserType());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                message.setText("Login error. See console.");
            }
        });

        signupBtn.setOnAction(e -> SignupScreen.show(stage));
    }
}

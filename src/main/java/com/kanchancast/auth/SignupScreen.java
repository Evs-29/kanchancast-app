package com.kanchancast.auth;

import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;

public class SignupScreen {
    private static final UserDAO userDAO = new UserDAO();

    public static void show(Stage stage) {
        System.out.println("SignupScreen.show()"); // marker

        Label title = new Label("Create Customer Account");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextField username = new TextField();
        username.setPromptText("Username");

        PasswordField pw1 = new PasswordField();
        pw1.setPromptText("Password");

        PasswordField pw2 = new PasswordField();
        pw2.setPromptText("Confirm Password");

        TextField address = new TextField();
        address.setPromptText("Address (optional)");

        ComboBox<String> gender = new ComboBox<>();
        gender.getItems().addAll("Male","Female","Other");
        gender.setPromptText("Gender (optional)");

        Button createBtn = new Button("Create");
        createBtn.setStyle("-fx-background-color:#2d7; -fx-text-fill:white; -fx-font-weight:bold;");

        Button backBtn = new Button("Back to Login");
        backBtn.setStyle("-fx-background-color:#eee; -fx-text-fill:#333;");

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#b00;");

        HBox actions = new HBox(10, createBtn, backBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(10, title, username, pw1, pw2, address, gender, actions, msg);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color:white; -fx-border-color:#ddd; -fx-border-radius:12; -fx-background-radius:12;");

        BorderPane root = new BorderPane(form);
        BorderPane.setMargin(form, new Insets(30));
        root.setStyle("-fx-background-color: linear-gradient(#f7f7f7,#ececec);");

        Scene scene = new Scene(root, 560, 420);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Sign up");
        stage.show();

        createBtn.setOnAction(e -> {
            msg.setText("");
            String u = username.getText() == null ? "" : username.getText().trim();
            String p1 = pw1.getText() == null ? "" : pw1.getText();
            String p2 = pw2.getText() == null ? "" : pw2.getText();
            String addr = address.getText();
            String gen = gender.getValue();

            if (u.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
                msg.setText("Username and both passwords are required.");
                return;
            }
            if (!p1.equals(p2)) {
                msg.setText("Passwords do not match.");
                return;
            }

            try {
                Optional<User> created = userDAO.createCustomerReturn(u, p1, addr, gen);
                if (created.isEmpty()) {
                    msg.setText("Could not create account. Change username or try a stronger password.");
                } else {
                    User nu = created.get();
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Account created");
                    ok.setHeaderText("Customer created!");
                    ok.setContentText("Your User Code is:\n\n" + nu.getUserCode() + "\n\nSave it to log in.");
                    ok.showAndWait();
                    LoginScreen.show(stage);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText("Error creating account. See console.");
            }
        });

        backBtn.setOnAction(e -> LoginScreen.show(stage));
    }
}

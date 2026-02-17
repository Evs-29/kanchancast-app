package com.kanchancast.dialogs;

import com.jewelleryapp.dao.EmployeeDAO;
import com.kanchancast.auth.PasswordUtil;
import com.kanchancast.model.StaffRow;
import com.kanchancast.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Admin popup to view employee details and RESET password.
 * Note: hashed passwords cannot be reversed, so we never "show" the original password.
 */
public class EmployeeDetailsDialog {

    public static void show(Stage owner, EmployeeDAO employeeDAO, StaffRow row) {
        if (owner == null || employeeDAO == null || row == null) return;

        Optional<User> opt = employeeDAO.findEmployeeById(row.getUserId());
        User u = opt.orElseGet(() -> fallbackUser(row));

        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Employee Details");

        Label title = new Label(safe(u.getUserName(), "Employee"));
        title.setFont(Font.font("Segoe UI", 22));
        title.setStyle("-fx-font-weight: bold;");

        Label subtitle = new Label("User ID: " + u.getUserId());
        subtitle.setStyle("-fx-text-fill: #666666;");

        VBox header = new VBox(4, title, subtitle);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));

        int r = 0;
        addRow(grid, r++, "User Code", safe(u.getUserCode(), "N/A"));
        addRow(grid, r++, "Username", safe(u.getUserName(), safe(row.getUserName(), "N/A")));
        addRow(grid, r++, "Role", safe(u.getUserType(), "employee"));
        addRow(grid, r++, "Work Area", safe(u.getArea(), safe(row.getWorkArea(), "N/A")));
        addRow(grid, r++, "Gender", safe(u.getGender(), safe(row.getGender(), "N/A")));

        // ✅ NEW: Show DOB (if available)
        addRow(grid, r++, "Date of Birth", safe(u.getDob(), safe(row.getDob(), "N/A")));

        // Age is now calculated from DOB in EmployeeDAO whenever DOB exists,
        // otherwise falls back to stored age.
        addRow(grid, r++, "Age", String.valueOf((u.getAge() > 0) ? u.getAge() : row.getAge()));

        addRow(grid, r++, "Address", safe(u.getAddress(), safe(row.getAddress(), "N/A")));

        // Password section (masked) + reset button
        Label pwTitle = new Label("Password");
        pwTitle.setStyle("-fx-font-weight: bold;");
        Label pwMasked = new Label("•••••••• (hidden for security)");
        pwMasked.setStyle("-fx-text-fill: #222222;");
        Label pwNote = new Label(
                "Passwords are stored securely (hashed). The original password cannot be displayed.\n" +
                        "Use Reset Password to set a new one."
        );
        pwNote.setWrapText(true);
        pwNote.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        Button resetBtn = new Button("Reset Password");
        resetBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> handleReset(owner, employeeDAO, u));

        VBox pwBox = new VBox(8, pwTitle, pwMasked, pwNote, resetBtn);
        pwBox.setPadding(new Insets(12));
        pwBox.setStyle("""
            -fx-background-color: #fafafa;
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-border-color: #e7e7e7;
        """);

        Button closeBtn = new Button("Close");
        closeBtn.setDefaultButton(true);
        closeBtn.setStyle("""
            -fx-background-color: #b83b5e;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-cursor: hand;
        """);
        closeBtn.setOnAction(e -> dlg.close());

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(14, header, new Separator(), grid, pwBox, footer);
        root.setPadding(new Insets(18));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #fdfbfb, #ebedee);
            -fx-font-family: 'Segoe UI';
        """);

        dlg.setScene(new Scene(root, 640, 560));
        dlg.showAndWait();
    }

    private static void handleReset(Stage owner, EmployeeDAO dao, User u) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Reset Password");
        d.initOwner(owner);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        PasswordField p1 = new PasswordField();
        PasswordField p2 = new PasswordField();
        p1.setPromptText("New password");
        p2.setPromptText("Confirm new password");

        Label hint = new Label("Rule: 8+ characters, at least 1 letter and 1 number.");
        hint.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(8);
        gp.setPadding(new Insets(14));
        gp.addRow(0, new Label("New Password"), p1);
        gp.addRow(1, new Label("Confirm"), p2);
        gp.add(hint, 0, 2, 2, 1);

        d.getDialogPane().setContent(gp);

        Node okBtn = d.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ANY, ev -> {
            String a = p1.getText();
            String b = p2.getText();

            if (a == null || a.isBlank() || b == null || b.isBlank()) {
                showError("Password fields cannot be empty.");
                ev.consume();
                return;
            }
            if (!a.equals(b)) {
                showError("Passwords do not match.");
                ev.consume();
                return;
            }
            if (!PasswordUtil.isStrongEnough(a)) {
                showError("Password too weak. Use 8+ characters with at least 1 letter and 1 number.");
                ev.consume();
                return;
            }

            boolean ok = dao.updateEmployeePassword(u.getUserId(), a);
            if (!ok) {
                showError("Failed to reset password (check console).");
                ev.consume();
            }
        });

        d.showAndWait();
    }

    private static void addRow(GridPane grid, int row, String key, String value) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: #666666; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #222222;");
        v.setWrapText(true);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private static User fallbackUser(StaffRow row) {
        User tmp = new User();
        tmp.setUserId(row.getUserId());
        tmp.setUserName(row.getUserName());
        tmp.setGender(row.getGender());
        tmp.setDob(row.getDob()); // ✅ NEW
        tmp.setAge(row.getAge());
        tmp.setAddress(row.getAddress());
        tmp.setArea(row.getWorkArea());
        tmp.setUserType("employee");
        return tmp;
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}

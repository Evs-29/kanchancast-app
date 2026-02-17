package com.kanchancast.ui;

import com.jewelleryapp.dao.UserDAO;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.Period;

public class StaffFormDialog extends Dialog<Boolean> {

    private static final String[] AREAS = com.kanchancast.model.StageEnum.labels();

    public StaffFormDialog(Window owner, boolean allowElevatedRoles) {
        setTitle("Create Employee");
        if (owner != null) initOwner(owner);

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        ComboBox<String> role = new ComboBox<>();
        if (allowElevatedRoles) {
            role.getItems().addAll("employee", "admin", "owner");
        } else {
            role.getItems().add("employee");
        }
        role.setValue("employee");

        TextField address = new TextField();

        ComboBox<String> gender = new ComboBox<>();
        gender.getItems().addAll("M", "F", "Other");
        gender.setValue("M");

        ComboBox<String> area = new ComboBox<>();
        area.getItems().addAll(AREAS);
        area.setEditable(false);
        area.setValue(AREAS[0]);

        // ✅ DOB picker replaces age field
        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("YYYY-MM-DD");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.setPadding(new Insets(12));
        int r = 0;
        g.add(new Label("Username"), 0, r); g.add(username, 1, r++);
        g.add(new Label("Password"), 0, r); g.add(password, 1, r++);
        g.add(new Label("Role"), 0, r);     g.add(role, 1, r++);
        g.add(new Label("Address"), 0, r);  g.add(address, 1, r++);
        g.add(new Label("Gender"), 0, r);   g.add(gender, 1, r++);
        g.add(new Label("Work Area"), 0, r);g.add(area, 1, r++);
        g.add(new Label("Date of Birth"), 0, r); g.add(dobPicker, 1, r++);

        getDialogPane().setContent(g);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return Boolean.FALSE;

            String rRole = role.getValue();
            if (!allowElevatedRoles && !"employee".equalsIgnoreCase(rRole)) {
                new Alert(Alert.AlertType.ERROR, "Admins can create employees only.", ButtonType.OK).showAndWait();
                return Boolean.FALSE;
            }

            if (dobPicker.getValue() == null) {
                new Alert(Alert.AlertType.ERROR, "Date of Birth is required.", ButtonType.OK).showAndWait();
                return Boolean.FALSE;
            }

            LocalDate dob = dobPicker.getValue();
            LocalDate today = LocalDate.now();
            if (dob.isAfter(today)) {
                new Alert(Alert.AlertType.ERROR, "DOB cannot be in the future.", ButtonType.OK).showAndWait();
                return Boolean.FALSE;
            }

            int years = Period.between(dob, today).getYears();
            if (years < 10 || years > 120) {
                new Alert(Alert.AlertType.ERROR, "Please enter a realistic DOB (age 10 to 120).", ButtonType.OK).showAndWait();
                return Boolean.FALSE;
            }

            boolean ok = new UserDAO()
                    .createStaffReturn(username.getText().trim(),
                            password.getText(),
                            rRole,
                            address.getText().trim(),
                            gender.getValue(),
                            area.getValue())
                    .isPresent();

            // ✅ store dob after creating the user (since UserDAO createStaffReturn doesn't include it)
            if (ok) {
                tryUpdateDob(username.getText().trim(), dob.toString());
            }

            return ok;
        });
    }

    private void tryUpdateDob(String username, String dobIso) {
        final String SQL = "UPDATE users SET dob=? WHERE user_name=?";
        try (java.sql.Connection c = com.jewelleryapp.dao.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(SQL)) {
            ps.setString(1, dobIso);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (Exception ignore) {}
    }
}

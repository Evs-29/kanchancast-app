package com.kanchancast.ui;

import com.jewelleryapp.dao.UserDAO;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * If allowElevatedRoles=false (Admin use): role is locked to "employee" and area is required from fixed list.
 * If allowElevatedRoles=true (Owner use): role dropdown includes admin/owner/employee.
 */
public class StaffFormDialog extends Dialog<Boolean> {

    private static final String[] AREAS = {
            "Casting", "Mould", "Wax", "Setting", "Polish", "Rhodium",
            "Soldering", "Hallmark", "QC", "Packing", "Dispatch"
    };

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

        TextField age = new TextField();

        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        int r=0;
        g.add(new Label("Username"),0,r); g.add(username,1,r++);
        g.add(new Label("Password"),0,r); g.add(password,1,r++);
        g.add(new Label("Role"),0,r);     g.add(role,1,r++);
        g.add(new Label("Address"),0,r);  g.add(address,1,r++);
        g.add(new Label("Gender"),0,r);   g.add(gender,1,r++);
        g.add(new Label("Work Area"),0,r);g.add(area,1,r++);
        g.add(new Label("Age"),0,r);      g.add(age,1,r++);

        getDialogPane().setContent(g);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return Boolean.FALSE;
            String rRole = role.getValue();
            if (!allowElevatedRoles && !"employee".equalsIgnoreCase(rRole)) {
                new Alert(Alert.AlertType.ERROR, "Admins can create employees only.", ButtonType.OK).showAndWait();
                return Boolean.FALSE;
            }
            // Persist via DAO
            boolean ok = new UserDAO()
                    .createStaffReturn(username.getText().trim(),
                            password.getText(),
                            rRole,
                            address.getText().trim(),
                            gender.getValue(),
                            area.getValue())
                    .isPresent();
            // If you store age in a separate column, update it here:
            if (ok) {
                try {
                    int a = Integer.parseInt(age.getText().trim());
                    tryUpdateAge(username.getText().trim(), a);
                } catch (NumberFormatException ignore) {}
            }
            return ok;
        });
    }

    // Optional helper: set age after insert, if you keep it in users.age
    private void tryUpdateAge(String username, int age) {
        final String SQL = "UPDATE users SET age=? WHERE user_name=?";
        try (java.sql.Connection c = com.jewelleryapp.dao.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(SQL)) {
            ps.setInt(1, age);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (Exception ignore) {}
    }
}

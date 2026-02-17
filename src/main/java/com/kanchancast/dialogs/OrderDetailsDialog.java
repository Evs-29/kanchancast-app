package com.kanchancast.dialogs;

import com.jewelleryapp.dao.EmployeeDAO;
import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StaffRow;
import com.kanchancast.model.StageEnum;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assign Employees dialog
 * - Uses the same 11 official stages everywhere (StageEnum.labels()).
 * - Filters employees by their work_area to match the stage.
 * - Saves assignments into order_stages.
 */
public class OrderDetailsDialog {

    // ✅ One canonical list for this dialog (same order as customer tracking)
    private static final String[] WORK_AREAS = StageEnum.labels();

    public static void show(Window owner, OrderDAO orderDAO, EmployeeDAO employeeDAO, OrderSummary order) {
        if (order == null)
            return;

        Stage dlg = new Stage();
        if (owner != null) {
            dlg.initOwner(owner);
            dlg.initModality(Modality.WINDOW_MODAL);
        }
        dlg.setTitle("Assign employees for order: " + order.getProductName());

        // Fetch employees + existing assignments
        List<StaffRow> allEmployees = employeeDAO.listAll();
        Map<String, Integer> existingAssignments = orderDAO.getAssignedEmployeeIdsForOrder(order.getOrderId());

        // --- Build Grid ---
        GridPane gp = new GridPane();
        gp.setHgap(20);
        gp.setVgap(14);
        gp.setPadding(new Insets(20, 30, 20, 30));

        Map<String, ComboBox<StaffRow>> selectionMap = new HashMap<>();
        int row = 0;

        for (String area : WORK_AREAS) {
            Label lbl = new Label(area + ":");
            lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #333333;");

            // Filter employees by work area (must match the same stage name)
            List<StaffRow> filtered = allEmployees.stream()
                    .filter(emp -> emp.getWorkArea() != null && emp.getWorkArea().equalsIgnoreCase(area))
                    .collect(Collectors.toList());

            ComboBox<StaffRow> combo = new ComboBox<>(FXCollections.observableArrayList(filtered));
            combo.setPromptText(filtered.isEmpty() ? "No employees available" : "Select employee");
            combo.setPrefWidth(280);

            // Dropdown formatting
            combo.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(StaffRow item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getUserName());
                }
            });

            combo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(StaffRow item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Select employee" : item.getUserName());
                }
            });

            // Pre-fill existing assignment (if the assigned employee is in this filtered
            // list)
            Integer assignedId = existingAssignments.get(area);
            if (assignedId != null) {
                filtered.stream()
                        .filter(e -> e.getUserId() == assignedId)
                        .findFirst()
                        .ifPresent(combo::setValue);
            }

            gp.add(lbl, 0, row);
            gp.add(combo, 1, row);

            selectionMap.put(area, combo);
            row++;
        }

        // Scroll
        ScrollPane scrollPane = new ScrollPane(gp);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // --- Buttons ---
        Button btnSave = new Button("💾 Save Assignments");
        btnSave.setStyle("""
                    -fx-background-color: #0078D7;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-background-radius: 8;
                    -fx-padding: 6 14;
                """);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("""
                    -fx-background-color: #e0e0e0;
                    -fx-text-fill: #333;
                    -fx-font-weight: bold;
                    -fx-background-radius: 8;
                    -fx-padding: 6 14;
                """);
        btnCancel.setOnAction(e -> dlg.close());

        btnSave.setOnAction(e -> {
            boolean anyAssigned = false;

            for (Map.Entry<String, ComboBox<StaffRow>> entry : selectionMap.entrySet()) {
                String area = entry.getKey();
                StaffRow emp = entry.getValue().getValue();

                // If user selected an employee, save it
                if (emp != null) {
                    boolean ok = orderDAO.assignEmployeeToStage(order.getOrderId(), area, emp.getUserId());
                    if (ok)
                        anyAssigned = true;
                }
            }

            if (anyAssigned) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "✅ Employee assignments saved successfully!");
                a.initOwner(dlg); // ✅ Fix: Attached to dialog
                a.showAndWait();
                dlg.close();
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING,
                        "⚠️ No employees were assigned. Please select before saving.");
                a.initOwner(dlg); // ✅ Fix: Attached to dialog
                a.showAndWait();
            }
        });

        // Footer
        HBox footer = new HBox(10, btnSave, btnCancel);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 20, 15, 20));

        // Root layout
        BorderPane root = new BorderPane();

        Label title = new Label("Assign employees for order: " + order.getProductName());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox top = new VBox(title);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(12, 0, 10, 25));

        root.setTop(top);
        root.setCenter(scrollPane);
        root.setBottom(footer);

        root.setStyle("""
                    -fx-background-color: linear-gradient(to bottom right, #fafafa, #f1f1f1);
                    -fx-font-family: 'Segoe UI', sans-serif;
                    -fx-font-size: 13px;
                """);

        Scene scene = new Scene(root, 560, 600);
        dlg.setScene(scene);
        dlg.showAndWait();
    }
}

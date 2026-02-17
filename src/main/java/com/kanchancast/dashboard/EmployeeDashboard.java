package com.kanchancast.dashboard;

import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.AssignedTask;
import com.kanchancast.model.User;
import com.kanchancast.auth.LoginScreen;
import com.kanchancast.ui.PopupUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Employee Dashboard
 * --------------------
 * Shows tasks assigned to a logged-in employee.
 * Allows them to mark tasks as completed or not completed.
 * Adds logout button to return to login screen.
 */
public final class EmployeeDashboard {

    private EmployeeDashboard() {}

    public static void show(Stage stage, User employee) {
        if (employee == null) {
            PopupUtil.showError(stage, "No employee data found. Please log in again.");
            return;
        }

        // ---- DAO + Table setup ----
        OrderDAO orderDAO = new OrderDAO();
        TableView<AssignedTask> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AssignedTask, Number> cOrder = new TableColumn<>("Order #");
        cOrder.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getOrderId()));

        TableColumn<AssignedTask, String> cProduct = new TableColumn<>("Product");
        cProduct.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProductName()));

        TableColumn<AssignedTask, String> cCustomer = new TableColumn<>("Customer");
        cCustomer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCustomerName()));

        TableColumn<AssignedTask, String> cStage = new TableColumn<>("Stage");
        cStage.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStage()));

        TableColumn<AssignedTask, String> cDone = new TableColumn<>("Completed");
        cDone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isCompleted() ? "Yes" : "No"));

        tv.getColumns().addAll(cOrder, cProduct, cCustomer, cStage, cDone);

        // ---- Loader ----
        Runnable reload = () -> {
            List<AssignedTask> items = orderDAO.listTasksAssignedToEmployee(employee.getUserId());
            tv.setItems(FXCollections.observableArrayList(items));
        };

        // ---- Buttons ----
        Button refresh = new Button("🔄 Refresh");
        Button markDone = new Button("✅ Mark Done");
        Button markNotDone = new Button("❌ Mark Not Done");
        Button logout = new Button("🚪 Logout");

        // Button styling
        refresh.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold;");
        markDone.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        markNotDone.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        logout.setStyle("-fx-background-color: #ff7f50; -fx-text-fill: white; -fx-font-weight: bold;");

        refresh.setOnAction(e -> reload.run());

        markDone.setOnAction(e -> {
            AssignedTask sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) {
                PopupUtil.showWarn(stage, "Please select a task first.");
                return;
            }

            boolean okConfirm = PopupUtil.confirm(stage, "Confirm Completion", "Mark this stage as completed?");
            if (!okConfirm) return;

            boolean ok = orderDAO.markStageAsCompleted(sel.getOrderId(), sel.getStage());
            if (ok) {
                PopupUtil.showInfo(stage, "✅ Stage marked as completed!");
                reload.run();
            } else {
                PopupUtil.showError(stage, "⚠️ Could not update stage.");
            }
        });

        markNotDone.setOnAction(e -> {
            AssignedTask sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) {
                PopupUtil.showWarn(stage, "Please select a task first.");
                return;
            }

            boolean okConfirm = PopupUtil.confirm(stage, "Confirm Change", "Mark this stage as not completed?");
            if (!okConfirm) return;

            boolean ok = orderDAO.markStageAsIncomplete(sel.getOrderId(), sel.getStage());
            if (ok) {
                PopupUtil.showInfo(stage, "❌ Stage marked as not completed!");
                reload.run();
            } else {
                PopupUtil.showError(stage, "⚠️ Could not update stage.");
            }
        });

        // ---- Logout Button Action ----
        logout.setOnAction(e -> {
            boolean okConfirm = PopupUtil.confirm(stage, "Confirm Logout", "Are you sure you want to logout?");
            if (!okConfirm) return;

            LoginScreen.show(stage); // Redirect to login
        });

        // ---- Header section ----
        Label lblTitle = new Label("Employee Dashboard");
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #343a40;");

        Label lblName = new Label("👤 " + employee.getUserName() + "  (" + employee.getArea() + ")");
        lblName.setStyle("-fx-font-size: 14px; -fx-text-fill: #495057;");

        HBox headerLeft = new HBox(10, lblTitle, lblName);
        headerLeft.setAlignment(Pos.CENTER_LEFT);

        HBox headerRight = new HBox(logout);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        BorderPane header = new BorderPane();
        header.setLeft(headerLeft);
        header.setRight(headerRight);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #ffffff, #e9ecef);");

        // ---- Toolbar ----
        HBox bar = new HBox(12, new Label("Assigned Tasks:"), refresh, markDone, markNotDone);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));

        VBox root = new VBox(10, header, bar, tv);
        root.setPadding(new Insets(10));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #f8f9fa, #dee2e6);
            -fx-font-family: 'Segoe UI', sans-serif;
            -fx-font-size: 13px;
        """);

        // ---- Initial load ----
        reload.run();

        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Employee Dashboard");
        stage.show();
    }
}

package com.kanchancast.dashboard;

import javafx.beans.property.ReadOnlyObjectWrapper;
import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.AssignedTask;
import com.kanchancast.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public final class EmployeeDashboard {

    private EmployeeDashboard() {}

    public static void show(Stage stage, User employee) {
        if (employee == null) {
            new Alert(Alert.AlertType.ERROR, "No employee data found. Please log in again.").showAndWait();
            return;
        }

        // ---- Table setup ----
        TableView<AssignedTask> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AssignedTask, Number> cOrder = new TableColumn<>("Order #");
        cOrder.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getOrderId()));

        TableColumn<AssignedTask, String> cStage = new TableColumn<>("Stage");
        cStage.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStage()));

        TableColumn<AssignedTask, String> cProduct = new TableColumn<>("Product");
        cProduct.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getProductName()));

        TableColumn<AssignedTask, String> cCustomer = new TableColumn<>("Customer");
        cCustomer.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCustomerName()));

        TableColumn<AssignedTask, String> cDone = new TableColumn<>("Completed");
        cDone.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().isCompleted() ? "Yes" : "No"));

        tv.getColumns().addAll(cOrder, cProduct, cCustomer, cStage, cDone);

        // ---- DAO ----
        OrderDAO orderDAO = new OrderDAO();

        // ---- Loader ----
        Runnable reload = () -> {
            List<AssignedTask> items = orderDAO.listTasksAssignedToEmployee(employee.getUserId());
            tv.setItems(FXCollections.observableArrayList(items));
        };

        // ---- Buttons ----
        Button refresh = new Button("🔄 Refresh");
        refresh.setOnAction(e -> reload.run());

        Button markDone = new Button("✅ Mark Done");
        markDone.setOnAction(e -> {
            AssignedTask sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a task first.").showAndWait();
                return;
            }
            boolean ok = orderDAO.setStageCompletion(sel.getOrderId(), sel.getStage(), true);
            if (ok) {
                new Alert(Alert.AlertType.INFORMATION, "Marked as completed!").showAndWait();
                reload.run();
            } else {
                new Alert(Alert.AlertType.ERROR, "Could not update task.").showAndWait();
            }
        });

        Button markNotDone = new Button("❌ Mark Not Done");
        markNotDone.setOnAction(e -> {
            AssignedTask sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a task first.").showAndWait();
                return;
            }
            boolean ok = orderDAO.setStageCompletion(sel.getOrderId(), sel.getStage(), false);
            if (ok) {
                new Alert(Alert.AlertType.INFORMATION, "Marked as not completed!").showAndWait();
                reload.run();
            } else {
                new Alert(Alert.AlertType.ERROR, "Could not update task.").showAndWait();
            }
        });

        // ---- Layout ----
        HBox bar = new HBox(10, new Label("Assigned Tasks:"), refresh, markDone, markNotDone);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, bar, tv);
        root.setPadding(new Insets(10));

        // ---- Initial load ----
        reload.run();

        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Employee Dashboard");
        stage.show();
    }
}

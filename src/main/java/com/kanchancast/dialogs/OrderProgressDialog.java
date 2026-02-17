package com.kanchancast.dialogs;

import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StageRow;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Admin "View" popup for an order.
 * Shows each production stage, assigned employee, and completion state.
 */
public class OrderProgressDialog {

    public static void show(Stage owner, OrderDAO orderDAO, OrderSummary order) {
        if (owner == null || orderDAO == null || order == null) return;

        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Order Progress — #" + order.getOrderId());

        Label title = new Label("Order #" + order.getOrderId() + " — " + safe(order.getProductName()));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitle = new Label(
                "Customer: " + safe(order.getCustomerName())
                        + "   |   Status: " + safe(order.getStatus())
                        + "   |   Progress: " + order.getProgressPercent() + "%"
        );
        subtitle.setStyle("-fx-text-fill: #666666;");

        TableView<StageRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<StageRow, String> cStage = new TableColumn<>("Stage");
        cStage.setCellValueFactory(v -> v.getValue().stageProperty());

        TableColumn<StageRow, String> cEmp = new TableColumn<>("Assigned Employee");
        cEmp.setCellValueFactory(v -> v.getValue().employeeNameProperty());

        TableColumn<StageRow, String> cDone = new TableColumn<>("Completed");
        cDone.setCellValueFactory(v -> v.getValue().completedTextProperty());

        table.getColumns().addAll(cStage, cEmp, cDone);

        Runnable refresh = () -> {
            List<StageRow> rows = orderDAO.listStagesForOrder(order.getOrderId());
            table.setItems(FXCollections.observableArrayList(rows));
        };
        refresh.run();

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refresh.run());

        Button closeBtn = new Button("Close");
        closeBtn.setDefaultButton(true);
        closeBtn.setOnAction(e -> dlg.close());

        HBox actions = new HBox(10, refreshBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, title, subtitle, table, actions);
        root.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);

        dlg.setScene(new Scene(root, 860, 520));
        dlg.showAndWait();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

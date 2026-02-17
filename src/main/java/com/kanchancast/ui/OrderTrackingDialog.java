package com.kanchancast.ui;

import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StageRow;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OrderTrackingDialog extends Stage {

    public OrderTrackingDialog(javafx.stage.Window owner, OrderSummary summary, OrderDAO orderDAO) {
        setTitle("Order Tracking — #" + summary.getOrderId());

        TableView<StageRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StageRow, String> stageCol = new TableColumn<>("Stage");
        stageCol.setCellValueFactory(v -> v.getValue().stageProperty());

        TableColumn<StageRow, String> completedCol = new TableColumn<>("Completed");
        completedCol.setCellValueFactory(v -> v.getValue().completedTextProperty());

        table.getColumns().addAll(stageCol, completedCol);

        // Customer should always see all 11 stages, even if no one is assigned.
        table.setItems(FXCollections.observableArrayList(
                orderDAO.listStagesForCustomerTracking(summary.getOrderId())
        ));

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> close());

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, table, footer);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 520, 420);
        UIKit.apply(scene);
        setScene(scene);

        if (owner != null) initOwner(owner);
    }
}

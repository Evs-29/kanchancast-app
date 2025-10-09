package com.kanchancast.ui;

import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StageRow;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class OrderTrackingDialog extends Stage {
    public OrderTrackingDialog(javafx.stage.Window owner, OrderSummary summary, OrderDAO orderDAO) {
        setTitle("Order Tracking — #" + summary.getOrderId());

        TableView<StageRow> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StageRow, String> cStage = new TableColumn<>("Stage");
        cStage.setCellValueFactory(v -> v.getValue().stageProperty());

        TableColumn<StageRow, String> cEmp = new TableColumn<>("Assigned to");
        cEmp.setCellValueFactory(v -> v.getValue().employeeNameProperty());

        TableColumn<StageRow, String> cDone = new TableColumn<>("Completed");
        cDone.setCellValueFactory(v -> v.getValue().completedTextProperty());

        tv.getColumns().addAll(cStage, cEmp, cDone);

        tv.setItems(FXCollections.observableArrayList(
                orderDAO.listStagesForOrder(summary.getOrderId())
        ));


        Button close = new Button("Close");
        close.setOnAction(e -> close());
        HBox foot = new HBox(close);
        foot.setAlignment(Pos.CENTER_RIGHT);

        var root = new javafx.scene.layout.VBox(8, tv, foot);
        root.setPadding(new javafx.geometry.Insets(12));
        Scene scene = new Scene(root, 520, 420);
        UIKit.apply(scene);
        setScene(scene);
        if (owner != null) initOwner(owner);
    }
}

package com.kanchancast.dialogs;

import com.kanchancast.model.StageRow;
import com.kanchancast.ui.UIKit;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * Simple read-only dialog that shows an order's production stages.
 * Works with StageRow that has JavaFX properties: stage, employeeName, completedText.
 */
public class OrderDetailsDialog {

    /**
     * Open a modal window showing the stage rows for an order.
     *
     * @param owner       parent window (can be null)
     * @param orderId     order id
     * @param productName product name to show in title
     * @param rows        rows to display (usually from OrderDAO.listStagesForOrder)
     */
    public static void open(Window owner, int orderId, String productName, List<StageRow> rows) {
        Stage dlg = new Stage();
        if (owner != null) {
            dlg.initOwner(owner);
            dlg.initModality(Modality.WINDOW_MODAL);
        }
        dlg.setTitle("Order #" + orderId + " — " + productName);

        // Table
        TableView<StageRow> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StageRow, String> cStage = new TableColumn<>("Stage");
        // StageRow has stageProperty(), so "stage" works with PropertyValueFactory
        cStage.setCellValueFactory(new PropertyValueFactory<>("stage"));
        cStage.setMinWidth(200);

        TableColumn<StageRow, String> cEmp = new TableColumn<>("Assigned Employee");
        // StageRow has employeeNameProperty()
        cEmp.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        TableColumn<StageRow, String> cDone = new TableColumn<>("Completed");
        // Map completedText ("Yes"/"No"/"In Progress") to ✓ / ✗ for nicer UI
        cDone.setCellValueFactory(v ->
                new SimpleStringProperty("Yes".equalsIgnoreCase(v.getValue().getCompletedText()) ? "✓" : "✗"));
        cDone.setMaxWidth(120);

        tv.getColumns().setAll(cStage, cEmp, cDone);
        tv.setItems(FXCollections.observableArrayList(rows));

        // Top title
        Label title = new Label("Order #" + orderId + " — " + productName);
        title.getStyleClass().add("title");
        VBox top = new VBox(6, title);
        top.setPadding(new Insets(10, 10, 0, 10));

        // Footer with Close button
        Button close = new Button("Close");
        close.setOnAction(e -> dlg.close());
        HBox foot = new HBox(close);
        foot.setAlignment(Pos.CENTER_RIGHT);
        foot.setPadding(new Insets(8, 10, 10, 10));

        // Root
        BorderPane root = new BorderPane(tv, top, null, foot, null);
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root, 560, 440);
        UIKit.apply(scene);

        dlg.setScene(scene);
        dlg.showAndWait();
    }
}

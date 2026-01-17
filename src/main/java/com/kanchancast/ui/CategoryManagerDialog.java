package com.kanchancast.ui;

import com.jewelleryapp.dao.ProductDAO;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Simple dialog to manage product categories.
 * Allows viewing, adding, and deleting categories with basic validation.
 */
public class CategoryManagerDialog {

    public static void show(Stage owner, ProductDAO productDAO) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Manage Product Categories");

        // --- Category Table ---
        TableView<String> table = new TableView<>();
        TableColumn<String, String> nameCol = new TableColumn<>("Category Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        nameCol.setPrefWidth(250);
        table.getColumns().add(nameCol);

        refreshTable(table, productDAO);

        // --- Input Fields ---
        TextField tfNewCat = new TextField();
        tfNewCat.setPromptText("Enter new category name");

        Button btnAdd = new Button("➕ Add Category");
        Button btnDelete = new Button("🗑️ Delete Selected");
        Button btnClose = new Button("❌ Close");

        btnAdd.setOnAction(e -> {
            String newCat = tfNewCat.getText().trim();

            if (newCat.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Category name cannot be empty.");
                return;
            }

            // ✅ Prevent duplicates
            List<String> existing = productDAO.listAllCategories();
            if (existing.stream().anyMatch(c -> c.equalsIgnoreCase(newCat))) {
                showAlert(Alert.AlertType.WARNING, "Duplicate Category", "This category already exists.");
                return;
            }

            boolean ok = productDAO.addCategory(newCat);
            if (ok) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Category added successfully!");
                tfNewCat.clear();
                refreshTable(table, productDAO);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not add category.");
            }
        });

        btnDelete.setOnAction(e -> {
            String selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a category to delete.");
                return;
            }

            // ✅ Confirm before deleting
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete the category: " + selected + "?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm Deletion");
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    boolean ok = productDAO.deleteCategory(selected);
                    if (ok) {
                        showAlert(Alert.AlertType.INFORMATION, "Deleted", "Category deleted successfully!");
                        refreshTable(table, productDAO);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete category. Check DB constraints.");
                    }
                }
            });
        });

        btnClose.setOnAction(e -> dlg.close());

        // --- Layout ---
        GridPane inputBox = new GridPane();
        inputBox.setHgap(10);
        inputBox.setVgap(8);
        inputBox.setPadding(new Insets(10));
        inputBox.addRow(0, new Label("New Category:"), tfNewCat, btnAdd);

        HBox footer = new HBox(10, btnDelete, btnClose);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(inputBox);
        root.setCenter(table);
        root.setBottom(footer);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 400, 400);
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    // Helper: refresh table contents
    private static void refreshTable(TableView<String> table, ProductDAO productDAO) {
        table.setItems(FXCollections.observableArrayList(productDAO.listAllCategories()));
    }

    // Helper: show alert
    private static void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }
}

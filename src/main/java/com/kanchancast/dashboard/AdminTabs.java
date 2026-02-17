package com.kanchancast.dashboard;

import com.kanchancast.ui.PopupUtil;
import com.kanchancast.dialogs.ProductDetailsDialog;
import com.kanchancast.auth.PasswordUtil;
import com.kanchancast.dialogs.EmployeeDetailsDialog;
import com.kanchancast.dialogs.OrderDetailsDialog;
import com.kanchancast.dialogs.OrderProgressDialog;
import com.jewelleryapp.dao.EmployeeDAO;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.Product;
import com.kanchancast.model.StaffRow;
import com.kanchancast.ui.CategoryManagerDialog;
import com.kanchancast.ui.ImageUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;

public class AdminTabs {

    public static TabPane buildTabs(Stage stage,
            ProductDAO productDAO,
            OrderDAO orderDAO,
            EmployeeDAO employeeDAO,
            Runnable onDataChanged) {

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ==========================
        // ===== PRODUCTS TAB =====
        // ==========================
        TableView<Product> productTable = new TableView<>();
        // ✅ FIX: Use ALL_COLUMNS policy so description doesn't eat everything.
        // Columns will share space, users can resize or scroll if needed.
        // ✅ FIX: Use FLEX_LAST_COLUMN so Description takes remaining space
        // and set reasonable initial widths for other columns.
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Product, String> pimg = new TableColumn<>("Image");
        pimg.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImagePath()));
        pimg.setPrefWidth(120);
        pimg.setMinWidth(120);
        pimg.setMaxWidth(140);
        pimg.setSortable(false);

        pimg.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                ImageView img = ImageUtil.getProductImage(path, 95, 60);
                setGraphic(img);
            }
        });

        TableColumn<Product, Number> pid = new TableColumn<>("Product ID");
        pid.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getProductId()));
        pid.setMinWidth(80);
        pid.setPrefWidth(100);

        TableColumn<Product, String> pname = new TableColumn<>("Product Name");
        pname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        pname.setMinWidth(120);
        pname.setPrefWidth(180);

        TableColumn<Product, String> ptype = new TableColumn<>("Category");
        ptype.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        ptype.setMinWidth(100);
        ptype.setPrefWidth(120);

        TableColumn<Product, String> pdesc = new TableColumn<>("Description");
        pdesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        productTable.getColumns().addAll(pimg, pid, pname, ptype, pdesc);
        productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));

        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2 && event.isPrimaryButtonDown()) {
                    ProductDetailsDialog.show(stage, row.getItem());
                }
            });
            return row;
        });

        Button btnAddProduct = new Button("➕ Create Product");
        Button btnViewProduct = new Button("👁 View");
        Button btnRefreshProduct = new Button("🔄 Refresh");
        Button btnDeleteProduct = new Button("🗑️ Delete");
        Button btnManageCategories = new Button("⚙️ Manage Categories");

        btnAddProduct.setOnAction(e -> {
            try {
                com.kanchancast.ui.ProductFormDialog.show(stage); // validation handled inside ProductFormDialog
                productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));
                fireRefresh(onDataChanged);
            } catch (Exception ex) {
                PopupUtil.showError(stage, "Error opening product dialog: " + ex.getMessage());
            }
        });

        btnViewProduct.setOnAction(e -> {
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Please select a product to view.");
                return;
            }
            ProductDetailsDialog.show(stage, selected);
        });

        btnRefreshProduct.setOnAction(e -> {
            productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));
            fireRefresh(onDataChanged); // refresh KPIs/charts too
        });

        btnDeleteProduct.setOnAction(e -> {
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Please select a product to delete.");
                return;
            }
            if (confirm(stage, "Delete Product", "Are you sure you want to delete " + selected.getName() + "?")) {
                boolean deleted = productDAO.deleteProduct(selected.getProductId());
                if (!deleted) {
                    PopupUtil.showError(stage,
                            "Could not delete this product.\n\n" +
                                    "Most common reason: there are existing orders linked to this product.\n" +
                                    "Delete those orders first (Orders tab), then try again.");
                    return;
                }
                productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));
                fireRefresh(onDataChanged);
            }
        });

        btnManageCategories.setOnAction(e -> {
            try {
                CategoryManagerDialog.show(stage, productDAO);
                productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));
                fireRefresh(onDataChanged);
            } catch (Exception ex) {
                PopupUtil.showError(stage, "Error opening category manager: " + ex.getMessage());
            }
        });

        HBox productBtns = new HBox(10, btnAddProduct, btnViewProduct, btnRefreshProduct, btnDeleteProduct,
                btnManageCategories);
        VBox productBox = new VBox(10, new Label("All Products"), productBtns, productTable);
        productBox.setPadding(new Insets(10));
        VBox.setVgrow(productTable, Priority.ALWAYS);

        Tab productTab = new Tab("Products", productBox);

        // ==========================
        // ===== ORDERS TAB =====
        // ==========================
        TableView<OrderSummary> orderTable = new TableView<>();
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<OrderSummary, Number> oid = new TableColumn<>("Order ID");
        oid.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getOrderId()));

        TableColumn<OrderSummary, String> ocust = new TableColumn<>("Customer");
        ocust.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));

        TableColumn<OrderSummary, String> oprod = new TableColumn<>("Product");
        oprod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));

        TableColumn<OrderSummary, String> odate = new TableColumn<>("Date Ordered");
        odate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateOrdered()));

        TableColumn<OrderSummary, String> odel = new TableColumn<>("Delivery Date");
        odel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDeliveryDate()));

        TableColumn<OrderSummary, String> ostat = new TableColumn<>("Status");
        ostat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        orderTable.getColumns().addAll(oid, ocust, oprod, odate, odel, ostat);
        orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()));

        // ✅ Double-click order row to view stage-by-stage progress
        orderTable.setRowFactory(tv -> {
            TableRow<OrderSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2 && event.isPrimaryButtonDown()) {
                    OrderProgressDialog.show(stage, orderDAO, row.getItem());
                }
            });
            return row;
        });

        Button btnAssign = new Button("👷 Assign Employees");
        Button btnViewOrder = new Button("👁 View");
        Button btnDeleteOrder = new Button("🗑️ Delete Order");
        Button btnRefreshOrders = new Button("🔄 Refresh Orders");

        btnAssign.setOnAction(e -> {
            OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Please select an order to assign.");
                return;
            }
            OrderDetailsDialog.show(stage, orderDAO, employeeDAO, selected);
            orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()));
            fireRefresh(onDataChanged);
        });

        btnViewOrder.setOnAction(e -> {
            OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Please select an order to view.");
                return;
            }
            OrderProgressDialog.show(stage, orderDAO, selected);
        });

        btnDeleteOrder.setOnAction(e -> {
            OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Please select an order to delete.");
                return;
            }
            boolean ok = confirm(
                    stage,
                    "Delete Order",
                    "Delete Order #" + selected.getOrderId() + " (" + selected.getProductName()
                            + ")?\n\nThis will also remove its stage tracking.");

            if (ok) {
                boolean deleted = orderDAO.deleteOrder(selected.getOrderId());
                if (!deleted) {
                    PopupUtil.showError(stage, "Failed to delete order. Check console.");
                }
                orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()));
                fireRefresh(onDataChanged);
            }
        });

        btnRefreshOrders.setOnAction(e -> {
            orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()));
            fireRefresh(onDataChanged); // refresh KPIs/charts too
        });

        HBox orderBtns = new HBox(10, btnAssign, btnViewOrder, btnDeleteOrder, btnRefreshOrders);
        VBox orderBox = new VBox(10, new Label("All Orders"), orderBtns, orderTable);
        orderBox.setPadding(new Insets(10));
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        Tab orderTab = new Tab("Orders", orderBox);

        // ==========================
        // ===== EMPLOYEES TAB =====
        // ==========================
        TableView<StaffRow> empTable = new TableView<>();
        empTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<StaffRow, Number> eid = new TableColumn<>("User ID");
        eid.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()));

        TableColumn<StaffRow, String> ename = new TableColumn<>("Username");
        ename.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserName()));

        TableColumn<StaffRow, String> earea = new TableColumn<>("Work Area");
        earea.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWorkArea()));

        TableColumn<StaffRow, String> egender = new TableColumn<>("Gender");
        egender.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGender()));

        TableColumn<StaffRow, Number> eage = new TableColumn<>("Age");
        eage.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAge()));

        TableColumn<StaffRow, String> eaddr = new TableColumn<>("Address");
        eaddr.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));

        TableColumn<StaffRow, Number> eactive = new TableColumn<>("Products Assigned");
        eactive.setCellValueFactory(c -> {
            int count = employeeDAO.countActiveProductsForEmployee(c.getValue().getUserId());
            return new SimpleIntegerProperty(count);
        });

        empTable.getColumns().addAll(eid, ename, earea, egender, eage, eaddr, eactive);
        empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));

        Button btnAddEmp = new Button("➕ Create Employee");
        Button btnViewEmp = new Button("👁 View");
        Button btnDelEmp = new Button("🗑️ Delete Employee");
        Button btnRefreshEmp = new Button("🔄 Refresh");

        empTable.setRowFactory(tv -> {
            TableRow<StaffRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2 && event.isPrimaryButtonDown()) {
                    EmployeeDetailsDialog.show(stage, employeeDAO, row.getItem()); // reset password is inside this
                                                                                   // dialog
                }
            });
            return row;
        });

        btnViewEmp.setOnAction(e -> {
            StaffRow selected = empTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Please select an employee to view.");
                return;
            }
            EmployeeDetailsDialog.show(stage, employeeDAO, selected);
        });

        // ✅ Create Employee with owner+modal so it stays on same screen
        btnAddEmp.setOnAction(e -> {
            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Create New Employee");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            // ✅ THIS is the popup fix:
            PopupUtil.prepareDialog(stage, dlg);

            GridPane gp = new GridPane();
            gp.setHgap(10);
            gp.setVgap(8);
            gp.setPadding(new Insets(20, 25, 20, 25));

            TextField tfName = new TextField();
            PasswordField tfPass = new PasswordField();
            tfPass.setText("password123");

            ComboBox<String> tfGender = new ComboBox<>(FXCollections.observableArrayList("Male", "Female"));
            tfGender.setValue("Male");

            TextField tfAddr = new TextField();

            DatePicker dpDob = new DatePicker();
            dpDob.setPromptText("YYYY-MM-DD");

            ComboBox<String> tfArea = new ComboBox<>(FXCollections.observableArrayList(
                    com.kanchancast.model.StageEnum.labels()));
            tfArea.setValue(com.kanchancast.model.StageEnum.labels()[0]);

            int r = 0;
            gp.addRow(r++, new Label("Username:"), tfName);
            gp.addRow(r++, new Label("Password:"), tfPass);
            gp.addRow(r++, new Label("Gender:"), tfGender);
            gp.addRow(r++, new Label("Date of Birth:"), dpDob);
            gp.addRow(r++, new Label("Address:"), tfAddr);
            gp.addRow(r++, new Label("Work Area:"), tfArea);

            Label hint = new Label("Password rule: 8+ chars, at least 1 letter and 1 number.");
            hint.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
            gp.add(hint, 0, r, 2, 1);

            dlg.getDialogPane().setContent(gp);

            Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
            okBtn.addEventFilter(javafx.event.ActionEvent.ANY, ev -> {
                String name = tfName.getText() == null ? "" : tfName.getText().trim();
                String pass = tfPass.getText() == null ? "" : tfPass.getText().trim();
                String gender = tfGender.getValue();
                String addr = tfAddr.getText() == null ? "" : tfAddr.getText().trim();
                String area = tfArea.getValue();

                // ✅ FIX: Alerts must be owned by the DIALOG, not the stage (main window)
                Stage dlgStage = (Stage) dlg.getDialogPane().getScene().getWindow();

                if (name.isEmpty()) {
                    PopupUtil.showError(dlgStage, "Username cannot be empty.");
                    ev.consume();
                    return;
                }
                if (pass.isEmpty()) {
                    PopupUtil.showError(dlgStage, "Password cannot be empty.");
                    ev.consume();
                    return;
                }
                if (gender == null || gender.isBlank()) {
                    PopupUtil.showError(dlgStage, "Gender cannot be empty.");
                    ev.consume();
                    return;
                }
                if (dpDob.getValue() == null) {
                    PopupUtil.showError(dlgStage, "Date of Birth cannot be empty.");
                    ev.consume();
                    return;
                }
                if (addr.isEmpty()) {
                    PopupUtil.showError(dlgStage, "Address cannot be empty.");
                    ev.consume();
                    return;
                }
                if (area == null || area.isBlank()) {
                    PopupUtil.showError(dlgStage, "Work Area cannot be empty.");
                    ev.consume();
                    return;
                }

                java.time.LocalDate dob = dpDob.getValue();
                java.time.LocalDate today = java.time.LocalDate.now();
                if (dob.isAfter(today)) {
                    PopupUtil.showError(dlgStage, "DOB cannot be in the future.");
                    ev.consume();
                    return;
                }

                int years = java.time.Period.between(dob, today).getYears();
                if (years < 10 || years > 120) {
                    PopupUtil.showError(dlgStage, "Please enter a realistic DOB (age 10 to 120).");
                    ev.consume();
                    return;
                }

                if (!PasswordUtil.isStrongEnough(pass)) {
                    PopupUtil.showError(dlgStage,
                            "Password too weak. Use 8+ characters with at least 1 letter and 1 number.");
                    ev.consume();
                    return;
                }

                boolean ok = employeeDAO.createEmployee(name, pass, gender, addr, area, dob.toString());
                if (!ok) {
                    PopupUtil.showError(dlgStage, "Failed to create employee. Check console for details.");
                    ev.consume();
                    return;
                }

                empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));
                fireRefresh(onDataChanged);
            });

            dlg.showAndWait();
        });

        btnDelEmp.setOnAction(e -> {
            StaffRow selected = empTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Select an employee to delete.");
                return;
            }
            if (confirm(stage, "Delete Employee", "Are you sure you want to delete " + selected.getUserName() + "?")) {
                boolean deleted = employeeDAO.deleteEmployee(selected.getUserId());
                if (deleted) {
                    empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));
                    fireRefresh(onDataChanged);
                    PopupUtil.showInfo(stage, "Employee deleted successfully.");
                } else {
                    // ✅ FIX: Show error popup if delete fails (e.g. assigned to order)
                    PopupUtil.showError(stage, "Cannot delete user as they are assigned to an order.");
                }
            }
        });

        btnRefreshEmp.setOnAction(e -> {
            empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));
            fireRefresh(onDataChanged); // refresh KPIs/charts too
        });

        HBox empBtns = new HBox(10, btnAddEmp, btnViewEmp, btnDelEmp, btnRefreshEmp);
        VBox empBox = new VBox(10, new Label("Employees"), empBtns, empTable);
        empBox.setPadding(new Insets(10));
        VBox.setVgrow(empTable, Priority.ALWAYS);

        Tab empTab = new Tab("Employees", empBox);

        tabs.getTabs().addAll(productTab, orderTab, empTab);
        return tabs;
    }

    // ---- Helpers ----

    private static void installIntFilter(TextField tf) {
        tf.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            if (next.isEmpty() || next.matches("\\d*"))
                return change;
            return null;
        }));
    }

    private static void fireRefresh(Runnable onDataChanged) {
        if (onDataChanged == null)
            return;

        Platform.runLater(() -> Platform.runLater(onDataChanged));
    }

    private static boolean confirm(Stage owner, String title, String msg) {
        // ✅ Use PopupUtil.confirm for consistent ownership and modality behavior
        return PopupUtil.confirm(owner, title, msg);
    }

    private static void showError(Stage owner, String msg) {
        PopupUtil.showError(owner, msg);
    }
}

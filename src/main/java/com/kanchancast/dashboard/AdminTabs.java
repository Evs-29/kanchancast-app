package com.kanchancast.dashboard;

import com.jewelleryapp.dao.EmployeeDAO;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.kanchancast.dialogs.OrderDetailsDialog;
import com.kanchancast.model.Product;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StaffRow;
import com.kanchancast.ui.CategoryManagerDialog;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;

public class AdminTabs {

    public static TabPane buildTabs(Stage stage, ProductDAO productDAO, OrderDAO orderDAO, EmployeeDAO employeeDAO) {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ==========================
        // ===== PRODUCTS TAB =====
        // ==========================
        TableView<Product> productTable = new TableView<>();
        TableColumn<Product, Number> pid = new TableColumn<>("Product ID");
        pid.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getProductId()));

        TableColumn<Product, String> pname = new TableColumn<>("Product Name");
        pname.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Product, String> ptype = new TableColumn<>("Category");
        ptype.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));

        TableColumn<Product, String> pdesc = new TableColumn<>("Description");
        pdesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        productTable.getColumns().addAll(pid, pname, ptype, pdesc);
        productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));

        // --- Buttons ---
        Button btnAddProduct = new Button("➕ Create Product");
        Button btnRefreshProduct = new Button("🔄 Refresh");
        Button btnDeleteProduct = new Button("🗑️ Delete");
        Button btnManageCategories = new Button("⚙️ Manage Categories");

        btnAddProduct.setOnAction(e -> {
            try {
                com.kanchancast.ui.ProductFormDialog.show(stage);
                productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error opening product dialog: " + ex.getMessage()).showAndWait();
            }
        });

        btnRefreshProduct.setOnAction(e ->
                productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()))
        );

        btnDeleteProduct.setOnAction(e -> {
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a product to delete.").showAndWait();
                return;
            }
            if (confirm("Delete Product", "Are you sure you want to delete " + selected.getName() + "?")) {
                productDAO.deleteProduct(selected.getProductId());
                productTable.setItems(FXCollections.observableArrayList(productDAO.listALL()));
            }
        });

        btnManageCategories.setOnAction(e -> {
            try {
                CategoryManagerDialog.show(stage, productDAO);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error opening category manager: " + ex.getMessage()).showAndWait();
            }
        });

        HBox productBtns = new HBox(10, btnAddProduct, btnRefreshProduct, btnDeleteProduct, btnManageCategories);
        VBox productBox = new VBox(10, new Label("All Products"), productBtns, productTable);
        productBox.setPadding(new Insets(10));

        Tab productTab = new Tab("Products", productBox);


        // ==========================
        // ===== ORDERS TAB =====
        // ==========================
        TableView<OrderSummary> orderTable = new TableView<>();
        TableColumn<OrderSummary, Number> oid = new TableColumn<>("Order ID");
        oid.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getOrderId()));

        TableColumn<OrderSummary, String> ocust = new TableColumn<>("Customer");
        ocust.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));

        TableColumn<OrderSummary, String> oprod = new TableColumn<>("Product");
        oprod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));

        TableColumn<OrderSummary, String> odate = new TableColumn<>("Date Ordered");
        odate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateOrdered()));

        TableColumn<OrderSummary, String> ostat = new TableColumn<>("Status");
        ostat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        orderTable.getColumns().addAll(oid, ocust, oprod, odate, ostat);
        orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()));

        Button btnAssign = new Button("👷 Assign Employees");
        Button btnRefreshOrders = new Button("🔄 Refresh Orders");

        btnAssign.setOnAction(e -> {
            OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Please select an order to assign.").showAndWait();
                return;
            }
            OrderDetailsDialog.show(stage, orderDAO, employeeDAO, selected);
        });

        btnRefreshOrders.setOnAction(e ->
                orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()))
        );

        HBox orderBtns = new HBox(10, btnAssign, btnRefreshOrders);
        VBox orderBox = new VBox(10, new Label("All Orders"), orderBtns, orderTable);
        orderBox.setPadding(new Insets(10));

        Tab orderTab = new Tab("Orders", orderBox);


        // ==========================
        // ===== EMPLOYEES TAB =====
        // ==========================
        TableView<StaffRow> empTable = new TableView<>();

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
        Button btnDelEmp = new Button("🗑️ Delete Employee");
        Button btnRefreshEmp = new Button("🔄 Refresh");

        btnAddEmp.setOnAction(e -> {
            Dialog<String> dlg = new Dialog<>();
            dlg.setTitle("Create New Employee");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane gp = new GridPane();
            gp.setHgap(10);
            gp.setVgap(8);
            gp.setPadding(new Insets(20, 25, 20, 25));

            TextField tfName = new TextField();
            TextField tfPass = new TextField("password123");
            ComboBox<String> tfGender = new ComboBox<>(FXCollections.observableArrayList("Male", "Female"));
            tfGender.setValue("Male");

            TextField tfAddr = new TextField();
            TextField tfAge = new TextField();

            ComboBox<String> tfArea = new ComboBox<>(FXCollections.observableArrayList(
                    "Production",
                    "Raw Material Procurement",
                    "Raw Material Procurement and Management",
                    "Casting",
                    "Investment Casting",
                    "Design & CAD Modelling",
                    "Polishing",
                    "Stone Setting",
                    "Finishing",
                    "Quality Control",
                    "Inventory & Packaging"
            ));
            tfArea.setValue("Production");

            int r = 0;
            gp.addRow(r++, new Label("Username:"), tfName);
            gp.addRow(r++, new Label("Password:"), tfPass);
            gp.addRow(r++, new Label("Gender:"), tfGender);
            gp.addRow(r++, new Label("Age:"), tfAge);
            gp.addRow(r++, new Label("Address:"), tfAddr);
            gp.addRow(r++, new Label("Work Area:"), tfArea);

            dlg.getDialogPane().setContent(gp);

            dlg.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        String name = tfName.getText().trim();
                        String pass = tfPass.getText().trim();
                        String gender = tfGender.getValue();
                        String addr = tfAddr.getText().trim();
                        String area = tfArea.getValue();
                        int age = Integer.parseInt(tfAge.getText().trim());

                        if (name.isEmpty()) {
                            showAlert("Username cannot be empty."); return null;
                        }
                        if (age <= 0 || age > 120) {
                            showAlert("Please enter a valid age."); return null;
                        }

                        boolean ok = employeeDAO.createEmployee(name, pass, gender, addr, area, age);
                        if (!ok) showAlert("Failed to create employee. Check console for details.");

                    } catch (NumberFormatException ex) {
                        showAlert("Age must be a number.");
                    } catch (Exception ex) {
                        showAlert("Unexpected error: " + ex.getMessage());
                    }
                }
                return null;
            });

            dlg.showAndWait();
            empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));
        });

        btnDelEmp.setOnAction(e -> {
            StaffRow selected = empTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Select an employee to delete.").showAndWait();
                return;
            }
            if (confirm("Delete Employee", "Are you sure you want to delete " + selected.getUserName() + "?")) {
                employeeDAO.deleteEmployee(selected.getUserId());
                empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));
            }
        });

        btnRefreshEmp.setOnAction(e ->
                empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()))
        );

        HBox empBtns = new HBox(10, btnAddEmp, btnDelEmp, btnRefreshEmp);
        VBox empBox = new VBox(10, new Label("Employees"), empBtns, empTable);
        empBox.setPadding(new Insets(10));

        Tab empTab = new Tab("Employees", empBox);

        // Add all tabs
        tabs.getTabs().addAll(productTab, orderTab, empTab);
        return tabs;
    }

    private static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(title);
        return a.showAndWait().filter(r -> r == ButtonType.YES).isPresent();
    }

    private static void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}

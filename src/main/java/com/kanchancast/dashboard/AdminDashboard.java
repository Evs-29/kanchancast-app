package com.kanchancast.dashboard;

import com.jewelleryapp.dao.EmployeeDAO;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.Product;
import com.kanchancast.model.StaffRow;
import com.kanchancast.model.User;
import com.kanchancast.nav.ScreenRouter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Map;
import java.util.LinkedHashMap;

import java.util.List;

public class AdminDashboard {

    public static void show(Stage stage, User adminUser) {

        // DAOs
        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();
        EmployeeDAO employeeDAO = new EmployeeDAO();
        UserDAO userDAO = new UserDAO();

        // =========================
        // TAB 1 — PRODUCTS
        // =========================
        TextField tfName = new TextField();
        tfName.setPromptText("Product Name");

        ComboBox<String> cbCategory = new ComboBox<>();
        cbCategory.setPromptText("Select or Add Category");

        TextField tfPrice = new TextField();
        tfPrice.setPromptText("Price");

        TextField tfGold = new TextField();
        tfGold.setPromptText("Gold Weight");

        TextField tfDiamond = new TextField();
        tfDiamond.setPromptText("Diamond Weight");

        TextArea tfDescription = new TextArea();
        tfDescription.setPromptText("Description");
        tfDescription.setPrefRowCount(3);

        // categories loader
        Runnable loadCategories = () -> {
            List<String> cats = productDAO.listAllCategories();
            cbCategory.setItems(FXCollections.observableArrayList(cats));
        };
        loadCategories.run();

        Button btnNewCategory = new Button("➕ Add Category");
        Button btnDeleteCategory = new Button("🗑 Delete Category");

        btnNewCategory.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Category");
            dialog.setHeaderText("Add a new product category");
            dialog.setContentText("Enter category name:");
            dialog.showAndWait().ifPresent(cat -> {
                if (cat != null && !cat.trim().isEmpty()) {
                    boolean ok = productDAO.addCategory(cat.trim());
                    if (ok) {
                        loadCategories.run();
                        cbCategory.setValue(cat.trim());
                        new Alert(Alert.AlertType.INFORMATION, "✅ Category added.").showAndWait();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "⚠️ Failed to add category. It may already exist or DB error.").showAndWait();
                    }
                }
            });
        });

        btnDeleteCategory.setOnAction(e -> {
            String cat = cbCategory.getValue();
            if (cat == null || cat.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Select a category first.").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete category \"" + cat + "\"?\n(Note: You must remove/reassign products using this category first.)",
                    ButtonType.CANCEL, ButtonType.OK);
            confirm.setHeaderText("Delete Category");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    boolean ok = productDAO.deleteCategory(cat);
                    if (ok) {
                        new Alert(Alert.AlertType.INFORMATION, "✅ Category deleted.").showAndWait();
                        loadCategories.run();
                        cbCategory.setValue(null);
                    } else {
                        new Alert(Alert.AlertType.ERROR, "⚠️ Could not delete. Make sure no products use this category.").showAndWait();
                    }
                }
            });
        });

        // product table
        TableView<Product> productTable = new TableView<>();
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Product, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));

        TableColumn<Product, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Product, Double> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        productTable.getColumns().addAll(colId, colName, colType, colPrice);

        Runnable loadProducts = () -> {
            productTable.setItems(FXCollections.observableArrayList(productDAO.listAll()));
        };
        loadProducts.run();

        Button btnAdd = new Button("Add Product");
        Button btnDelete = new Button("Delete Product");
        Button btnRefreshProducts = new Button("Refresh");

        btnAdd.setOnAction(e -> {
            try {
                String name = tfName.getText().trim();
                String type = cbCategory.getValue();
                double price = Double.parseDouble(tfPrice.getText().trim());
                double gold = tfGold.getText().isBlank() ? 0.0 : Double.parseDouble(tfGold.getText().trim());
                double dia  = tfDiamond.getText().isBlank() ? 0.0 : Double.parseDouble(tfDiamond.getText().trim());
                String desc = tfDescription.getText() == null ? "" : tfDescription.getText().trim();

                if (name.isEmpty() || type == null || type.isBlank()) {
                    new Alert(Alert.AlertType.WARNING, "Name and Category are required.").showAndWait();
                    return;
                }

                // make sure category exists
                productDAO.ensureCategoryExists(type);

                Product p = new Product();
                p.setName(name);
                p.setType(type);
                p.setPrice(price);
                p.setGoldWeight(gold);
                p.setDiamondWeight(dia);
                p.setDescription(desc);
                p.setImagePath("");

                boolean ok = productDAO.addProduct(p);
                if (ok) {
                    new Alert(Alert.AlertType.INFORMATION, "✅ Product added.").showAndWait();
                    tfName.clear(); tfPrice.clear(); tfGold.clear(); tfDiamond.clear(); tfDescription.clear();
                    loadProducts.run();
                } else {
                    new Alert(Alert.AlertType.ERROR, "⚠️ Failed to add product.").showAndWait();
                }
            } catch (NumberFormatException nfe) {
                new Alert(Alert.AlertType.ERROR, "⚠️ Invalid number in Price/Weights.").showAndWait();
            }
        });

        btnDelete.setOnAction(e -> {
            Product sel = productTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a product first.").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete \"" + sel.getName() + "\"?", ButtonType.CANCEL, ButtonType.OK);
            confirm.setHeaderText("Delete Product");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    if (productDAO.deleteProduct(sel.getProductId())) {
                        loadProducts.run();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "⚠️ Could not delete product.").showAndWait();
                    }
                }
            });
        });

        btnRefreshProducts.setOnAction(e -> {
            loadCategories.run();
            loadProducts.run();
        });

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.addRow(0, new Label("Name:"), tfName);
        form.addRow(1, new Label("Category:"), new HBox(10, cbCategory, btnNewCategory, btnDeleteCategory));
        form.addRow(2, new Label("Price:"), tfPrice);
        form.addRow(3, new Label("Gold Wt:"), tfGold);
        form.addRow(4, new Label("Diamond Wt:"), tfDiamond);
        form.addRow(5, new Label("Description:"), tfDescription);

        HBox productActions = new HBox(10, btnAdd, btnDelete, btnRefreshProducts);
        productActions.setAlignment(Pos.CENTER_LEFT);

        VBox productBox = new VBox(10, new Label("Products"), form, productActions, productTable);
        productBox.setPadding(new Insets(10));

        // =========================
        // TAB 2 — ALL CUSTOMER ORDERS
        // =========================
        TableView<OrderSummary> orderTable = new TableView<>();
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<OrderSummary, Integer> oId = new TableColumn<>("Order ID");
        oId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        TableColumn<OrderSummary, String> oUser = new TableColumn<>("Customer");
        oUser.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        TableColumn<OrderSummary, String> oProduct = new TableColumn<>("Product");
        oProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        TableColumn<OrderSummary, String> oDate = new TableColumn<>("Date Ordered");
        oDate.setCellValueFactory(new PropertyValueFactory<>("dateOrdered"));
        TableColumn<OrderSummary, String> oStatus = new TableColumn<>("Status");
        oStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<OrderSummary, Integer> oProgress = new TableColumn<>("Progress %");
        oProgress.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getProgressPercent()).asObject());

        orderTable.getColumns().addAll(oId, oUser, oProduct, oDate, oStatus, oProgress);

        Runnable loadOrders = () -> orderTable.setItems(FXCollections.observableArrayList(orderDAO.listAll()));
        loadOrders.run();

        Button btnRefreshOrders = new Button("Refresh Orders");
        Button btnAssignEmployees = new Button("Assign Employees");

        btnRefreshOrders.setOnAction(e -> loadOrders.run());

        // (Keeps your current placeholder for assignment – we can wire the full dialog next.)
        btnAssignEmployees.setOnAction(e -> {
            OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Select an order first!").showAndWait();
                return;
            }

            int orderId = selected.getOrderId();

            // Ensure stages exist for this order
            orderDAO.ensureOrderStagesExist(orderId);

            // List of all stages
            List<String> stages = List.of(
                    "Raw Material Procurement",
                    "Design & CAD Modelling",
                    "Wax Model Creation",
                    "Investment Casting",
                    "Cleaning & Devesting",
                    "Filing & Pre-Polishing",
                    "Stone Setting",
                    "Final Polishing",
                    "Plating",
                    "Quality Control",
                    "Packaging & Dispatch"
            );

            // Load already assigned employees for this order
            Map<String, Integer> assigned = orderDAO.getAssignedEmployeesForOrder(orderId);

            // Create dialog UI
            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Assign Employees");
            dlg.setHeaderText("Assign employees to each production stage");

            GridPane gp = new GridPane();
            gp.setVgap(8);
            gp.setHgap(12);
            gp.setPadding(new Insets(10, 20, 10, 20));

            // Keep dropdowns in order
            Map<String, ComboBox<StaffRow>> pickers = new LinkedHashMap<>();

            int row = 0;
            for (String stageName : stages) {
                List<StaffRow> staff = employeeDAO.listByWorkArea(stageName);
                ComboBox<StaffRow> cb = new ComboBox<>(FXCollections.observableArrayList(staff));

                // Nice label text formatting
                cb.setConverter(new javafx.util.StringConverter<StaffRow>() {
                    @Override
                    public String toString(StaffRow s) {
                        return (s == null) ? "" : s.getUserName() + " (" + s.getWorkArea() + ")";
                    }

                    @Override
                    public StaffRow fromString(String s) {
                        return null;
                    }
                });

                // 🔹 Preselect if someone already assigned
                if (assigned.containsKey(stageName)) {
                    int assignedEmpId = assigned.get(stageName);
                    staff.stream()
                            .filter(s -> s.getUserId() == assignedEmpId)
                            .findFirst()
                            .ifPresent(cb::setValue);
                }

                gp.addRow(row++, new Label(stageName + ":"), cb);
                pickers.put(stageName, cb);
            }

            dlg.getDialogPane().setContent(gp);
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dlg.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;

                int assignedCount = 0;
                for (var entry : pickers.entrySet()) {
                    String stageName = entry.getKey();
                    StaffRow emp = entry.getValue().getValue();
                    if (emp != null) {
                        boolean ok = orderDAO.assignEmployeeToStage(orderId, stageName, emp.getUserId());
                        if (ok) assignedCount++;
                    }
                }

                new Alert(Alert.AlertType.INFORMATION, "✅ Assigned " + assignedCount + " employees to order #" + orderId).showAndWait();
                return null;
            });

            dlg.showAndWait();
        });

        VBox orderBox = new VBox(10, new Label("All Customer Orders"),
                new HBox(10, btnRefreshOrders, btnAssignEmployees), orderTable);
        orderBox.setPadding(new Insets(10));

        // =========================
        // TAB 3 — EMPLOYEES
        // =========================
        TableView<StaffRow> empTable = new TableView<>();
        empTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StaffRow, Number> colEId = new TableColumn<>("ID");
        colEId.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getUserId()));
        TableColumn<StaffRow, String> colEName = new TableColumn<>("Name");
        colEName.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getUserName()));
        TableColumn<StaffRow, String> colEWorkArea = new TableColumn<>("Work Area");
        colEWorkArea.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getWorkArea()));
        TableColumn<StaffRow, String> colEGender = new TableColumn<>("Gender");
        colEGender.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getGender()));
        empTable.getColumns().addAll(colEId, colEName, colEWorkArea, colEGender);

        TextField tfUsername = new TextField();
        PasswordField tfPassword = new PasswordField();
        TextField tfAddressEmp = new TextField();

        ComboBox<String> cbGender = new ComboBox<>();
        cbGender.getItems().addAll("Male", "Female", "Other");
        cbGender.setPromptText("Select Gender");

        ComboBox<String> cbArea = new ComboBox<>();
        cbArea.getItems().addAll(
                "Raw Material Procurement",
                "Design & CAD Modelling",
                "Wax Model Creation",
                "Investment Casting",
                "Cleaning & Devesting",
                "Filing & Pre-Polishing",
                "Stone Setting",
                "Final Polishing",
                "Plating",
                "Quality Control",
                "Packaging & Dispatch"
        );
        cbArea.setPromptText("Select Work Area");

        Runnable loadEmployees = () -> empTable.setItems(FXCollections.observableArrayList(employeeDAO.listAll()));
        loadEmployees.run();

        Button btnCreateEmp = new Button("Create Employee");
        Button btnRefreshEmp = new Button("Refresh");

        btnCreateEmp.setOnAction(e -> {
            boolean ok = employeeDAO.createEmployee(
                    tfUsername.getText(),
                    tfPassword.getText(),
                    cbArea.getValue(),
                    tfAddressEmp.getText(),
                    cbGender.getValue()
            );
            if (ok) {
                new Alert(Alert.AlertType.INFORMATION, "✅ Employee created successfully!").showAndWait();
                tfUsername.clear(); tfPassword.clear(); tfAddressEmp.clear();
                cbGender.setValue(null); cbArea.setValue(null);
                loadEmployees.run();
            } else {
                new Alert(Alert.AlertType.ERROR, "⚠️ Failed to create employee. Check fields.").showAndWait();
            }
        });

        btnRefreshEmp.setOnAction(e -> loadEmployees.run());

        GridPane empForm = new GridPane();
        empForm.setHgap(10); empForm.setVgap(10);
        empForm.addRow(0, new Label("Username:"), tfUsername);
        empForm.addRow(1, new Label("Password:"), tfPassword);
        empForm.addRow(2, new Label("Work Area:"), cbArea);
        empForm.addRow(3, new Label("Address:"), tfAddressEmp);
        empForm.addRow(4, new Label("Gender:"), cbGender);
        empForm.addRow(5, new Label(""), new HBox(10, btnCreateEmp, btnRefreshEmp));

        VBox empBox = new VBox(10, new Label("Employees"), empForm, empTable);
        empBox.setPadding(new Insets(10));

        // =========================
        // LAYOUT
        // =========================
        TabPane tabs = new TabPane(
                new Tab("Products", productBox),
                new Tab("All Customer Orders", orderBox),
                new Tab("Employees", empBox)
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> ScreenRouter.goToLogin(stage));
        HBox top = new HBox(logoutBtn);
        top.setAlignment(Pos.CENTER_RIGHT);
        top.setPadding(new Insets(10));

        BorderPane root = new BorderPane(tabs);
        root.setTop(top);

        Scene scene = new Scene(root, 1180, 720);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Admin Dashboard");
        stage.show();
    }
}

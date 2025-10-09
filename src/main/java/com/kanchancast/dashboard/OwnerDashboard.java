package com.kanchancast.dashboard;

import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import com.kanchancast.nav.ScreenRouter;
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

import java.util.List;

public class OwnerDashboard {

    public static void show(Stage stage, User ownerUser) {
        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();
        UserDAO userDAO = new UserDAO();

        // ---------------- TAB 1: PRODUCTS ----------------
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

        Button btnRefreshProducts = new Button("Refresh Products");
        btnRefreshProducts.setOnAction(e -> {
            List<Product> products = productDAO.listAll();
            productTable.setItems(FXCollections.observableArrayList(products));
        });

        VBox productTab = new VBox(10, new Label("All Products"), btnRefreshProducts, productTable);
        productTab.setPadding(new Insets(10));

        // ---------------- TAB 2: ALL ORDERS ----------------
        TableView<OrderSummary> ordersTable = new TableView<>();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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
        oProgress.setCellValueFactory(v ->
                new SimpleIntegerProperty(v.getValue().getProgressPercent()).asObject());

        ordersTable.getColumns().addAll(oId, oUser, oProduct, oDate, oStatus, oProgress);

        Button btnRefreshOrders = new Button("Refresh Orders");
        btnRefreshOrders.setOnAction(e -> {
            List<OrderSummary> orders = orderDAO.listAll();
            ordersTable.setItems(FXCollections.observableArrayList(orders));
        });

        VBox ordersTab = new VBox(10, new Label("All Orders and Assigned Employees"), btnRefreshOrders, ordersTable);
        ordersTab.setPadding(new Insets(10));

        // ---------------- TAB 3: USERS (ADMINS + EMPLOYEES) ----------------
        TableView<User> usersTable = new TableView<>();
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<User, Integer> uId = new TableColumn<>("User ID");
        uId.setCellValueFactory(new PropertyValueFactory<>("userId"));

        TableColumn<User, String> uName = new TableColumn<>("Name");
        uName.setCellValueFactory(new PropertyValueFactory<>("userName"));

        TableColumn<User, String> uType = new TableColumn<>("Type");
        uType.setCellValueFactory(new PropertyValueFactory<>("userType"));

        TableColumn<User, String> uGender = new TableColumn<>("Gender");
        uGender.setCellValueFactory(new PropertyValueFactory<>("gender"));

        // ✅ NEW: Age column (read-only)
        TableColumn<User, Integer> uAge = new TableColumn<>("Age");
        uAge.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getAge()).asObject());

        TableColumn<User, String> uAddress = new TableColumn<>("Address");
        uAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<User, String> uArea = new TableColumn<>("Work Area");
        uArea.setCellValueFactory(new PropertyValueFactory<>("area"));

        usersTable.getColumns().addAll(uId, uName, uType, uGender, uAge, uAddress, uArea);

        Button btnRefreshUsers = new Button("Refresh Users");
        Button btnDeleteUser = new Button("Delete Selected");
        Button btnAddAdmin = new Button("Create New Admin");

        Runnable loadUsers = () -> {
            List<User> users = userDAO.listAll();
            usersTable.setItems(FXCollections.observableArrayList(users.stream()
                    .filter(u -> u.getUserType().equalsIgnoreCase("employee") ||
                            u.getUserType().equalsIgnoreCase("admin"))
                    .toList()));
        };

        btnRefreshUsers.setOnAction(e -> loadUsers.run());

        btnDeleteUser.setOnAction(e -> {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Select a user first!").showAndWait();
                return;
            }
            if (selected.getUserType().equalsIgnoreCase("owner")) {
                new Alert(Alert.AlertType.WARNING, "You cannot delete the Owner account!").showAndWait();
                return;
            }
            boolean deleted = userDAO.deleteUser(selected.getUserId());
            if (deleted)
                new Alert(Alert.AlertType.INFORMATION, "✅ User deleted successfully!").showAndWait();
            else
                new Alert(Alert.AlertType.ERROR, "⚠️ Failed to delete user!").showAndWait();

            loadUsers.run();
        });

        btnAddAdmin.setOnAction(e -> {
            Dialog<User> dialog = new Dialog<>();
            dialog.setTitle("Create New Administrator");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));

            TextField nameField = new TextField();
            PasswordField passField = new PasswordField();
            TextField addressField = new TextField();
            TextField genderField = new TextField();

            grid.addRow(0, new Label("Username:"), nameField);
            grid.addRow(1, new Label("Password:"), passField);
            grid.addRow(2, new Label("Address:"), addressField);
            grid.addRow(3, new Label("Gender:"), genderField);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    boolean ok = userDAO.createUser(
                            "admin",
                            nameField.getText(),
                            passField.getText(),
                            addressField.getText(),
                            genderField.getText(),
                            null
                    );
                    if (ok)
                        new Alert(Alert.AlertType.INFORMATION, "✅ Admin created successfully!").showAndWait();
                    else
                        new Alert(Alert.AlertType.ERROR, "⚠️ Failed to create admin!").showAndWait();
                    loadUsers.run();
                }
                return null;
            });

            dialog.showAndWait();
        });

        HBox userButtons = new HBox(10, btnRefreshUsers, btnAddAdmin, btnDeleteUser);
        VBox usersTab = new VBox(10, new Label("Employees & Administrators"), userButtons, usersTable);
        usersTab.setPadding(new Insets(10));

        // ---------------- TAB PANE ----------------
        TabPane tabs = new TabPane(
                new Tab("Products", productTab),
                new Tab("Orders", ordersTab),
                new Tab("Users", usersTab)
        );
        tabs.getTabs().forEach(tab -> tab.setClosable(false));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> ScreenRouter.goToLogin(stage));

        BorderPane root = new BorderPane(tabs);
        HBox top = new HBox(logoutBtn);
        top.setAlignment(Pos.CENTER_RIGHT);
        top.setPadding(new Insets(10));
        root.setTop(top);

        Scene scene = new Scene(root, 1180, 720);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Owner Dashboard");
        stage.show();

        // initial load
        btnRefreshProducts.fire();
        btnRefreshOrders.fire();
        loadUsers.run();
    }
}

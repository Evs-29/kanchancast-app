package com.kanchancast.dashboard;

import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import com.kanchancast.nav.ScreenRouter;
import com.kanchancast.ui.OrderTrackingDialog;
import com.kanchancast.ui.ProductDetailsDialog;
import com.kanchancast.ui.ProductGrid;
import com.kanchancast.ui.UIKit;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Dashboard
 * -------------------
 * Displays:
 *  - All available products (left panel)
 *  - User’s placed orders (right panel)
 *  - Allows placing new orders and tracking progress.
 *  - Dynamically loads categories from DB.
 */
public class CustomerDashboard {

    public static void show(Stage stage, User user) {
        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();

        // ---------- ORDERS TABLE (RIGHT PANEL) ----------
        TableView<OrderSummary> ordersTable = new TableView<>();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<OrderSummary, Integer> colId = new TableColumn<>("Order ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<OrderSummary, String> colProduct = new TableColumn<>("Product");
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<OrderSummary, String> colDate = new TableColumn<>("Date Ordered");
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateOrdered"));

        TableColumn<OrderSummary, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<OrderSummary, Integer> colProgress = new TableColumn<>("Progress %");
        colProgress.setCellValueFactory(v ->
                new SimpleIntegerProperty(v.getValue().getProgressPercent()).asObject());

        ordersTable.getColumns().addAll(colId, colProduct, colDate, colStatus, colProgress);

        Label ordersLabel = new Label("My Orders");
        ordersLabel.getStyleClass().add("section-title");

        Button refreshOrders = new Button("Refresh");
        Button trackProgress = new Button("Track Progress");

        HBox ordersBar = new HBox(10, refreshOrders, trackProgress);
        ordersBar.setAlignment(Pos.CENTER_LEFT);
        ordersBar.setPadding(new Insets(5, 0, 10, 0));

        VBox ordersPanel = new VBox(8, ordersLabel, ordersBar, ordersTable);
        ordersPanel.setPadding(new Insets(10));
        ordersPanel.getStyleClass().add("card");

        // ✅ Load user’s orders
        Runnable loadOrders = () -> {
            List<OrderSummary> orders = orderDAO.getOrdersForUser(user.getUserId());
            ordersTable.setItems(FXCollections.observableArrayList(orders));
        };

        refreshOrders.setOnAction(e -> loadOrders.run());
        trackProgress.setOnAction(e -> {
            OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Please select an order first!", ButtonType.OK).showAndWait();
                return;
            }
            new OrderTrackingDialog(stage, selected, orderDAO).show();
        });

        // ---------- PRODUCTS GRID (LEFT PANEL) ----------
        Label productsLabel = new Label("Products");
        productsLabel.getStyleClass().add("section-title");

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("Select category...");

        Button refreshProducts = new Button("Refresh");
        HBox productsBar = new HBox(10, new Label("Category:"), categoryFilter, refreshProducts);
        productsBar.setAlignment(Pos.CENTER_LEFT);
        productsBar.setPadding(new Insets(5, 0, 10, 0));

        ProductGrid productGrid = new ProductGrid(product ->
                new ProductDetailsDialog(stage, product, orderDAO, user, loadOrders::run).showAndWait()
        );

        // ✅ Dynamically load distinct categories from DB
        Runnable loadCategories = () -> {
            List<String> categories = new ArrayList<>();
            categories.add("all"); // Always show 'all' first
            try (Connection conn = com.jewelleryapp.dao.DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT LOWER(TRIM(type)) AS type FROM products WHERE type IS NOT NULL AND TRIM(type) != '' ORDER BY type ASC");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    categories.add(rs.getString("type"));
                }

            } catch (SQLException ex) {
                System.err.println("⚠️ Error loading categories: " + ex.getMessage());
            }

            categoryFilter.setItems(FXCollections.observableArrayList(categories));
            categoryFilter.setValue("all");
        };

        // ✅ Load products depending on selected category
        Runnable loadProducts = () -> {
            String selectedCategory = categoryFilter.getValue();
            List<Product> products;

            if (selectedCategory == null || selectedCategory.equalsIgnoreCase("all")) {
                products = productDAO.listAll();
            } else {
                products = productDAO.listByType(selectedCategory.trim().toLowerCase());
            }

            productGrid.setItems(products != null ? products : List.of());
        };

        // Auto-refresh on dropdown change
        categoryFilter.setOnAction(e -> loadProducts.run());
        refreshProducts.setOnAction(e -> loadProducts.run());

        VBox productsPanel = new VBox(8, productsLabel, productsBar, productGrid);
        productsPanel.setPadding(new Insets(10));
        productsPanel.getStyleClass().add("card");

        // ---------- LAYOUT ----------
        SplitPane splitPane = new SplitPane(productsPanel, ordersPanel);
        splitPane.setDividerPositions(0.55);

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> ScreenRouter.goToLogin(stage));
        HBox topBar = new HBox(logoutButton);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("toolbar");

        BorderPane root = new BorderPane(splitPane, topBar, null, null, null);
        Scene scene = new Scene(root, 1180, 720);
        UIKit.apply(scene);

        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Customer Dashboard");
        stage.show();

        // ---------- INITIAL LOAD ----------
        loadCategories.run();
        loadProducts.run();
        loadOrders.run();
    }
}

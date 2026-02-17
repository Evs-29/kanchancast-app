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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboard {

        public static void show(Stage stage, User user) {
                ProductDAO productDAO = new ProductDAO();
                OrderDAO orderDAO = new OrderDAO();

                // ---------- ORDERS TABLE ----------
                TableView<OrderSummary> ordersTable = new TableView<>();
                ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

                TableColumn<OrderSummary, Integer> colId = new TableColumn<>("Order ID");
                colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));

                TableColumn<OrderSummary, String> colProduct = new TableColumn<>("Product");
                colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));

                TableColumn<OrderSummary, String> colDate = new TableColumn<>("Date Ordered");
                colDate.setCellValueFactory(new PropertyValueFactory<>("dateOrdered"));

                TableColumn<OrderSummary, String> colDelivery = new TableColumn<>("Delivery Date");
                colDelivery.setCellValueFactory(new PropertyValueFactory<>("deliveryDate"));

                TableColumn<OrderSummary, String> colStatus = new TableColumn<>("Status");
                colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

                TableColumn<OrderSummary, Integer> colProgress = new TableColumn<>("Progress %");
                colProgress.setCellValueFactory(
                                v -> new SimpleIntegerProperty(v.getValue().getProgressPercent()).asObject());

                ordersTable.getColumns().addAll(
                                colId, colProduct, colDate, colDelivery, colStatus, colProgress);

                Label ordersLabel = new Label("My Orders"); // <-- make bigger (we will style after UIKit.apply)

                Button refreshOrders = new Button("Refresh");
                Button trackProgress = new Button("Track Progress");

                HBox ordersBar = new HBox(10, refreshOrders, trackProgress);
                ordersBar.setAlignment(Pos.CENTER_LEFT);
                ordersBar.setPadding(new Insets(5, 0, 10, 0));

                VBox ordersPanel = new VBox(10, ordersLabel, ordersBar, ordersTable);
                ordersPanel.setPadding(new Insets(12));
                ordersPanel.getStyleClass().add("card");

                Runnable loadOrders = () -> {
                        ordersTable.setItems(FXCollections.observableArrayList(
                                        orderDAO.getOrdersForUser(user.getUserId())));
                };

                refreshOrders.setOnAction(e -> loadOrders.run());

                trackProgress.setOnAction(e -> {
                        OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
                        if (selected == null) {
                                new Alert(Alert.AlertType.WARNING, "Please select an order first!", ButtonType.OK)
                                                .showAndWait();
                                return;
                        }
                        new OrderTrackingDialog(stage, selected, orderDAO).showAndWait();
                        loadOrders.run();
                });

                // ---------- PRODUCTS ----------
                Label productsLabel = new Label("Products"); // <-- make bigger (we will style after UIKit.apply)

                ComboBox<String> categoryFilter = new ComboBox<>();
                categoryFilter.setPromptText("Select category...");

                Button refreshProducts = new Button("Refresh");

                HBox productsBar = new HBox(10,
                                new Label("Category:"), categoryFilter, refreshProducts);
                productsBar.setAlignment(Pos.CENTER_LEFT);
                productsBar.setPadding(new Insets(5, 0, 10, 0));

                ProductGrid productGrid = new ProductGrid(
                                product -> new ProductDetailsDialog(stage, product, orderDAO, user, loadOrders)
                                                .showAndWait());

                Runnable loadCategories = () -> {
                        List<String> categories = new ArrayList<>();
                        categories.add("all");

                        try (Connection conn = com.jewelleryapp.dao.DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(
                                                        "SELECT DISTINCT LOWER(TRIM(type)) AS type FROM products " +
                                                                        "WHERE type IS NOT NULL AND TRIM(type) != '' ORDER BY type");
                                        ResultSet rs = ps.executeQuery()) {

                                while (rs.next())
                                        categories.add(rs.getString("type"));
                        } catch (SQLException ex) {
                                System.err.println("Category load error: " + ex.getMessage());
                        }

                        categoryFilter.setItems(FXCollections.observableArrayList(categories));
                        categoryFilter.setValue("all");
                };

                Runnable loadProducts = () -> {
                        String cat = categoryFilter.getValue();
                        List<Product> products = (cat == null || cat.equals("all"))
                                        ? productDAO.listAll()
                                        : productDAO.listByType(cat);
                        productGrid.setItems(products);
                };

                categoryFilter.setOnAction(e -> loadProducts.run());
                refreshProducts.setOnAction(e -> loadProducts.run());

                VBox productsPanel = new VBox(10, productsLabel, productsBar, productGrid);
                productsPanel.setPadding(new Insets(12));
                productsPanel.getStyleClass().add("card");

                // ---------- CENTER SPLIT ----------
                SplitPane splitPane = new SplitPane(productsPanel, ordersPanel);
                splitPane.setDividerPositions(0.55);

                // ---------- TOP BAR ----------
                String name = (user != null && user.getUserName() != null && !user.getUserName().isBlank())
                                ? user.getUserName()
                                : "Customer";

                Label welcomeLabel = new Label("Welcome " + name); // <-- match Admin Dashboard header style

                Button logoutButton = new Button("Logout");
                logoutButton.setOnAction(e -> ScreenRouter.goToLogin(stage));

                BorderPane topBar = new BorderPane();
                topBar.setCenter(welcomeLabel);
                topBar.setRight(logoutButton);
                BorderPane.setAlignment(welcomeLabel, Pos.CENTER);
                BorderPane.setMargin(logoutButton, new Insets(0, 10, 0, 0));
                topBar.setPadding(new Insets(10));
                topBar.getStyleClass().add("toolbar");

                // ---------- ROOT ----------
                BorderPane root = new BorderPane();
                root.setTop(topBar);
                root.setCenter(splitPane);

                // ✅ FIX: Keep full screen state
                ScreenRouter.replaceSceneContent(stage, root, 1180, 720);
                Scene scene = stage.getScene();

                // Apply the theme first (it was overriding your fonts)
                UIKit.apply(scene);

                // ✅ NOW FORCE the sizes using inline styles (inline style overrides the
                // stylesheet)
                // Admin header style: Verdana 28, #333333, padding like AdminDashboard
                welcomeLabel.setStyle(
                                "-fx-font-family: 'Verdana';" +
                                                "-fx-font-size: 28px;" +
                                                "-fx-font-weight: normal;" +
                                                "-fx-text-fill: #333333;");
                welcomeLabel.setPadding(new Insets(12, 0, 8, 0));

                // Bigger section headings so they don't look tiny
                String sectionHeadingStyle = "-fx-font-family: 'Verdana';" +
                                "-fx-font-size: 24px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #333333;";

                productsLabel.setStyle(sectionHeadingStyle);
                ordersLabel.setStyle(sectionHeadingStyle);

                stage.setTitle("Kanchan Cast — Customer Dashboard");
                stage.show();

                // ---------- INITIAL LOAD ----------
                loadCategories.run();
                loadProducts.run();
                loadOrders.run();
        }
}

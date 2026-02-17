package com.kanchancast.dashboard;

import javafx.application.Platform;
import com.kanchancast.dialogs.AdminDetailsDialog;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import com.kanchancast.nav.ScreenRouter;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import com.kanchancast.ui.PopupUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OwnerDashboard {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void show(Stage stage, User ownerUser) {
        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();
        UserDAO userDAO = new UserDAO();

        // ===== HEADER =====
        Label header = new Label("Owner Dashboard");
        header.setFont(Font.font("Verdana", 28));
        header.setTextFill(Color.web("#333333"));
        header.setPadding(new Insets(12, 0, 8, 0));

        // ===== KPI CARDS =====
        Label totalOrdersVal = new Label("-");
        Label completedOrdersVal = new Label("-");
        Label inProgressVal = new Label("-");
        Label revenueVal = new Label("-");
        Label topProductVal = new Label("-");

        VBox card1 = buildCard("📦 Total Orders", totalOrdersVal);
        VBox card2 = buildCard("✅ Completed", completedOrdersVal);
        VBox card3 = buildCard("⏳ In Progress", inProgressVal);
        VBox card4 = buildCard("💰 Est. Revenue (Completed)", revenueVal);
        VBox card5 = buildCard("⭐ Top Product", topProductVal);

        HBox summaryCards = new HBox(14, card1, card2, card3, card4, card5);
        summaryCards.setAlignment(Pos.CENTER);
        summaryCards.setPadding(new Insets(10, 0, 14, 0));

        // ===== CHARTS =====
        PieChart statusChart = new PieChart();
        statusChart.setTitle("Order Status Breakdown");
        statusChart.setLabelsVisible(true);
        statusChart.setLegendVisible(true);

        CategoryAxis prodX = new CategoryAxis();
        prodX.setLabel("Product");
        prodX.setTickLabelRotation(60);
        prodX.setTickLabelGap(10);
        prodX.setTickLabelFont(Font.font("Arial", 11));

        NumberAxis prodY = new NumberAxis();
        prodY.setLabel("Orders");
        BarChart<String, Number> topProductsChart = new BarChart<>(prodX, prodY);
        topProductsChart.setTitle("Top Products (by Orders)");
        topProductsChart.setLegendVisible(false);
        topProductsChart.setCategoryGap(18);
        topProductsChart.setBarGap(4);
        XYChart.Series<String, Number> topProductsSeries = new XYChart.Series<>();
        topProductsChart.getData().add(topProductsSeries);

        CategoryAxis monthX = new CategoryAxis();
        monthX.setLabel("Month");
        monthX.setTickLabelRotation(45);
        monthX.setTickLabelGap(6);

        NumberAxis monthY = new NumberAxis();
        monthY.setLabel("Orders");
        LineChart<String, Number> trendChart = new LineChart<>(monthX, monthY);
        trendChart.setTitle("Orders Trend (Last 6 Months)");
        trendChart.setLegendVisible(false);
        XYChart.Series<String, Number> trendSeries = new XYChart.Series<>();
        trendChart.getData().add(trendSeries);

        // ✅ avoids first-load jitter/misalignment
        statusChart.setAnimated(false);
        topProductsChart.setAnimated(false);
        trendChart.setAnimated(false);

        topProductsChart.setVerticalGridLinesVisible(false);
        topProductsChart.setAlternativeColumnFillVisible(false);

        // ===== TABLE: PRODUCTS =====
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

        VBox productTab = new VBox(10, new Label("All Products"), btnRefreshProducts, productTable);
        productTab.setPadding(new Insets(10));

        // ===== TABLE: ORDERS =====
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

        TableColumn<OrderSummary, String> oDelivery = new TableColumn<>("Delivery Date");
        oDelivery.setCellValueFactory(new PropertyValueFactory<>("deliveryDate"));

        TableColumn<OrderSummary, Integer> oProgress = new TableColumn<>("Progress %");
        oProgress.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getProgressPercent()).asObject());

        ordersTable.getColumns().addAll(oId, oUser, oProduct, oDate, oDelivery, oStatus, oProgress);

        Button btnRefreshOrders = new Button("Refresh Orders");
        VBox ordersTab = new VBox(10, new Label("All Orders"), btnRefreshOrders, ordersTable);
        ordersTab.setPadding(new Insets(10));

        // ===== TABLE: USERS (ADMINS ONLY) =====
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

        TableColumn<User, Integer> uAge = new TableColumn<>("Age");
        uAge.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getAge()).asObject());

        TableColumn<User, String> uAddress = new TableColumn<>("Address");
        uAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<User, String> uArea = new TableColumn<>("Work Area");
        uArea.setCellValueFactory(new PropertyValueFactory<>("area"));

        usersTable.getColumns().addAll(uId, uName, uType, uGender, uAge, uAddress, uArea);

        Button btnRefreshUsers = new Button("Refresh Users");
        Button btnDeleteUser = new Button("Delete Selected");
        Button btnViewUser = new Button("View Selected");
        Button btnAddAdmin = new Button("Create New Admin");

        Runnable loadUsers = () -> {
            List<User> users = userDAO.listAll();
            usersTable.setItems(FXCollections.observableArrayList(
                    users.stream().filter(u -> eqType(u, "admin")).toList()));
        };

        btnRefreshUsers.setOnAction(e -> loadUsers.run());

        btnViewUser.setOnAction(e -> {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Select an admin first!");
                return;
            }
            AdminDetailsDialog.show(stage, userDAO, selected.getUserId());
        });

        btnDeleteUser.setOnAction(e -> {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                PopupUtil.showWarn(stage, "Select a user first!");
                return;
            }
            if (eqType(selected, "owner")) {
                PopupUtil.showWarn(stage, "You cannot delete the Owner account!");
                return;
            }
            boolean deleted = userDAO.deleteUser(selected.getUserId());
            if (deleted) {
                PopupUtil.showInfo(stage, "✅ User deleted successfully!");
            } else {
                PopupUtil.showError(stage, "⚠️ Failed to delete user!");
            }
            loadUsers.run();
        });

        // ✅ UPDATED: create admin -> generate user_code, save it, show popup with code
        btnAddAdmin.setOnAction(e -> {
            Dialog<User> dialog = new Dialog<>();
            dialog.initOwner(stage); // ✅ FIX: Set owner to keep it on top of the dashboard
            dialog.setTitle("Create New Administrator");

            PopupUtil.prepareDialog(stage, dialog);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));

            TextField nameField = new TextField();
            PasswordField passField = new PasswordField();
            TextField addressField = new TextField();

            ComboBox<String> genderBox = new ComboBox<>();
            genderBox.getItems().addAll("Male", "Female");
            genderBox.setPromptText("Select");

            TextField workAreaField = new TextField();
            workAreaField.setPromptText("e.g., Head Office");

            DatePicker dobPicker = new DatePicker();
            dobPicker.setPromptText("Select");

            grid.addRow(0, new Label("Username:"), nameField);
            grid.addRow(1, new Label("Password:"), passField);
            grid.addRow(2, new Label("Address:"), addressField);
            grid.addRow(3, new Label("Gender:"), genderBox);
            grid.addRow(4, new Label("Work Area:"), workAreaField);
            grid.addRow(5, new Label("Date of Birth:"), dobPicker);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    String username = nameField.getText() == null ? "" : nameField.getText().trim();
                    String password = passField.getText() == null ? "" : passField.getText().trim();
                    String address = addressField.getText() == null ? "" : addressField.getText().trim();
                    String gender = genderBox.getValue();
                    String workArea = workAreaField.getText() == null ? "" : workAreaField.getText().trim();
                    LocalDate dob = dobPicker.getValue();

                    if (username.isEmpty() || password.isEmpty() || address.isEmpty()
                            || gender == null || dob == null || workArea.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "All fields are required. Please complete Username, Password, Address, Gender, Work Area and Date of Birth.");
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                        return null;
                    }
                    if (username.length() < 3) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Username must be at least 3 characters.");
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                        return null;
                    }
                    if (username.contains(" ")) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Username cannot contain spaces.");
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                        return null;
                    }
                    if (password.length() < 4) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Password must be at least 4 characters.");
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                        return null;
                    }
                    if (dob.isAfter(LocalDate.now())) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Date of Birth cannot be in the future.");
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                        return null;
                    }

                    // ✅ Generate a unique admin user_code
                    String userCode = generateUniqueAdminCode(userDAO);

                    boolean ok = userDAO.createUserWithCode(
                            "admin",
                            username,
                            password,
                            address,
                            gender,
                            workArea,
                            dob.format(ISO),
                            userCode);

                    if (ok) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                "✅ Admin created successfully!\n\nUser Code: " + userCode);
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "⚠️ Failed to create admin!");
                        alert.initOwner(dialog.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                    }

                    loadUsers.run();
                }
                return null;
            });

            dialog.showAndWait();
        });

        HBox userButtons = new HBox(10, btnRefreshUsers, btnAddAdmin, btnViewUser, btnDeleteUser);
        VBox usersTab = new VBox(10, new Label("Administrators"), userButtons, usersTable);
        usersTab.setPadding(new Insets(10));

        // ===== REFRESH ALL =====
        Runnable refreshAll = () -> {
            List<Product> products = productDAO.listAll();
            List<OrderSummary> orders = orderDAO.listAll();

            productTable.setItems(FXCollections.observableArrayList(products));
            ordersTable.setItems(FXCollections.observableArrayList(orders));
            loadUsers.run();

            totalOrdersVal.setText(String.valueOf(orders.size()));

            long completed = orders.stream().filter(o -> o.getProgressPercent() >= 100).count();
            long inProgress = orders.stream().filter(o -> o.getProgressPercent() < 100).count();
            completedOrdersVal.setText(String.valueOf(completed));
            inProgressVal.setText(String.valueOf(inProgress));

            Map<Integer, Double> priceByProductId = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, Product::getPrice, (a, b) -> a));

            double revenue = orders.stream()
                    .filter(o -> o.getProgressPercent() >= 100)
                    .mapToDouble(o -> priceByProductId.getOrDefault(o.getProductId(), 0.0))
                    .sum();

            // Rupees sign
            revenueVal.setText(String.format("₹%.2f", revenue));

            String topProduct = "N/A";
            if (!orders.isEmpty()) {
                Map<Integer, Long> counts = orders.stream()
                        .collect(Collectors.groupingBy(OrderSummary::getProductId, Collectors.counting()));
                Optional<Integer> topPid = counts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey);
                if (topPid.isPresent()) {
                    int pid = topPid.get();
                    topProduct = products.stream()
                            .filter(p -> p.getProductId() == pid)
                            .map(Product::getName)
                            .findFirst()
                            .orElse("Product #" + pid);
                }
            }
            topProductVal.setText(topProduct);

            long total = Math.max(1, orders.size());
            // ✅ FIX: Use incremental update specific for Owner Dashboard status chart
            Map<String, Long> statusData = new HashMap<>();
            statusData.put("Completed", completed);
            statusData.put("In Progress", total - completed);
            updatePieChartData(statusChart, statusData);

            // Top products bar chart shows max 6 products, or all if fewer exist
            topProductsSeries.getData().clear();

            // Count orders per productId
            Map<Integer, Long> ordersByPid = orders.stream()
                    .collect(Collectors.groupingBy(OrderSummary::getProductId, Collectors.counting()));

            // Sort all products by order count desc, then by name
            List<Product> sortedProducts = new ArrayList<>(products);
            sortedProducts.sort(
                    Comparator.comparingLong((Product p) -> ordersByPid.getOrDefault(p.getProductId(), 0L))
                            .reversed()
                            .thenComparing(p -> safe(p.getName(), ""), String.CASE_INSENSITIVE_ORDER));

            int limit = Math.min(6, sortedProducts.size());
            for (int i = 0; i < limit; i++) {
                Product p = sortedProducts.get(i);
                long count = ordersByPid.getOrDefault(p.getProductId(), 0L);

                String label = safe(p.getName(), "Unknown");
                topProductsSeries.getData().add(new XYChart.Data<>(label, count));
            }

            trendSeries.getData().clear();
            Map<YearMonth, Long> perMonth = new HashMap<>();
            for (OrderSummary o : orders) {
                YearMonth ym = parseYearMonth(o.getDateOrdered());
                if (ym != null)
                    perMonth.merge(ym, 1L, Long::sum);
            }
            YearMonth now = YearMonth.now();
            monthX.getCategories().clear();
            for (int i = 5; i >= 0; i--) {
                YearMonth m = now.minusMonths(i);
                String label = m.getMonth().name().substring(0, 3) + " " + m.getYear();
                monthX.getCategories().add(label);

                long cnt = perMonth.getOrDefault(m, 0L);
                trendSeries.getData().add(new XYChart.Data<>(label, cnt));
            }
        };

        btnRefreshProducts.setOnAction(e -> refreshAll.run());
        btnRefreshOrders.setOnAction(e -> refreshAll.run());

        Tab t1 = new Tab("Products", productTab);
        Tab t2 = new Tab("Orders", ordersTab);
        Tab t3 = new Tab("Users", usersTab);

        TabPane tabs = new TabPane(t1, t2, t3);
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        HBox chartBox = new HBox(36, statusChart, topProductsChart, trendChart);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPadding(new Insets(10, 10, 10, 10));
        chartBox.setMaxHeight(360);
        chartBox.setPrefHeight(360);

        statusChart.setMinHeight(320);
        topProductsChart.setMinHeight(320);
        trendChart.setMinHeight(320);

        Button refreshBtn = new Button("Refresh Dashboard");
        refreshBtn.setOnAction(e -> refreshAll.run());

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("""
                    -fx-background-color: #b83b5e;
                    -fx-text-fill: white;
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                """);
        logoutBtn.setOnAction(e -> ScreenRouter.goToLogin(stage));

        HBox topBar = new HBox(10, refreshBtn, logoutBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10, 20, 10, 20));

        VBox content = new VBox(14, header, summaryCards, chartBox, tabs);
        content.setPadding(new Insets(18));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("""
                    -fx-background-color: linear-gradient(to bottom right, #fdfbfb, #ebedee);
                    -fx-font-family: 'Segoe UI', sans-serif;
                """);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(content);

        // ✅ FIX: Use replaceSceneContent to preserve full-screen state
        ScreenRouter.replaceSceneContent(stage, root, 1280, 820);
        stage.setTitle("Kanchan Cast — Owner Dashboard");
        stage.show();

        Platform.runLater(refreshAll);
    }

    // ✅ helper: generate a unique code using existing users list (no extra DB
    // method needed)
    private static String generateUniqueAdminCode(UserDAO dao) {
        Set<String> existing = dao.listAll().stream()
                .map(User::getUserCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        for (int tries = 0; tries < 20; tries++) {
            String code = dao.generateUserCode("KC-ADM");
            if (!existing.contains(code))
                return code;
        }
        // fallback (very unlikely)
        return "KC-ADM" + System.currentTimeMillis();
    }

    private static boolean eqType(User u, String type) {
        if (u == null || u.getUserType() == null)
            return false;
        return u.getUserType().trim().equalsIgnoreCase(type);
    }

    private static String safe(String s, String fallback) {
        if (s == null)
            return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static YearMonth parseYearMonth(String dateStr) {
        if (dateStr == null)
            return null;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim(), ISO);
            return YearMonth.of(d.getYear(), d.getMonth());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static VBox buildCard(String title, Label valueLbl) {
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", 13));
        titleLbl.setTextFill(Color.GRAY);

        valueLbl.setFont(Font.font("Arial", 22));
        valueLbl.setTextFill(Color.web("#333333"));
        valueLbl.setStyle("-fx-font-weight: bold;");

        VBox card = new VBox(5, titleLbl, valueLbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        card.setPrefWidth(200);
        card.setMinWidth(185);

        card.setStyle("""
                    -fx-background-color: white;
                    -fx-border-radius: 12;
                    -fx-background-radius: 12;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.3, 0, 3);
                """);

        return card;
    }

    private static void updatePieChartData(PieChart chart, Map<String, Long> newData) {
        var currentData = chart.getData();
        currentData.removeIf(d -> !newData.containsKey(d.getName()));

        for (var entry : newData.entrySet()) {
            String category = entry.getKey();
            double value = entry.getValue();

            var existingData = currentData.stream()
                    .filter(d -> d.getName().equals(category))
                    .findFirst();

            if (existingData.isPresent()) {
                existingData.get().setPieValue(value);
            } else {
                currentData.add(new PieChart.Data(category, value));
            }
        }
    }
}

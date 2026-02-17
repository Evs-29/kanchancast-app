package com.kanchancast.dashboard;

import com.kanchancast.ui.PopupUtil;
import com.jewelleryapp.dao.EmployeeDAO;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.Product;
import com.kanchancast.model.StaffRow;
import com.kanchancast.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class AdminDashboard {

    public static void show(Stage stage, User adminUser) {

        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();
        EmployeeDAO employeeDAO = new EmployeeDAO();
        UserDAO userDAO = new UserDAO();

        // ===== HEADER =====
        Label header = new Label("Admin Dashboard");
        header.setFont(Font.font("Verdana", 28));
        header.setTextFill(Color.web("#333333"));
        header.setPadding(new Insets(12, 0, 8, 0));

        // ===== KPI CARDS =====
        Label totalOrdersVal = new Label("-");
        Label completedOrdersVal = new Label("-");
        Label inProgressVal = new Label("-");
        Label topProductVal = new Label("-");
        Label topEmployeeVal = new Label("-");

        VBox card1 = buildCard("📦 Total Orders", totalOrdersVal);
        VBox card2 = buildCard("✅ Orders Fully Completed", completedOrdersVal);
        VBox card3 = buildCard("⏳ Orders In Progress", inProgressVal);
        VBox card4 = buildCard("⭐ Top Product", topProductVal);
        VBox card5 = buildCard("👑 Top Employee", topEmployeeVal);

        HBox summaryCards = new HBox(14, card1, card2, card3, card4, card5);
        summaryCards.setAlignment(Pos.CENTER);
        summaryCards.setPadding(new Insets(10, 0, 14, 0));

        // ===== PIE CHART =====
        PieChart categoryChart = new PieChart();
        categoryChart.setTitle("Product Category Breakdown");
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);
        categoryChart.setAnimated(false); // ✅ avoid first-frame jitter

        // ===== BAR CHART (Top performers) =====
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Employee");

        // ✅ improve initial readability & spacing
        xAxis.setTickLabelRotation(35);
        xAxis.setTickLabelGap(8);
        xAxis.setTickLabelFont(Font.font("Arial", 11));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Stages Completed");

        BarChart<String, Number> performanceChart = new BarChart<>(xAxis, yAxis);
        performanceChart.setTitle("Top Performers (Stages Completed)");
        performanceChart.setLegendVisible(false);

        performanceChart.setCategoryGap(30);
        performanceChart.setBarGap(8);

        performanceChart.setVerticalGridLinesVisible(false);
        performanceChart.setAlternativeColumnFillVisible(false);

        // (optional, but nice) keep it clean
        performanceChart.setHorizontalGridLinesVisible(true);
        performanceChart.setAlternativeRowFillVisible(false);

        // ✅ important: animation can cause misalignment on first render
        performanceChart.setAnimated(false);

        // Force integer ticks + visible y-axis scale
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        XYChart.Series<String, Number> performanceData = new XYChart.Series<>();
        performanceChart.getData().add(performanceData);

        // ===== REFRESH FUNCTION =====
        Runnable refreshAll = () -> {
            var allOrders = orderDAO.listAll();
            var allProducts = productDAO.listALL();

            // KPIs
            totalOrdersVal.setText(String.valueOf(allOrders.size()));

            long completedOrders = allOrders.stream()
                    .filter(o -> o.getProgressPercent() >= 100)
                    .count();
            completedOrdersVal.setText(String.valueOf(completedOrders));

            long inProgress = allOrders.stream()
                    .filter(o -> o.getProgressPercent() < 100)
                    .count();
            inProgressVal.setText(String.valueOf(inProgress));

            // Top product by order frequency
            String topProduct = "N/A";
            if (!allOrders.isEmpty()) {
                Map<Integer, Long> counts = allOrders.stream()
                        .collect(Collectors.groupingBy(o -> o.getProductId(), Collectors.counting()));

                Optional<Integer> topPid = counts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey);

                if (topPid.isPresent()) {
                    int pid = topPid.get();
                    topProduct = allProducts.stream()
                            .filter(p -> p.getProductId() == pid)
                            .map(Product::getName)
                            .findFirst()
                            .orElse("Product #" + pid);
                }
            }
            topProductVal.setText(topProduct);

            // Pie chart categories
            Map<String, Long> pieData = allProducts.stream()
                    .collect(Collectors.groupingBy(
                            p -> safeString(p.getType(), "Unknown"),
                            Collectors.counting()));

            //
            updatePieChartData(categoryChart, pieData);

            // ===== Bar chart: TOP 8 leaderboard =====
            performanceData.getData().clear();

            List<StaffRow> stats = userDAO.listEmployeeStats();
            if (stats == null || stats.isEmpty()) {
                yAxis.setUpperBound(1);
                topEmployeeVal.setText("N/A");
                return;
            }

            List<StaffRow> sorted = stats.stream()
                    .sorted(Comparator
                            .comparingInt(StaffRow::getOrdersDone).reversed()
                            .thenComparing(s -> safeString(s.getUserName(), "")))
                    .collect(Collectors.toList());

            int topN = Math.min(8, sorted.size());
            List<StaffRow> top = sorted.subList(0, topN);

            // top employee (by stages completed)
            StaffRow best = top.get(0);
            topEmployeeVal.setText(safeString(best.getUserName(), "N/A") + " (" + best.getOrdersDone() + ")");

            int maxDone = Math.max(1, top.stream().mapToInt(StaffRow::getOrdersDone).max().orElse(1));
            yAxis.setUpperBound(maxDone + 1); // breathing room

            for (StaffRow emp : top) {
                String name = safeString(emp.getUserName(), "Employee");
                int done = emp.getOrdersDone();
                performanceData.getData().add(new XYChart.Data<>(name, done));
            }

            // Add labels/tooltips only AFTER JavaFX has created/laid out the bar nodes.
            Platform.runLater(() -> {
                performanceChart.applyCss();
                performanceChart.layout();

                for (XYChart.Data<String, Number> d : performanceData.getData()) {
                    if (d.getNode() == null)
                        continue;

                    int val = d.getYValue() == null ? 0 : d.getYValue().intValue();
                    String labelText = String.valueOf(val);

                    Tooltip.install(d.getNode(),
                            new Tooltip(d.getXValue() + ": " + labelText + " stages"));

                    Label valueLabel = new Label(labelText);
                    valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

                    StackPane bar = (StackPane) d.getNode();

                    // prevent duplicate labels on repeated refresh
                    bar.getChildren().removeIf(n -> n instanceof Label);

                    bar.getChildren().add(valueLabel);
                    StackPane.setAlignment(valueLabel, Pos.TOP_CENTER);
                    valueLabel.setTranslateY(-14);
                }
            });
        };

        // ===== CHART LAYOUT =====
        HBox chartBox = new HBox(50, categoryChart, performanceChart);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPadding(new Insets(10, 10, 10, 10));
        chartBox.setMaxHeight(360);
        chartBox.setPrefHeight(360);

        categoryChart.setMinHeight(320);
        performanceChart.setMinHeight(320);

        // ===== ADMIN TABS =====
        TabPane tabs = AdminTabs.buildTabs(stage, productDAO, orderDAO, employeeDAO, refreshAll);
        tabs.setPadding(new Insets(10, 0, 10, 0));
        tabs.setTabMinWidth(110);
        tabs.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // ===== LOGOUT BUTTON =====
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("""
                    -fx-background-color: #b83b5e;
                    -fx-text-fill: white;
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                """);
        logoutBtn.setOnAction(e -> {
            // ✅ Do NOT close the stage — just route back to login on same stage
            com.kanchancast.nav.ScreenRouter.goToLogin(stage);
        });

        HBox topBar = new HBox(logoutBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10, 20, 10, 20));

        // ===== MAIN LAYOUT =====
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

        // ✅ FIX: Keep full screen state
        com.kanchancast.nav.ScreenRouter.replaceSceneContent(stage, root, 1280, 820);
        stage.setTitle("Kanchan Cast — Admin Dashboard");
        stage.show();

        // ✅ FIX: run first refresh after the window has been laid out (prevents ugly
        // initial chart)
        Platform.runLater(refreshAll);
    }

    private static String safeString(String s, String fallback) {
        if (s == null)
            return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
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

        // 1. Remove categories that no longer exist
        currentData.removeIf(d -> !newData.containsKey(d.getName()));

        // 2. Update existing or add new
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

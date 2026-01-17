package com.kanchancast.dashboard;

import com.jewelleryapp.dao.EmployeeDAO;
import com.jewelleryapp.dao.OrderDAO;
import com.jewelleryapp.dao.ProductDAO;
import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboard {

    public static void show(Stage stage, User adminUser) {

        ProductDAO productDAO = new ProductDAO();
        OrderDAO orderDAO = new OrderDAO();
        EmployeeDAO employeeDAO = new EmployeeDAO();

        // ===== HEADER =====
        Label header = new Label("Admin Dashboard");
        header.setFont(Font.font("Verdana", 28));
        header.setTextFill(Color.web("#333333"));
        header.setPadding(new Insets(20, 0, 10, 0));

        // ===== KPI SUMMARY CARDS =====
        int totalOrders = orderDAO.listAll().size();

        // Replace .getProgressPercent() with a proper field like .getTotalPrice() if available
        double totalRevenue = orderDAO.listAll().stream()
                .mapToDouble(o -> o.getProgressPercent())
                .sum();

        String topProduct = productDAO.listALL().stream()
                .map(Product::getName)
                .findFirst().orElse("N/A");

        VBox card1 = buildCard("📦 Total Orders", String.valueOf(totalOrders));
        VBox card2 = buildCard("💰 Total Revenue", "$" + String.format("%.2f", totalRevenue));
        VBox card3 = buildCard("⭐ Top Product", topProduct);

        HBox summaryCards = new HBox(30, card1, card2, card3);
        summaryCards.setAlignment(Pos.CENTER);
        summaryCards.setPadding(new Insets(20, 0, 30, 0));

        // ===== PIE CHART: Product Category Breakdown =====
        List<Product> products = productDAO.listALL();
        PieChart categoryChart = new PieChart();
        categoryChart.setTitle("Product Category Breakdown");

        products.stream()
                .collect(Collectors.groupingBy(Product::getType, Collectors.counting()))
                .forEach((type, count) ->
                        categoryChart.getData().add(new PieChart.Data(type, count))
                );

        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);

        // ===== BAR CHART: Employee Performance =====
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Employee");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Orders Completed");

        BarChart<String, Number> performanceChart = new BarChart<>(xAxis, yAxis);
        performanceChart.setTitle("Employee Performance");

        XYChart.Series<String, Number> performanceData = new XYChart.Series<>();
        performanceData.setName("Orders Done");

        employeeDAO.listAll().forEach(emp ->
                performanceData.getData().add(
                        new XYChart.Data<>(emp.getUserName(), emp.getOrdersDone())
                )
        );

        performanceChart.getData().add(performanceData);

        // ===== CHART LAYOUT =====
        HBox chartBox = new HBox(50, categoryChart, performanceChart);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPadding(new Insets(20, 10, 20, 10));

        // ===== ADMIN TABS =====
        TabPane tabs = AdminTabs.buildTabs(stage, productDAO, orderDAO, employeeDAO);
        tabs.setPadding(new Insets(10, 0, 20, 0));

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
            stage.close();
            com.kanchancast.nav.ScreenRouter.goToLogin(new Stage());
        });

        HBox topBar = new HBox(logoutBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10, 20, 10, 20));

        // ===== MAIN LAYOUT =====
        VBox content = new VBox(20, header, summaryCards, chartBox, tabs);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #fdfbfb, #ebedee);
            -fx-font-family: 'Segoe UI', sans-serif;
        """);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(content);

        Scene scene = new Scene(root, 1280, 820);
        stage.setScene(scene);
        stage.setTitle("Kanchan Cast — Admin Dashboard");
        stage.show();
    }

    // ===== Helper method for KPI Cards =====
    private static VBox buildCard(String title, String value) {
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", 14));
        titleLbl.setTextFill(Color.GRAY);

        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font("Arial", 22));
        valueLbl.setTextFill(Color.web("#333333"));
        valueLbl.setStyle("-fx-font-weight: bold;");

        VBox card = new VBox(5, titleLbl, valueLbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setPrefWidth(200);
        card.setStyle("""
            -fx-background-color: white;
            -fx-border-radius: 12;
            -fx-background-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.3, 0, 3);
        """);

        return card;
    }
}

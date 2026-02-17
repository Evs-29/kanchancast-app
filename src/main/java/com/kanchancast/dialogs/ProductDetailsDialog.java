package com.kanchancast.dialogs;

import com.kanchancast.model.Product;
import com.kanchancast.ui.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.lang.reflect.Method;

public class ProductDetailsDialog {

    public static void show(Stage owner, Product p) {
        if (p == null) return;

        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Product Details");

        Label title = new Label(safe(str(p, "getName"), "Product"));
        title.setFont(Font.font("Segoe UI", 22));
        title.setStyle("-fx-font-weight: bold;");

        Label subtitle = new Label("Product ID: " + safe(num(p, "getProductId"), "N/A"));
        subtitle.setStyle("-fx-text-fill: #666666;");

        VBox header = new VBox(4, title, subtitle);

        String imgPath = str(p, "getImagePath");
        ImageView image = ImageUtil.getProductImage(imgPath, 280, 220);
        StackPane imageCard = new StackPane(image);
        imageCard.setPadding(new Insets(10));
        imageCard.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0.3, 0, 4);
        """);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));

        int r = 0;
        addRow(grid, r++, "Category", safe(str(p, "getType"), safe(str(p, "getCategory"), "N/A")));
        addRow(grid, r++, "Price", safe(num(p, "getPrice"), safe(num(p, "getProductPrice"), "N/A")));
        addRow(grid, r++, "Diamond Weight", safe(num(p, "getDiamondWeight"), "N/A"));
        addRow(grid, r++, "Gold Weight", safe(num(p, "getGoldWeight"), "N/A"));

        // ✅ Better stone weight display: show N/A if null or 0
        addRow(grid, r++, "Stone Weight", formatStoneWeight(p));

        // ✅ NEW: Duration
        addRow(grid, r++, "Duration", formatDuration(p));

        String desc = safe(str(p, "getDescription"), "No description provided.");
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #333333;");

        VBox descBox = new VBox(8, new Label("Description"), descLabel);
        descBox.setPadding(new Insets(12));
        descBox.setStyle("""
            -fx-background-color: #fafafa;
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-border-color: #e7e7e7;
        """);

        VBox right = new VBox(12, grid, new Separator(), descBox);
        right.setPrefWidth(430);

        HBox content = new HBox(18, imageCard, right);
        content.setAlignment(Pos.TOP_LEFT);

        Button closeBtn = new Button("Close");
        closeBtn.setDefaultButton(true);
        closeBtn.setStyle("""
            -fx-background-color: #b83b5e;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-cursor: hand;
        """);
        closeBtn.setOnAction(e -> dlg.close());

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(14, header, content, footer);
        root.setPadding(new Insets(18));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #fdfbfb, #ebedee);
            -fx-font-family: 'Segoe UI';
        """);

        Scene scene = new Scene(root, 820, 520);
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    private static void addRow(GridPane grid, int row, String key, String value) {
        Label k = new Label(key + ":");
        k.setStyle("-fx-text-fill: #666666; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #222222;");
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private static String formatStoneWeight(Product p) {
        try {
            Double sw = p.getStoneWeight();
            if (sw == null || sw <= 0.0) return "N/A";
            return String.valueOf(sw);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String formatDuration(Product p) {
        try {
            int amt = p.getDurationAmount();
            if (amt <= 0) return "N/A";
            String unit = (p.getDurationUnit() == null) ? "DAYS" : p.getDurationUnit().trim().toUpperCase();
            String word = switch (unit) {
                case "WEEKS" -> (amt == 1 ? "week" : "weeks");
                case "MONTHS" -> (amt == 1 ? "month" : "months");
                default -> (amt == 1 ? "day" : "days");
            };
            return amt + " " + word;
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String str(Object obj, String getter) {
        try {
            Method m = obj.getClass().getMethod(getter);
            Object val = m.invoke(obj);
            return val == null ? null : String.valueOf(val);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String num(Object obj, String getter) {
        try {
            Method m = obj.getClass().getMethod(getter);
            Object val = m.invoke(obj);
            if (val == null) return null;
            return String.valueOf(val);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }
}

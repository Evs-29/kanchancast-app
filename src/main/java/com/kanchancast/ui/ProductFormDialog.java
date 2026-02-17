package com.kanchancast.ui;

import com.jewelleryapp.dao.ProductDAO;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class ProductFormDialog extends Dialog<Boolean> {

    private final TextField name = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField price = new TextField();
    private final TextField goldW = new TextField();
    private final TextField diamondW = new TextField();
    private final TextField stoneW = new TextField();
    private final TextField imagePath = new TextField();
    private final TextArea description = new TextArea();

    private final TextField durationAmount = new TextField();
    private final ComboBox<String> durationUnit = new ComboBox<>();

    public ProductFormDialog(Stage owner) {
        setTitle("Create Product");
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);

        ProductDAO dao = new ProductDAO();
        List<String> categories = dao.listAllCategories();
        if (categories.isEmpty()) categories.addAll(List.of("ring", "necklace", "bracelet", "earrings", "other"));
        category.getItems().addAll(categories);
        category.setValue(categories.get(0));

        description.setPromptText("Description");
        description.setPrefRowCount(3);

        installDecimalFilter(price);
        installDecimalFilter(goldW);
        installDecimalFilter(diamondW);
        installDecimalFilter(stoneW);

        installIntegerFilter(durationAmount);
        durationAmount.setPromptText("e.g. 2");
        durationUnit.getItems().addAll("Days", "Weeks", "Months");
        durationUnit.setValue("Days");

        imagePath.setPromptText("Select an image using Browse…");
        imagePath.setEditable(false);

        ImageView preview = ImageUtil.getProductImage("", 220, 130);

        Button browseBtn = new Button("Browse…");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Product Image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File chosen = fc.showOpenDialog(owner);
            if (chosen == null) return;

            try {
                Path destDir = ImageUtil.appImagesDir();
                Files.createDirectories(destDir);

                String ext = getExt(chosen.getName());
                String newName = "prod_" + UUID.randomUUID() + ext;
                Path dest = destDir.resolve(newName);

                Files.copy(chosen.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                imagePath.setText(newName);

                ImageView newPrev = ImageUtil.getProductImage(newName, 220, 130);
                preview.setImage(newPrev.getImage());

            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Could not save image: " + ex.getMessage(), ButtonType.OK).showAndWait();
            }
        });

        HBox imageRow = new HBox(10, imagePath, browseBtn);

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(8);
        gp.setPadding(new Insets(10));

        int r = 0;
        gp.add(new Label("Name"), 0, r); gp.add(name, 1, r++);
        gp.add(new Label("Category"), 0, r); gp.add(category, 1, r++);
        gp.add(new Label("Price"), 0, r); gp.add(price, 1, r++);
        gp.add(new Label("Gold Weight"), 0, r); gp.add(goldW, 1, r++);
        gp.add(new Label("Diamond Weight"), 0, r); gp.add(diamondW, 1, r++);
        gp.add(new Label("Stone Weight"), 0, r); gp.add(stoneW, 1, r++);

        HBox durRow = new HBox(10, durationAmount, durationUnit);
        gp.add(new Label("Duration"), 0, r); gp.add(durRow, 1, r++);

        gp.add(new Label("Image"), 0, r); gp.add(imageRow, 1, r++);
        gp.add(new Label("Preview"), 0, r); gp.add(preview, 1, r++);
        gp.add(new Label("Description"), 0, r); gp.add(description, 1, r++);

        getDialogPane().setContent(gp);

        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancel, ok);

        Node okBtn = getDialogPane().lookupButton(ok);
        okBtn.addEventFilter(javafx.event.ActionEvent.ANY, ev -> {
            try {
                String n = name.getText().trim();
                String t = category.getValue();
                String priceRaw = price.getText().trim();
                String gwRaw = goldW.getText().trim();
                String dwRaw = diamondW.getText().trim();
                String swRaw = stoneW.getText().trim();
                String img = imagePath.getText().trim();
                String desc = description.getText().trim();

                String durAmtRaw = durationAmount.getText().trim();
                String durUnitRaw = durationUnit.getValue();

                if (n.isEmpty()) { showError("Product name cannot be empty."); ev.consume(); return; }
                if (t == null || t.isBlank()) { showError("Category cannot be empty."); ev.consume(); return; }
                if (priceRaw.isEmpty()) { showError("Price cannot be empty."); ev.consume(); return; }
                if (gwRaw.isEmpty()) { showError("Gold weight cannot be empty."); ev.consume(); return; }
                if (dwRaw.isEmpty()) { showError("Diamond weight cannot be empty."); ev.consume(); return; }
                if (swRaw.isEmpty()) { showError("Stone weight cannot be empty."); ev.consume(); return; }

                if (durAmtRaw.isEmpty()) { showError("Duration amount cannot be empty."); ev.consume(); return; }
                int durAmt = Integer.parseInt(durAmtRaw);
                if (durAmt <= 0) { showError("Duration must be at least 1."); ev.consume(); return; }
                if (durUnitRaw == null || durUnitRaw.isBlank()) { showError("Duration unit cannot be empty."); ev.consume(); return; }

                if (img.isEmpty()) { showError("Please choose an image using Browse…"); ev.consume(); return; }
                if (desc.isEmpty()) { showError("Description cannot be empty."); ev.consume(); return; }

                double p = Double.parseDouble(priceRaw);
                double gw = Double.parseDouble(gwRaw);
                double dw = Double.parseDouble(dwRaw);
                double sw = Double.parseDouble(swRaw);

                if (p < 0) { showError("Price cannot be negative."); ev.consume(); return; }
                if (gw < 0 || dw < 0 || sw < 0) { showError("Weights cannot be negative."); ev.consume(); return; }

                String dbUnit = switch (durUnitRaw.toLowerCase()) {
                    case "weeks" -> "WEEKS";
                    case "months" -> "MONTHS";
                    default -> "DAYS";
                };

                // ✅ FIX: pass stoneWeight (sw) into DAO
                boolean saved = dao.createProduct(n, t, gw, dw, sw, p, img, desc, durAmt, dbUnit);

                if (!saved) {
                    showError("Could not save product. Check console for details.");
                    ev.consume();
                } else {
                    setResult(Boolean.TRUE);
                }
            } catch (NumberFormatException ex) {
                showError("Please check inputs: Price/weights must be numbers, duration must be a whole number.");
                ev.consume();
            } catch (Exception ex) {
                showError("Unexpected error: " + ex.getMessage());
                ev.consume();
            }
        });
    }

    private String getExt(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        return filename.substring(dot).toLowerCase();
    }

    private void installDecimalFilter(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) return change;
            if (next.matches("\\d*(\\.\\d*)?")) return change;
            return null;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }

    private void installIntegerFilter(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) return change;
            if (next.matches("\\d*")) return change;
            return null;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public static void show(Stage owner) {
        new ProductFormDialog(owner).showAndWait();
    }
}

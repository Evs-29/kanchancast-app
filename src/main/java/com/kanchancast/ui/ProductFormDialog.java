package com.kanchancast.ui;

import com.jewelleryapp.dao.ProductDAO;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class ProductFormDialog extends Dialog<Boolean> {

    private final TextField name = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField price = new TextField();
    private final TextField goldW = new TextField();
    private final TextField diamondW = new TextField();
    private final TextField stoneW = new TextField();
    private final TextField imagePath = new TextField();
    private final TextArea description = new TextArea();

    public ProductFormDialog(Stage owner) {
        setTitle("Create Product");
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);

        category.getItems().addAll("ring", "necklace", "bracelet", "earrings", "other");
        category.setValue("other");

        description.setPromptText("Description (optional)");
        description.setPrefRowCount(3);

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
        gp.add(new Label("Image Path"), 0, r); gp.add(imagePath, 1, r++);
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
                double p = Double.parseDouble(price.getText().trim());
                double gw = parseDoubleSafe(goldW.getText());
                double dw = parseDoubleSafe(diamondW.getText());
                double sw = parseDoubleSafe(stoneW.getText());
                String img = imagePath.getText().trim();
                String desc = description.getText().trim();

                boolean saved = new ProductDAO().createProduct(n, t, gw, dw, p, img, desc);
                if (!saved) {
                    new Alert(Alert.AlertType.ERROR, "Could not save product. Check console for details.", ButtonType.OK).showAndWait();
                    ev.consume();
                } else {
                    setResult(Boolean.TRUE);
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Please check inputs (price/weights must be numbers).", ButtonType.OK).showAndWait();
                ev.consume();
            }
        });
    }

    private double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return 0.0;
        return Double.parseDouble(s.trim());
    }
}

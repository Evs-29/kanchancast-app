package com.kanchancast.ui;

import com.kanchancast.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

public class ProductDetailsDialogController {

    @FXML private Label nameLabel;
    @FXML private Label typeLabel;
    @FXML private Label goldWeightLabel;
    @FXML private Label diamondWeightLabel;
    @FXML private Label stoneWeightLabel;
    @FXML private Label priceLabel;
    @FXML private Label descriptionLabel;
    @FXML private ImageView productImage;

    public void setProduct(Product p) {
        if (p == null) return;

        nameLabel.setText(p.getName());
        typeLabel.setText(p.getType());
        goldWeightLabel.setText(String.valueOf(p.getGoldWeight()));
        diamondWeightLabel.setText(String.valueOf(p.getDiamondWeight()));
        stoneWeightLabel.setText(String.valueOf(p.getStoneWeight()));
        priceLabel.setText("₹" + p.getPrice());
        descriptionLabel.setText(p.getDescription() != null ? p.getDescription() : "");

        if (p.getImagePath() != null && !p.getImagePath().isBlank()) {
            File f = new File(p.getImagePath());
            if (f.exists()) {
                productImage.setImage(new Image(f.toURI().toString(), 380, 200, true, true));
            }
        }
    }
}

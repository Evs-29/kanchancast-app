package com.kanchancast.ui;

import com.kanchancast.model.Product;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class ProductDetailsDialogController {

    private static final DecimalFormat ONE_DP = new DecimalFormat("0.0");
    private static final NumberFormat PRICE_FMT = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    @FXML private Label nameLabel;
    @FXML private Label typeLabel;
    @FXML private Label goldWeightLabel;
    @FXML private Label diamondWeightLabel;
    @FXML private Label stoneWeightLabel;
    @FXML private Label durationLabel;
    @FXML private Label priceLabel;

    @FXML private Label descriptionLabel;
    @FXML private ImageView productImage;

    public void setProduct(Product p) {
        if (p == null) return;

        nameLabel.setText(nvl(p.getName()));
        typeLabel.setText(nvl(p.getType()));

        goldWeightLabel.setText(fmtWeight(p.getGoldWeight()));
        diamondWeightLabel.setText(fmtWeight(p.getDiamondWeight()));
        stoneWeightLabel.setText(fmtWeight(p.getStoneWeight()));

        int amt = p.getDurationAmount();
        String unit = (p.getDurationUnit() == null) ? "DAYS" : p.getDurationUnit().trim().toUpperCase();

        if (amt <= 0) {
            durationLabel.setText("-");
        } else {
            String word = switch (unit) {
                case "WEEKS" -> (amt == 1 ? "week" : "weeks");
                case "MONTHS" -> (amt == 1 ? "month" : "months");
                default -> (amt == 1 ? "day" : "days");
            };
            durationLabel.setText(amt + " " + word);
        }

        PRICE_FMT.setMaximumFractionDigits(2);
        PRICE_FMT.setMinimumFractionDigits(0);
        priceLabel.setText("₹" + PRICE_FMT.format(p.getPrice()));

        String desc = (p.getDescription() == null) ? "" : p.getDescription().trim();
        descriptionLabel.setText(desc.isBlank() ? "-" : desc);

        try {
            ImageView iv = ImageUtil.getProductImage(p.getImagePath(), 380, 200);
            if (iv != null && iv.getImage() != null) productImage.setImage(iv.getImage());
        } catch (Exception ignored) {}
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    private static String fmtWeight(Double d) {
        if (d == null) return "-";
        return ONE_DP.format(d);
    }
}

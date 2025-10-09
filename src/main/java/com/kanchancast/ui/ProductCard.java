package com.kanchancast.ui;

import com.kanchancast.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * ProductCard component.
 * Displays product image, name, and price.
 * On click, opens a simple Product Details dialog with option to order.
 */
public class ProductCard extends VBox {
    private final Product product;

    public ProductCard(Product product, Consumer<Product> onOpen) {
        this.product = product;

        // ---- Layout styling ----
        setPadding(new Insets(10));
        setSpacing(8);
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("product-card");

        // ---- Image ----
        ImageView img = ImageUtil.getProductImage(product.getImagePath(), 300, 160);
        img.setPreserveRatio(false);
        img.setFitWidth(300);
        img.setFitHeight(160);

        // ---- Name ----
        Label name = new Label(product.getName());
        name.getStyleClass().add("product-name");

        // ---- Price ----
        Label price = new Label(PriceFmt.inr(product.getPrice()));
        price.getStyleClass().add("product-price");

        getChildren().addAll(img, name, price);

        // ---- Click handler ----
        setOnMouseClicked(e -> {
            if (onOpen != null) {
                // Use custom handler if provided (e.g., Dashboard)
                onOpen.accept(product);
            } else {
                // Default behavior: show details popup
                showProductDetails(product);
            }
        });
    }

    public Product getProduct() {
        return product;
    }

    /** Default product details popup (used if no custom onOpen handler provided). */
    private void showProductDetails(Product product) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Product Details");
        alert.setHeaderText(product.getName());

        String content = String.format("""
                Type: %s
                Karat: %.1f
                Weight: %.1f
                Price: ₹%.2f
                
                %s
                """,
                product.getType(),
                product.getGoldWeight(),
                product.getDiamondWeight(),
                product.getPrice(),
                product.getDescription() == null || product.getDescription().isEmpty()
                        ? "No description available."
                        : product.getDescription()
        );

        alert.setContentText(content);

        // Optional "Place Order" button for demo
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(product.getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("✅ Order placed for: " + product.getName());
            // You can integrate order insert here if you want DB linkage
        }
    }
}

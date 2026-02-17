package com.kanchancast.ui;

import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import com.jewelleryapp.dao.OrderDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Customer-facing Product Details dialog.
 * Exactly 2 buttons:
 * - Close
 * - Buy
 */
public class ProductDetailsDialog extends Dialog<Boolean> {

    private final Product product;
    private final OrderDAO orderDAO;
    private final User user;
    private final Runnable onOrderPlaced;

    public ProductDetailsDialog(Stage owner, Product product, OrderDAO orderDAO, User user, Runnable onOrderPlaced) {
        this.product = product;
        this.orderDAO = orderDAO;
        this.user = user;
        this.onOrderPlaced = onOrderPlaced;

        // Match your screenshot / older behaviour (title shows product name)
        setTitle(product != null && product.getName() != null && !product.getName().isBlank()
                ? product.getName().trim()
                : "Product Details");

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);

        loadUI();
    }

    private void loadUI() {
        try {
            URL fxml = ProductDetailsDialog.class.getResource("/com/kanchancast/ui/ProductDetailsDialog.fxml");
            if (fxml == null)
                throw new IOException("Missing FXML: /com/kanchancast/ui/ProductDetailsDialog.fxml");

            FXMLLoader loader = new FXMLLoader(fxml);
            DialogPane pane = loader.load();
            setDialogPane(pane);

            // Apply stylesheet so buttons match your blue theme
            var css = UIKit.class.getResource("/com/kanchancast/ui/styles.css");
            if (css != null && !pane.getStylesheets().contains(css.toExternalForm())) {
                pane.getStylesheets().add(css.toExternalForm());
            }

            // Populate UI with product data
            ProductDetailsDialogController controller = loader.getController();
            if (controller != null)
                controller.setProduct(product);

            // Force exactly two buttons (avoid weird duplicates)
            pane.getButtonTypes().removeIf(bt -> {
                if (bt == null || bt.getText() == null)
                    return false;
                String t = bt.getText().trim().toLowerCase();
                return t.equals("close") || t.equals("cancel") || t.equals("buy");
            });

            ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType buyType = new ButtonType("Buy", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().addAll(closeType, buyType);

            Button closeBtn = (Button) pane.lookupButton(closeType);
            Button buyBtn = (Button) pane.lookupButton(buyType);

            // Make both buttons blue
            if (closeBtn != null)
                closeBtn.getStyleClass().add("primary");

            if (buyBtn != null) {
                buyBtn.getStyleClass().add("primary");
                buyBtn.setDefaultButton(true);

                buyBtn.addEventFilter(ActionEvent.ACTION, ev -> {
                    if (product == null || user == null) {
                        UIKit.toastWarn("Cannot place order", "Missing user/product.");
                        ev.consume();
                        return;
                    }

                    boolean ok = orderDAO.createOrder(user.getUserId(), product.getProductId(), "PENDING");
                    if (!ok) {
                        UIKit.toastWarn("Order Failed", "Could not place the order. Please try again.");
                        ev.consume();
                        return;
                    }

                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Order Placed");
                    a.setHeaderText(null);
                    a.setContentText("Your order has been placed successfully.");
                    a.initOwner(getDialogPane().getScene().getWindow());
                    a.showAndWait();

                    if (onOrderPlaced != null) {
                        try {
                            onOrderPlaced.run();
                        } catch (Exception ignored) {
                        }
                    }

                    setResult(Boolean.TRUE);
                });
            }

            setResultConverter(bt -> bt != null && bt.getButtonData() == ButtonBar.ButtonData.OK_DONE);

        } catch (IOException e) {
            UIKit.toastWarn("Error", "Could not open product details: " + e.getMessage());
            setResult(Boolean.FALSE);
        }
    }
}

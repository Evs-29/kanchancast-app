package com.kanchancast.ui;

import com.jewelleryapp.dao.OrderDAO;
import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Window;

/**
 * ProductDetailsDialog
 * --------------------
 * Displays detailed info for a product and allows the customer
 * to place an order (Buy button).
 */
public class ProductDetailsDialog extends Dialog<Void> {

    public ProductDetailsDialog(Window owner, Product product, OrderDAO orderDAO, User customer, Runnable onOrderPlaced) {
        super();

        try {
            // ✅ Load FXML correctly (always use absolute path)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/kanchancast/ui/ProductDetailsDialog.fxml"));
            DialogPane pane = loader.load();

            // ✅ Initialize controller
            ProductDetailsDialogController controller = loader.getController();
            controller.setProduct(product);

            setDialogPane(pane);
            setTitle(product.getName());
            if (owner != null) initOwner(owner);

            // ✅ Ensure dialog has the right buttons
            ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType buyType = new ButtonType("Buy", ButtonBar.ButtonData.OK_DONE);

            pane.getButtonTypes().setAll(closeType, buyType);

            // Use Platform.runLater to safely bind button actions
            Platform.runLater(() -> {
                Button buyButton = (Button) pane.lookupButton(buyType);
                Button closeButton = (Button) pane.lookupButton(closeType);

                // 🔍 Debug logs to verify everything works
                System.out.println("🟢 [ProductDetailsDialog] Loaded for product: " + product.getName());
                System.out.println("   ↳ userId=" + customer.getUserId() + ", productId=" + product.getProductId());
                System.out.println("   ↳ orderDAO=" + (orderDAO != null ? "OK" : "NULL"));

                // ✅ Buy button action
                if (buyButton != null) {
                    buyButton.setOnAction(e -> {
                        System.out.println("🟡 [Buy] Button clicked for productId=" + product.getProductId());

                        if (orderDAO == null) {
                            System.err.println("❌ OrderDAO is null — cannot create order!");
                            Toast.show(owner, "⚠️ Internal error: OrderDAO not set");
                            return;
                        }

                        boolean success = orderDAO.createOrder(customer.getUserId(), product.getProductId(), "Processing");

                        if (success) {
                            System.out.println("✅ [ProductDetailsDialog] Order created successfully!");
                            Toast.show(owner, "✅ Order placed successfully for " + product.getName());
                            if (onOrderPlaced != null) onOrderPlaced.run();
                        } else {
                            System.err.println("❌ [ProductDetailsDialog] Failed to create order!");
                            Toast.show(owner, "⚠️ Failed to place order. Try again.");
                        }
                        close();
                    });
                } else {
                    System.err.println("❌ Buy button not found in dialog!");
                }

                // ✅ Close button action
                if (closeButton != null) {
                    closeButton.setOnAction(e -> close());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(owner, "⚠️ Error opening product details: " + e.getMessage());
        }
    }
}

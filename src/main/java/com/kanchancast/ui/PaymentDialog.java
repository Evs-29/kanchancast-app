package com.kanchancast.ui;

import com.kanchancast.model.Product;
import com.kanchancast.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Simple payment choice dialog.
 * Returns TRUE when the user confirms, FALSE/NULL when cancelled.
 */
public class PaymentDialog extends Dialog<Boolean> {

    public PaymentDialog(Window owner, User customer, Product product) {
        setTitle("Payment");
        // ✅ set owner on the Dialog itself
        if (owner != null) {
            initOwner(owner);
        }

        // --- payment method choices ---
        ToggleGroup group = new ToggleGroup();
        RadioButton card = new RadioButton("Card");
        card.setToggleGroup(group);
        RadioButton cash = new RadioButton("Cash on delivery");
        cash.setToggleGroup(group);
        cash.setSelected(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        int r = 0;
        form.add(new Label("Paying for:"), 0, r);
        form.add(new Label(product != null ? product.getName() : "-"), 1, r++);
        form.add(new Label("Customer:"), 0, r);
        form.add(new Label(customer != null ? customer.getUserName() : "-"), 1, r++);

        VBox methods = new VBox(6, card, cash);
        form.add(new Label("Method:"), 0, r);
        form.add(methods, 1, r);

        // --- footer actions using standard dialog buttons ---
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Pay");

        // --- layout ---
        VBox content = new VBox(12,
                new Label("Choose your payment method"),
                form
        );
        content.setPadding(new Insets(14));
        content.setAlignment(Pos.TOP_LEFT);

        getDialogPane().setContent(content);

        // Return TRUE if user clicks OK, otherwise FALSE
        setResultConverter(bt -> (bt == ButtonType.OK) ? Boolean.TRUE : Boolean.FALSE);
    }
}

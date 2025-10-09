package com.kanchancast.ui;

import com.kanchancast.model.Product;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProductGrid extends ScrollPane {
    private final FlowPane flow = new FlowPane();
    private final Consumer<Product> onOpen;

    public ProductGrid(Consumer<Product> onOpen) {
        this.onOpen = (onOpen != null) ? onOpen : p -> {};
        flow.setHgap(16); flow.setVgap(16);
        flow.setPadding(new Insets(12));
        flow.setPrefWrapLength(680);

        setFitToWidth(true);
        setContent(flow);
        getStyleClass().add("card");
        setPadding(new Insets(8));
        setHbarPolicy(ScrollBarPolicy.NEVER);
    }

    public void setItems(List<Product> products) {
        if (products == null) products = new ArrayList<>();
        flow.getChildren().clear();
        for (Product p : products) flow.getChildren().add(new ProductCard(p, onOpen));
    }
}

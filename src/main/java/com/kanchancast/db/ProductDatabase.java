package com.kanchancast.db;

import com.jewelleryapp.dao.ProductDAO;
import com.kanchancast.model.Product;

import java.util.List;

/**
 * Thin facade around ProductDAO to keep older calls working.
 */
public class ProductDatabase {
    private static final ProductDAO dao = new ProductDAO();

    /** Legacy signature used elsewhere in your app. */
    public static boolean addProduct(Product p) {
        return dao.addProduct(p); // delegates to the overload in ProductDAO
    }

    public static List<Product> getAllProducts() {
        return dao.listAll();
    }

    public static List<Product> getProductsByType(String type) {
        return dao.listByType(type);
    }
}

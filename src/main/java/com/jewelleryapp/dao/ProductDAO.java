package com.jewelleryapp.dao;

import com.kanchancast.model.Product;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Product Data Access Object (DAO)
 * ---------------------------------
 * Handles all product-related database operations:
 *  - Create / Insert new products
 *  - Delete existing products
 *  - Retrieve all products or filter by type
 *
 * Updated (Oct 2025):
 *  Matches the current SQLite schema with columns:
 *  product_id | name | type | karat | weight | price | image_path | description
 */
public class ProductDAO {

    // ---------------- SQL ----------------
    private static final String SQL_SELECT_ALL = """
        SELECT
            product_id,
            name,
            type,
            karat,
            weight,
            price,
            image_path,
            description
        FROM products
        ORDER BY product_id DESC
        """;

    private static final String SQL_SELECT_BY_TYPE = """
        SELECT
            product_id,
            name,
            type,
            karat,
            weight,
            price,
            image_path,
            description
        FROM products
        WHERE LOWER(type) = LOWER(?)
        ORDER BY product_id DESC
        """;

    private static final String SQL_INSERT = """
        INSERT INTO products
            (name, type, karat, weight, price, image_path, description)
        VALUES (?,?,?,?,?,?,?)
        """;

    private static final String SQL_DELETE = """
        DELETE FROM products WHERE product_id = ?
        """;

    // ---------------- Public API ----------------

    /** Create product entry from form fields */
    public boolean createProduct(String name,
                                 String type,
                                 double karat,
                                 double weight,
                                 double price,
                                 String imagePath,
                                 String description) {
        Product p = new Product();
        p.setName(nvl(name));
        p.setType(nvl(type));
        p.setGoldWeight(karat);      // using goldWeight for karat
        p.setDiamondWeight(weight);  // using diamondWeight for total weight
        p.setPrice(price);
        p.setImagePath(nvl(imagePath));
        p.setDescription(nvl(description));
        return addProduct(p);
    }

    /** Insert a product into DB */
    public boolean addProduct(Product p) {
        if (p == null) return false;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nvl(p.getName()));
            ps.setString(2, nvl(p.getType()));
            ps.setBigDecimal(3, BigDecimal.valueOf(p.getGoldWeight()));     // karat
            ps.setBigDecimal(4, BigDecimal.valueOf(p.getDiamondWeight()));  // weight
            ps.setBigDecimal(5, BigDecimal.valueOf(p.getPrice()));
            ps.setString(6, nvl(p.getImagePath()));
            ps.setString(7, nvl(p.getDescription()));

            int updated = ps.executeUpdate();
            if (updated == 1) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        // Optional: p.setId(keys.getInt(1));
                    }
                } catch (Throwable ignored) {}
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error adding product: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /** Delete a product by ID */
    public boolean deleteProduct(int productId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error deleting product: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** Return all products */
    public List<Product> listAll() {
        List<Product> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            System.err.println("❌ Error listing all products: " + e.getMessage());
            e.printStackTrace();
        }
        return out;
    }

    /** Compatibility alias for legacy calls */
    public List<Product> listALL() {
        return listAll();
    }

    /** Return products filtered by type */
    public List<Product> listByType(String type) {
        List<Product> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_SELECT_BY_TYPE)) {
            ps.setString(1, nvl(type));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error listing products by type: " + e.getMessage());
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Combined method — works with category filter dropdown.
     * If filter == "all" → show all products,
     * else → filter by type.
     */
    public List<Product> listAll(String category) {
        if (category == null || category.equalsIgnoreCase("all")) {
            return listAll();
        }
        return listByType(category);
    }

    // ---------------- Helpers ----------------

    private static String nvl(String s) {
        return (s == null) ? "" : s;
    }

    /** Map a row from ResultSet to Product model. */
    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        try { p.setProductId(rs.getInt("product_id")); } catch (Throwable ignored) {}
        p.setName(rs.getString("name"));
        p.setType(rs.getString("type"));
        p.setGoldWeight(rs.getDouble("karat"));
        p.setDiamondWeight(rs.getDouble("weight"));
        p.setPrice(rs.getDouble("price"));
        p.setImagePath(rs.getString("image_path"));
        p.setDescription(rs.getString("description"));
        return p;
    }
    public List<String> listCategories() {
        List<String> out = new ArrayList<>();
        String sql = "SELECT name FROM categories ORDER BY name";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    private void ensureCategoriesTable() {
        final String sql = """
        CREATE TABLE IF NOT EXISTS product_categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL
        )
        """;
        try (java.sql.Connection c = DatabaseConnection.getConnection();
             java.sql.Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (java.sql.SQLException e) {
            System.err.println("⚠️ ensureCategoriesTable: " + e.getMessage());
        }
    }
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT type FROM products ORDER BY type";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) categories.add(rs.getString("type"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }
    // ---------------- CATEGORY MANAGEMENT ----------------

    /** List all unique product categories (types) */
    public java.util.List<String> listAllCategories() {
        ensureCategoriesTable();
        java.util.List<String> out = new java.util.ArrayList<>();
        final String sql = "SELECT name FROM product_categories ORDER BY name ASC";
        try (java.sql.Connection c = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString("name"));
        } catch (java.sql.SQLException e) {
            System.err.println("⚠️ listAllCategories: " + e.getMessage());
        }
        return out;
    }

    /** Add a new product category (if it doesn’t already exist) */
    public boolean addCategory(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ensureCategoriesTable();
        final String sql = "INSERT OR IGNORE INTO product_categories(name) VALUES (?)";
        try (java.sql.Connection c = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("⚠️ addCategory: " + e.getMessage());
            return false;
        }
    }

    /** Delete a category (removes all products with that type) */
    public boolean deleteCategory(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ensureCategoriesTable();
        final String check = "SELECT COUNT(*) FROM products WHERE LOWER(type)=LOWER(?)";
        final String del   = "DELETE FROM product_categories WHERE LOWER(name)=LOWER(?)";
        try (java.sql.Connection c = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps1 = c.prepareStatement(check)) {
            ps1.setString(1, name.trim());
            try (java.sql.ResultSet rs = ps1.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // products still reference this category → do not delete
                    return false;
                }
            }
            try (java.sql.PreparedStatement ps2 = c.prepareStatement(del)) {
                ps2.setString(1, name.trim());
                return ps2.executeUpdate() > 0;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("⚠️ deleteCategory: " + e.getMessage());
            return false;
        }
    }
    public void ensureCategoryExists(String name) {
        if (name == null || name.trim().isEmpty()) return;
        addCategory(name.trim());
    }
}

package com.jewelleryapp.dao;

import com.kanchancast.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Product DAO
 * ✅ Supports: stone_weight, duration_amount, duration_unit
 */
public class ProductDAO {

    private static final String SQL_SELECT_ALL = """
        SELECT
            product_id,
            name,
            type,
            karat,
            weight,
            COALESCE(stone_weight, 0) AS stone_weight,
            price,
            image_path,
            description,
            COALESCE(duration_amount, 0) AS duration_amount,
            COALESCE(duration_unit, 'DAYS') AS duration_unit
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
            COALESCE(stone_weight, 0) AS stone_weight,
            price,
            image_path,
            description,
            COALESCE(duration_amount, 0) AS duration_amount,
            COALESCE(duration_unit, 'DAYS') AS duration_unit
        FROM products
        WHERE LOWER(type) = LOWER(?)
        ORDER BY product_id DESC
        """;

    private static final String SQL_INSERT = """
        INSERT INTO products
            (name, type, karat, weight, stone_weight, price, image_path, description, duration_amount, duration_unit)
        VALUES (?,?,?,?,?,?,?,?,?,?)
        """;

    // ✅ Updated signature: includes stoneWeight
    public boolean createProduct(String name,
                                 String type,
                                 double karat,
                                 double diamondWeight,
                                 double stoneWeight,
                                 double price,
                                 String imagePath,
                                 String description,
                                 int durationAmount,
                                 String durationUnit) {

        Product p = new Product();
        p.setName(nvl(name));
        p.setType(nvl(type));
        p.setGoldWeight(karat);
        p.setDiamondWeight(diamondWeight);
        p.setStoneWeight(stoneWeight);
        p.setPrice(price);
        p.setImagePath(nvl(imagePath));
        p.setDescription(nvl(description));

        p.setDurationAmount(Math.max(0, durationAmount));
        p.setDurationUnit(normalizeUnit(durationUnit));

        return addProduct(p);
    }

    public boolean addProduct(Product p) {
        if (p == null) return false;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nvl(p.getName()));
            ps.setString(2, nvl(p.getType()));
            ps.setBigDecimal(3, BigDecimal.valueOf(nvlNum(p.getGoldWeight())));
            ps.setBigDecimal(4, BigDecimal.valueOf(nvlNum(p.getDiamondWeight())));
            ps.setBigDecimal(5, BigDecimal.valueOf(nvlNum(p.getStoneWeight())));
            ps.setBigDecimal(6, BigDecimal.valueOf(p.getPrice()));
            ps.setString(7, nvl(p.getImagePath()));
            ps.setString(8, nvl(p.getDescription()));
            ps.setInt(9, Math.max(0, p.getDurationAmount()));
            ps.setString(10, normalizeUnit(p.getDurationUnit()));

            int updated = ps.executeUpdate();
            return updated == 1;

        } catch (SQLException e) {
            System.err.println("❌ Error adding product: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /** Delete a product by ID (also deletes any linked orders + their stage rows) */
    public boolean deleteProduct(int productId) {

        String delStagesForProductOrders = """
            DELETE FROM order_stages
            WHERE order_id IN (SELECT order_id FROM orders WHERE product_id = ?)
        """;

        String delOrdersForProduct = "DELETE FROM orders WHERE product_id = ?";
        String delProduct = "DELETE FROM products WHERE product_id = ?";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps1 = c.prepareStatement(delStagesForProductOrders);
                 PreparedStatement ps2 = c.prepareStatement(delOrdersForProduct);
                 PreparedStatement ps3 = c.prepareStatement(delProduct)) {

                ps1.setInt(1, productId);
                ps1.executeUpdate();

                ps2.setInt(1, productId);
                ps2.executeUpdate();

                ps3.setInt(1, productId);
                int rows = ps3.executeUpdate();

                c.commit();
                return rows > 0;

            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error deleting product: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

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

    public List<Product> listALL() { return listAll(); }

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

    public List<Product> listAll(String category) {
        if (category == null || category.equalsIgnoreCase("all")) {
            return listAll();
        }
        return listByType(category);
    }

    // ---------------- Helpers ----------------
    private static String nvl(String s) { return (s == null) ? "" : s; }
    private static double nvlNum(Double d) { return (d == null) ? 0.0 : d; }

    private static String normalizeUnit(String unit) {
        if (unit == null) return "DAYS";
        String u = unit.trim().toUpperCase();
        if (u.equals("DAY") || u.equals("DAYS")) return "DAYS";
        if (u.equals("WEEK") || u.equals("WEEKS")) return "WEEKS";
        if (u.equals("MONTH") || u.equals("MONTHS")) return "MONTHS";
        return "DAYS";
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setName(rs.getString("name"));
        p.setType(rs.getString("type"));
        p.setGoldWeight(rs.getDouble("karat"));
        p.setDiamondWeight(rs.getDouble("weight"));
        p.setStoneWeight(rs.getDouble("stone_weight"));
        p.setPrice(rs.getDouble("price"));
        p.setImagePath(rs.getString("image_path"));
        p.setDescription(rs.getString("description"));
        p.setDurationAmount(rs.getInt("duration_amount"));
        p.setDurationUnit(rs.getString("duration_unit"));
        return p;
    }

    // ---- category code below: KEEP EXACTLY AS YOUR CURRENT FILE ----
    // (I’m keeping it unchanged to avoid breaking category management.)

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

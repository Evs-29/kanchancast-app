package com.kanchancast.dev;

import com.jewelleryapp.dao.DatabaseConnection;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeedUsersAndProducts {

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static String genUserCode() {
        StringBuilder sb = new StringBuilder("KC-");
        for (int i = 0; i < 12; i++) sb.append(BASE62[RNG.nextInt(BASE62.length)]);
        return sb.toString();
    }

    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);

            // --- seed users (plaintext passwords for simplicity) ---
            List<UserSeed> users = List.of(
                    // user_type,  username,    password,       address,       gender,  work_area
                    new UserSeed("owner",      "owner1",       "Owner1234",    "HQ",           "Other", null),
                    new UserSeed("admin",      "admin1",       "Admin1234",    "HQ",           "Other", null),
                    new UserSeed("employee",   "emp_raw",      "Emp12345",     "Workshop A",   "M",     "Raw Material Procurement and Management"),
                    new UserSeed("employee",   "emp_cad",      "Emp12345",     "Design Lab",   "F",     "Design & CAD Modelling"),
                    new UserSeed("customer",   "cust1",        "Cust12345",    "Customer Addr","F",     null)
            );

            for (UserSeed u : users) {
                String code = genUserCode();
                // ensure uniqueness (very unlikely to collide, but just in case)
                while (userCodeExists(conn, code)) code = genUserCode();

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (user_type, user_code, user_name, password, address, gender, work_area) " +
                                "VALUES (?,?,?,?,?,?,?)")) {
                    ps.setString(1, u.type);
                    ps.setString(2, code);
                    ps.setString(3, u.username);
                    ps.setString(4, u.password); // PLAINTEXT on purpose
                    ps.setString(5, u.address);
                    ps.setString(6, u.gender);
                    if (u.workArea == null || u.workArea.isBlank()) ps.setNull(7, Types.VARCHAR);
                    else ps.setString(7, u.workArea);
                    ps.executeUpdate();
                }

                System.out.printf("Seeded %-8s  user_code=%s  username=%s  password=%s  %s%n",
                        u.type, code, u.username, u.password,
                        (u.workArea != null ? ("work_area=" + u.workArea) : ""));
            }

            // --- seed a few products ---
            List<ProductSeed> products = new ArrayList<>();
            products.add(new ProductSeed("Elegant Gold Necklace", "necklaces", 22.0, 15.5, 79999, null));
            products.add(new ProductSeed("Classic Diamond Ring",  "rings",     18.0, 4.2,  120000, null));
            products.add(new ProductSeed("Pearl Earrings",        "earrings",  18.0, 3.1,  45000,  null));
            products.add(new ProductSeed("Charm Bracelet",        "bracelets", 14.0, 8.0,  38000,  null));

            for (ProductSeed p : products) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO products (name, type, karat, weight, price, image_path) VALUES (?,?,?,?,?,?)")) {
                    ps.setString(1, p.name);
                    ps.setString(2, p.type);
                    ps.setDouble(3, p.karat);
                    ps.setDouble(4, p.weight);
                    ps.setDouble(5, p.price);
                    if (p.imagePath == null) ps.setNull(6, Types.VARCHAR);
                    else ps.setString(6, p.imagePath);
                    ps.executeUpdate();
                }
            }
            System.out.println("Seeded products: " + products.size());

            conn.commit();
            System.out.println("✅ Seeding complete. You can now log in with the above creds.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean userCodeExists(Connection conn, String code) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE user_code = ? LIMIT 1")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            return false;
        }
    }

    private record UserSeed(String type, String username, String password, String address, String gender, String workArea) {}
    private record ProductSeed(String name, String type, double karat, double weight, double price, String imagePath) {}
}

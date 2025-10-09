package com.kanchancast.dev;

import com.jewelleryapp.dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

public class CreateAdmin {
    public static void main(String[] args) {
        String username = "admin2";      // Change if needed
        String password = "AdminPass123";  // Change if needed
        String address = "HQ";
        String gender = "Other";

        // Generate a unique user_code
        String userCode = "U-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String sql = "INSERT INTO users (user_type, user_code, user_name, password, address, gender) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "ADMIN");
            pstmt.setString(2, userCode);
            pstmt.setString(3, username);
            pstmt.setString(4, password); // Plain password for quick testing
            pstmt.setString(5, address);
            pstmt.setString(6, gender);

            pstmt.executeUpdate();
            System.out.println("✅ Admin created successfully!");
            System.out.println("User Code: " + userCode);
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

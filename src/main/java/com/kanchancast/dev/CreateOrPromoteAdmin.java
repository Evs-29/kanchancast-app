package com.kanchancast.dev;

import com.jewelleryapp.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Idempotent admin setup:
 * - If user_name exists: promote to admin, reset password, ensure user_code present
 * - Else: insert a new admin
 * Uses PLAINTEXT password for speed while testing.
 */
public class CreateOrPromoteAdmin {

    public static void main(String[] args) {
        String username = "newadmin";       // <-- change if you like
        String password = "AdminPass123";   // <-- change if you like (plaintext for testing)
        String address = "HQ";
        String gender  = "Other";

        try (Connection conn = DatabaseConnection.connect()) {
            // 1) Does this username already exist?
            Integer existingUserId = null;
            String existingCode = null;
            try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT user_id, user_code FROM users WHERE user_name = ? LIMIT 1")) {
                chk.setString(1, username);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) {
                        existingUserId = rs.getInt("user_id");
                        existingCode = rs.getString("user_code");
                    }
                }
            }

            // 2) Generate a code if needed
            String userCode = (existingCode != null && !existingCode.isBlank())
                    ? existingCode
                    : ("KC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());

            if (existingUserId != null) {
                // 3A) Update existing user: make admin, set password, ensure user_code present
                try (PreparedStatement up = conn.prepareStatement(
                        "UPDATE users SET user_type = ?, password = ?, address = ?, gender = ?, user_code = ? WHERE user_id = ?")) {
                    up.setString(1, "admin");           // keep lowercase to match rest of app
                    up.setString(2, password);          // plaintext for now
                    up.setString(3, address);
                    up.setString(4, gender);
                    up.setString(5, userCode);
                    up.setInt(6, existingUserId);
                    up.executeUpdate();
                }
                System.out.println("✅ Existing user promoted to admin and updated:");
                System.out.println("User ID  : " + existingUserId);
                System.out.println("User Code: " + userCode);
                System.out.println("Username : " + username);
                System.out.println("Password : " + password);
            } else {
                // 3B) Insert new admin
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO users (user_type, user_code, user_name, password, address, gender) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ins.setString(1, "admin");         // keep lowercase
                    ins.setString(2, userCode);
                    ins.setString(3, username);
                    ins.setString(4, password);        // plaintext for now
                    ins.setString(5, address);
                    ins.setString(6, gender);
                    ins.executeUpdate();
                }
                System.out.println("✅ Admin created successfully!");
                System.out.println("User Code: " + userCode);
                System.out.println("Username : " + username);
                System.out.println("Password : " + password);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

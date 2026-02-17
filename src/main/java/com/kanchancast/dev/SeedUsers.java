package com.kanchancast.dev;

import com.jewelleryapp.dao.DatabaseConnection;
import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.auth.PasswordUtil;
import com.kanchancast.model.StaffRow;
import com.kanchancast.model.User;

import java.sql.*;
import java.util.*;

/**
 * UserDAO – Handles authentication, creation, and listing of users.
 *
 * ✅ Teacher change supported: authenticateByCode(user_code + password)
 *
 * NOTE: This version does NOT use DOB because your current User model does not have setDob().
 */
public class SeedUsers {

    // ==============================
    // 🔐 AUTHENTICATION
    // ==============================

    /** Authenticate user by username + password (supports hashed or plaintext). */
    public Optional<User> authenticate(String username, String rawPassword) {
        final String SQL = "SELECT * FROM users WHERE user_name = ? LIMIT 1";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                boolean matches = storedHash != null && storedHash.equals(rawPassword);

                if (!matches) {
                    try { matches = PasswordUtil.verifyPassword(rawPassword, storedHash); }
                    catch (Exception ignored) {}
                }

                if (matches) return Optional.of(mapUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ authenticate error: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /** Authenticate using code + username + password (kept for backward compatibility). */
    public Optional<User> authenticateFull(String userCode, String userName, String rawPassword) {
        final String SQL = "SELECT * FROM users WHERE user_code = ? AND user_name = ? LIMIT 1";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setString(1, userCode.trim());
            ps.setString(2, userName.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                boolean matches = storedHash != null && storedHash.equals(rawPassword);

                if (!matches) {
                    try { matches = PasswordUtil.verifyPassword(rawPassword, storedHash); }
                    catch (Exception ignored) {}
                }

                if (matches) return Optional.of(mapUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ authenticateFull error: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * ✅ Teacher change: Authenticate using ONLY user_code + password.
     * Username is still loaded from DB so it displays everywhere else unchanged.
     */
    public Optional<User> authenticateByCode(String userCode, String rawPassword) {
        final String SQL = "SELECT * FROM users WHERE user_code = ? LIMIT 1";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setString(1, userCode == null ? "" : userCode.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                boolean matches = storedHash != null && storedHash.equals(rawPassword);

                if (!matches) {
                    try { matches = PasswordUtil.verifyPassword(rawPassword, storedHash); }
                    catch (Exception ignored) {}
                }

                if (matches) return Optional.of(mapUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ authenticateByCode error: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // ==============================
    // 👥 USER CREATION
    // ==============================

    /**
     * Creates a user record.
     * Stores a hashed password if PasswordUtil hashing is available; otherwise stores raw.
     */
    public boolean createUser(String type, String username, String rawPassword,
                              String address, String gender, String workArea) {

        String sql = "INSERT INTO users (user_type, user_name, password, address, gender, work_area) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String hashed = tryHash(rawPassword);

            ps.setString(1, type);
            ps.setString(2, username);
            ps.setString(3, hashed);
            ps.setString(4, address);
            ps.setString(5, gender);
            ps.setString(6, workArea);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ createUser error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean createAdmin(String username, String rawPassword, String address) {
        return createUser("admin", username, rawPassword, address, "", "");
    }

    // ==============================
    // ✅ METHODS REQUIRED BY SeedUsers / StaffFormDialog (restore compatibility)
    // ==============================

    /**
     * Used by SeedUsers to create admin/owner/staff and return the created User object.
     * DO NOT remove — other files depend on this name/signature.
     */
    public Optional<User> createUserInternalReturn(String username, String password, String role,
                                                   String address, String gender, String workArea) {
        boolean ok = createUser(role, username, password, address, gender, workArea);
        if (ok) return findByUsername(username);
        return Optional.empty();
    }

    /**
     * Legacy method used by some UI dialogs / seed scripts.
     * Keeps older code working without changes.
     */
    public Optional<User> createStaffReturn(String username, String password, String role,
                                            String address, String gender, String workArea) {
        boolean ok = createUser(role, username, password, address, gender, workArea);
        if (ok) return findByUsername(username);
        return Optional.empty();
    }

    // ==============================
    // 🔍 LOOKUPS & LISTS
    // ==============================

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE user_name = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapUser(rs));

        } catch (SQLException e) {
            System.err.println("❌ findByUsername error: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<User> listAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, user_code, user_name, user_type, gender, address, work_area, age FROM users ORDER BY user_id DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapUser(rs));

        } catch (SQLException e) {
            System.err.println("❌ listAll error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<User> listEmployeesOnly() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE LOWER(user_type) = 'employee' ORDER BY user_id DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapUser(rs));

        } catch (SQLException e) {
            System.err.println("❌ listEmployeesOnly error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ deleteUser error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==============================
    // 📊 STATS
    // ==============================

    public List<StaffRow> listEmployeeStats() {
        List<StaffRow> out = new ArrayList<>();
        String SQL = """
            SELECT
                U.user_id,
                U.user_name,
                COALESCE(U.work_area, '') AS area,
                COALESCE(U.age, 0) AS age,
                COALESCE(SUM(
                    CASE WHEN LOWER(COALESCE(S.completed,'no')) = 'yes' THEN 1 ELSE 0 END), 0) AS orders_done
            FROM users U
            LEFT JOIN order_stages S ON S.employee_id = U.user_id
            WHERE LOWER(COALESCE(U.user_type, '')) = 'employee'
            GROUP BY U.user_id, U.user_name, area, age
            ORDER BY U.user_id DESC
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                StaffRow s = new StaffRow();
                s.setUserId(rs.getInt("user_id"));
                s.setUserName(rs.getString("user_name"));
                s.setWorkArea(rs.getString("area"));
                s.setAge(rs.getInt("age"));
                s.setOrdersDone(rs.getInt("orders_done"));
                out.add(s);
            }

        } catch (SQLException e) {
            System.err.println("⚠️ Error fetching employee stats: " + e.getMessage());
            e.printStackTrace();
        }
        return out;
    }

    // ==============================
    // ⚙️ HELPERS
    // ==============================

    private static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUserCode(rs.getString("user_code"));
        u.setUserName(rs.getString("user_name"));
        u.setUserType(rs.getString("user_type"));
        u.setAddress(rs.getString("address"));

        try { u.setArea(rs.getString("work_area")); } catch (SQLException ignored) {}
        try { u.setGender(rs.getString("gender")); } catch (SQLException ignored) {}
        try { u.setAge(rs.getInt("age")); } catch (SQLException ignored) {}

        // ❌ No DOB here (User model doesn't support it)
        return u;
    }

    private static String tryHash(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        try { return PasswordUtil.hashPassword(raw); }
        catch (Throwable e) { return raw; }
    }

    // ======================================================
    // 🧩 Signup support methods
    // ======================================================

    public String generateUserCode(String prefix) {
        int random = (int) (Math.random() * 900000) + 100000; // 6 digits
        return prefix + random;
    }

    public boolean insertUser(com.kanchancast.model.User user) {
        String sql = "INSERT INTO users (user_type, user_name, password, address, gender, user_code) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, user.getUserType());
            ps.setString(2, user.getUserName());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getAddress());
            ps.setString(5, user.getGender());
            ps.setString(6, user.getUserCode());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ insertUser: " + e.getMessage());
            return false;
        }
    }
}

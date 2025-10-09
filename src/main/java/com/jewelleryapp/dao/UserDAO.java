package com.jewelleryapp.dao;

import com.kanchancast.model.User;
import com.kanchancast.model.StaffRow;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * UserDAO – authentication + CRUD helpers used across the app.
 *
 * Works with plaintext OR hashed passwords:
 *  - If a class com.kanchancast.auth.PasswordUtil exists, we will try (by reflection)
 *    common methods like matches/verify/check and hash/encode/hashPassword, etc.
 *  - If none are found, we fall back to plaintext comparison/storage.
 */
public class UserDAO {

    // ------------------------------------------------------------
    // Auth
    // ------------------------------------------------------------

    /** Username + password auth (accepts plaintext or hashed rows via reflection). */
    public Optional<User> authenticate(String username, String password) {
        final String SQL = "SELECT * FROM users WHERE user_name = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    if (passwordMatches(plain(password), stored)) {
                        return Optional.of(mapUser(rs));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    /** Auth by user_code + user_name + password (plaintext or hashed). */
    public Optional<User> authenticateByCodeAndUsername(String code, String username, String password) {
        final String SQL = "SELECT * FROM users WHERE user_code = ? AND user_name = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setString(1, code);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    if (passwordMatches(plain(password), stored)) {
                        return Optional.of(mapUser(rs));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    /** Compare plaintext to stored value; if not equal, try PasswordUtil (reflection). */
    private boolean passwordMatches(String plain, String stored) {
        if (stored == null) return false;
        if (stored.equals(plain)) return true; // legacy plaintext rows
        // Try reflection against com.kanchancast.auth.PasswordUtil
        try {
            Class<?> util = Class.forName("com.kanchancast.auth.PasswordUtil");
            // Try common verification method names (static boolean method(String plain, String hash))
            String[] names = {"matches", "verify", "verifyPassword", "check", "checkPassword", "compare", "comparePassword"};
            for (String n : names) {
                Method m = findStatic(util, n, String.class, String.class);
                if (m != null) {
                    Object ok = m.invoke(null, plain, stored);
                    if (ok instanceof Boolean && (Boolean) ok) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // ------------------------------------------------------------
    // Create
    // ------------------------------------------------------------

    /** Overload without explicit code prefix – derives from role. */
    public Optional<User> createUserInternalReturn(String username, String password, String role,
                                                   String address, String gender, String workArea) {
        return createUserInternalReturn(username, password, role, address, gender, workArea, prefixForRole(role));
    }

    /** Create user; stores hashed password if PasswordUtil hashing is available. */
    public Optional<User> createUserInternalReturn(String username, String password, String role,
                                                   String address, String gender, String workArea,
                                                   String userCodePrefix) {
        final String code = generateCode(userCodePrefix);
        final String toStore = tryHash(plain(password));

        final String SQL = "INSERT INTO users(user_code, user_name, password, user_type, address, gender, work_area) " +
                "VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, username);
            ps.setString(3, toStore);
            ps.setString(4, role);
            ps.setString(5, address);
            ps.setString(6, gender);
            ps.setString(7, workArea);

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        User u = new User();
                        u.setUserId(id);
                        u.setUserCode(code);
                        u.setUserName(username);
                        u.setUserType(role);
                        u.setAddress(address);
                        u.setArea(workArea);
                        return Optional.of(u);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<User> createCustomerReturn(String username, String password,
                                               String address, String gender) {
        return createUserInternalReturn(username, password, "customer", address, gender, null, "C");
    }

    public Optional<User> createStaffReturn(String username, String password, String role,
                                            String address, String gender, String workArea) {
        return createUserInternalReturn(username, password, role, address, gender, workArea, prefixForRole(role));
    }

    // ------------------------------------------------------------
    // Read / list
    // ------------------------------------------------------------

    public Optional<User> findByUsername(String username) {
        final String SQL = "SELECT * FROM users WHERE user_name = ? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    /** Legacy name kept: listALL() */
    public List<User> listALL() {
        final String SQL = "SELECT * FROM users ORDER BY user_id DESC";
        List<User> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public List<User> listEmployeesOnly() {
        final String SQL = "SELECT * FROM users WHERE LOWER(user_type) = 'employee' ORDER BY user_id DESC";
        List<User> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static String prefixForRole(String role) {
        if (role == null) return "U";
        String r = role.toLowerCase();
        if (r.equals("customer")) return "C";
        if (r.equals("admin"))    return "A";
        if (r.equals("owner"))    return "O";
        return "E"; // employees/default
    }

    private static String generateCode(String prefix) {
        if (prefix == null || prefix.isBlank()) prefix = "U";
        int r = 100_000 + new Random().nextInt(900_000);
        return prefix.toUpperCase() + r;
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUserCode(rs.getString("user_code"));
        u.setUserName(rs.getString("user_name"));
        u.setUserType(rs.getString("user_type"));
        u.setAddress(rs.getString("address"));
        try { u.setArea(rs.getString("work_area")); } catch (SQLException ignored) {}
        return u;
    }

    // ---- Password helpers (no compile-time dependency on PasswordUtil) ----

    private static String plain(String s) { return s == null ? "" : s; }

    /** Try to hash via PasswordUtil.<method>(String), else return plaintext. */
    private static String tryHash(String plain) {
        try {
            Class<?> util = Class.forName("com.kanchancast.auth.PasswordUtil");
            String[] names = {"hash", "encode", "hashPassword", "encrypt", "bcrypt"};
            for (String n : names) {
                Method m = findStatic(util, n, String.class);
                if (m != null) {
                    Object out = m.invoke(null, plain);
                    if (out instanceof String) return (String) out;
                }
            }
        } catch (Throwable ignored) {}
        return plain; // fallback: store plaintext (so users can still log in)
    }

    /** Find a public static method by name/signature; returns null if not found. */
    private static Method findStatic(Class<?> clazz, String name, Class<?>... params) {
        try {
            Method m = clazz.getMethod(name, params);
            if ((m.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) {
                return m;
            }
        } catch (NoSuchMethodException ignored) {}
        return null;
    }
    public java.util.List<com.kanchancast.model.StaffRow> listEmployeeStats() {
        java.util.List<com.kanchancast.model.StaffRow> out = new java.util.ArrayList<>();

        String SQL = """
        SELECT
            U.user_id,
            U.user_name,
            COALESCE(U.work_area, '') AS area,
            COALESCE(U.age, 0) AS age,
            COALESCE(SUM(CASE WHEN T.completed = 1 THEN 1 ELSE 0 END), 0) AS orders_done
        FROM users U
        LEFT JOIN assigned_tasks T ON T.employee_id = U.user_id
        WHERE LOWER(COALESCE(U.user_type, '')) = 'employee'
        GROUP BY U.user_id, U.user_name, area, age
        ORDER BY U.user_id DESC
        """;

        try (java.sql.Connection c = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(SQL);
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                com.kanchancast.model.StaffRow s = new com.kanchancast.model.StaffRow();
                s.setUserId(rs.getInt("user_id"));
                s.setUserName(rs.getString("user_name"));
                s.setWorkArea(rs.getString("area"));
                s.setAge(rs.getInt("age"));
                s.setOrdersDone(rs.getInt("orders_done"));
                out.add(s);
            }

        } catch (java.sql.SQLException e) {
            System.err.println("⚠️ Error fetching employee stats: " + e.getMessage());
            e.printStackTrace();
        }
        return out;
    }
    // 1️⃣ List all Admins and Employees
    public List<StaffRow> listAllAdminsAndEmployees() {
        List<StaffRow> list = new ArrayList<>();
        String sql = "SELECT user_id, user_name, user_type FROM users WHERE LOWER(user_type) IN ('admin','employee')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StaffRow s = new StaffRow();
                s.setUserId(rs.getInt("user_id"));
                s.setUserName(rs.getString("user_name"));
                s.setWorkArea(rs.getString("user_type"));
                list.add(s);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching all users: " + e.getMessage());
        }
        return list;
    }

    // 2️⃣ Delete user by ID
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ deleteUser: " + e.getMessage());
            return false;
        }
    }

    // 3️⃣ Create new Admin
    public boolean createAdmin(String username, String password, String address) {
        String sql = "INSERT INTO users (user_name, password, address, user_type) VALUES (?, ?, ?, 'admin')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, address);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ createAdmin: " + e.getMessage());
            return false;
        }
    }
    public List<User> listAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = """
        SELECT 
            user_id, 
            user_name, 
            user_type, 
            address, 
            area,
            COALESCE(gender, '') AS gender,
            COALESCE(age, 0) AS age
        FROM users
        ORDER BY user_id DESC
    """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUserName(rs.getString("user_name"));
                u.setUserType(rs.getString("user_type"));
                u.setAddress(rs.getString("address"));
                u.setArea(rs.getString("area"));
                u.setGender(rs.getString("gender"));
                u.setAge(rs.getInt("age"));
                list.add(u);
            }

        } catch (SQLException e) {
            System.err.println("❌ listAllUsers error: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    public boolean createAdmin(String username, String password, String phone, String address, String gender, int age) {
        String sql = "INSERT INTO users (user_name, password, user_type, phone, address, gender, age) VALUES (?, ?, 'admin', ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, phone);
            ps.setString(4, address);
            ps.setString(5, gender);
            ps.setInt(6, age);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ createAdmin: " + e.getMessage());
            return false;
        }
    }
    // =======================
// ADD THESE TWO METHODS
// =======================

    public List<User> listAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, user_name, user_type, gender, address, work_area, age FROM users ORDER BY user_id DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUserName(rs.getString("user_name"));
                u.setUserType(rs.getString("user_type"));
                u.setGender(rs.getString("gender"));
                u.setAddress(rs.getString("address"));
                u.setArea(rs.getString("work_area"));
                try {
                    u.setAge(rs.getInt("age"));
                } catch (Exception ignored) {
                    u.setAge(0); // fallback for older DBs without the age column
                }
                list.add(u);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error in listAll(): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }


    // Create user (for creating new Admins from OwnerDashboard)
    public boolean createUser(String type, String username, String password,
                              String address, String gender, String workArea) {
        String sql = "INSERT INTO users (user_type, user_name, password, address, gender, work_area) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, type);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, address);
            ps.setString(5, gender);
            ps.setString(6, workArea);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error in createUser(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}

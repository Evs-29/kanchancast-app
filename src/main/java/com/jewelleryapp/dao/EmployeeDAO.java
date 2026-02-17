package com.jewelleryapp.dao;

import com.kanchancast.auth.PasswordUtil;
import com.kanchancast.model.StaffRow;
import com.kanchancast.model.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDAO {

    // ---- helper: compute age in Java (used for inserts/backward compatibility) ----
    private static int computeAgeFromDobIso(String dobIso) {
        if (dobIso == null || dobIso.isBlank()) return 0;
        try {
            LocalDate dob = LocalDate.parse(dobIso.trim()); // expects YYYY-MM-DD
            if (dob.isAfter(LocalDate.now())) return 0;
            int years = Period.between(dob, LocalDate.now()).getYears();
            return Math.max(0, years);
        } catch (Exception e) {
            return 0;
        }
    }

    // --------- LIST BY WORK AREA ----------
    public List<StaffRow> listByWorkArea(String workArea) {
        List<StaffRow> list = new ArrayList<>();

        // ✅ If dob exists then compute age. Else fallback to stored age
        String sql = """
            SELECT user_id, user_name, work_area, gender, address, dob,
                   CASE
                       WHEN dob IS NOT NULL AND dob <> ''
                       THEN CAST((julianday('now') - julianday(dob)) / 365.25 AS INT)
                       ELSE COALESCE(age, 0)
                   END AS calc_age
            FROM users
            WHERE user_type = 'employee' AND work_area = ?
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, workArea);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StaffRow s = new StaffRow();
                    s.setUserId(rs.getInt("user_id"));
                    s.setUserName(rs.getString("user_name"));
                    s.setWorkArea(rs.getString("work_area"));
                    s.setGender(rs.getString("gender"));
                    s.setAddress(rs.getString("address"));
                    s.setDob(rs.getString("dob"));
                    s.setAge(rs.getInt("calc_age"));
                    list.add(s);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error in listByWorkArea: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // --------- FIND EMPLOYEE BY ID ----------
    public Optional<User> findEmployeeById(int userId) {
        String sql = """
            SELECT user_id, user_code, user_name, user_type, password, gender, address, work_area, dob,
                   CASE
                       WHEN dob IS NOT NULL AND dob <> ''
                       THEN CAST((julianday('now') - julianday(dob)) / 365.25 AS INT)
                       ELSE COALESCE(age, 0)
                   END AS calc_age
            FROM users
            WHERE user_type = 'employee' AND user_id = ? LIMIT 1
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setUserCode(rs.getString("user_code"));
                    u.setUserName(rs.getString("user_name"));
                    u.setUserType(rs.getString("user_type"));
                    u.setPassword(rs.getString("password"));
                    u.setGender(rs.getString("gender"));
                    u.setAddress(rs.getString("address"));
                    u.setArea(rs.getString("work_area"));
                    u.setDob(rs.getString("dob"));
                    u.setAge(rs.getInt("calc_age"));
                    return Optional.of(u);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error in findEmployeeById: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // --------- UPDATE EMPLOYEE PASSWORD (RESET) ----------
    /** Reset an employee password by storing a NEW hash (raw password is never stored). */
    public boolean updateEmployeePassword(int userId, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.isBlank()) return false;
        if (!PasswordUtil.isStrongEnough(newRawPassword)) return false;

        String hashed = PasswordUtil.hashPassword(newRawPassword);
        String sql = "UPDATE users SET password = ? WHERE user_type = 'employee' AND user_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("❌ Error in updateEmployeePassword: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --------- LIST ALL EMPLOYEES ----------
    public List<StaffRow> listAll() {
        List<StaffRow> list = new ArrayList<>();

        String sql = """
            SELECT user_id, user_name, work_area, gender, address, dob,
                   CASE
                       WHEN dob IS NOT NULL AND dob <> ''
                       THEN CAST((julianday('now') - julianday(dob)) / 365.25 AS INT)
                       ELSE COALESCE(age, 0)
                   END AS calc_age
            FROM users
            WHERE user_type = 'employee'
            ORDER BY user_id ASC
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                StaffRow s = new StaffRow();
                s.setUserId(rs.getInt("user_id"));
                s.setUserName(rs.getString("user_name"));
                s.setWorkArea(rs.getString("work_area"));
                s.setGender(rs.getString("gender"));
                s.setAddress(rs.getString("address"));
                s.setDob(rs.getString("dob"));
                s.setAge(rs.getInt("calc_age"));
                list.add(s);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching employees: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // --------- COUNT PRODUCTS ASSIGNED TO AN EMPLOYEE ----------
    public int countActiveProductsForEmployee(int userId) {
        String sql = """
            SELECT COUNT(*)
            FROM order_stages
            WHERE employee_id = ?
              AND LOWER(COALESCE(completed,'no')) != 'yes'
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error counting active products: " + e.getMessage());
        }
        return 0;
    }

    // --------- CREATE EMPLOYEE (NEW: DOB) ----------
    // ✅ This is the one your UI should call now (no age input).
    public boolean createEmployee(String userName, String rawPassword, String gender,
                                  String address, String workArea, String dobIso) {

        if (userName == null || userName.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            System.err.println("⚠️ Invalid input for createEmployee: empty username/password");
            return false;
        }

        String genderFormatted = (gender != null && gender.equalsIgnoreCase("female")) ? "Female" : "Male";
        String hashed = PasswordUtil.hashPassword(rawPassword);
        String code = "KC-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        // Keep age column filled too (backward compatibility), but dob is the source of truth
        int computedAge = computeAgeFromDobIso(dobIso);

        String sql = """
            INSERT INTO users (user_type, user_name, password, gender, address, work_area, dob, age, user_code)
            VALUES ('employee', ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userName.trim());
            ps.setString(2, hashed);
            ps.setString(3, genderFormatted);
            ps.setString(4, address == null ? "" : address.trim());
            ps.setString(5, workArea == null ? "" : workArea.trim());
            ps.setString(6, dobIso == null ? "" : dobIso.trim());
            ps.setInt(7, computedAge);
            ps.setString(8, code);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("✅ Employee created successfully: " + userName);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error in createEmployee(dob): " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // --------- CREATE EMPLOYEE (OLD SIGNATURE kept so old code still compiles) ----------
    // ⚠️ Keep this temporarily so you don’t “break working code”.
    // Your UI should stop using this once you add DatePicker DOB.
    public boolean createEmployee(String userName, String rawPassword, String gender,
                                  String address, String workArea, int age) {
        // Store blank dob; keep age as entered (legacy)
        return createEmployee(userName, rawPassword, gender, address, workArea, "");
    }

    // --------- DELETE EMPLOYEE ----------
    public boolean deleteEmployee(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ? AND user_type = 'employee'";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Employee with ID " + userId + " deleted successfully.");
                return true;
            } else {
                System.out.println("⚠️ No employee found with ID " + userId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error deleting employee: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}

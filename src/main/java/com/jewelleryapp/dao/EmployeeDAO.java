package com.jewelleryapp.dao;

import com.kanchancast.auth.PasswordUtil;
import com.kanchancast.model.StaffRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    // --------- LIST BY WORK AREA ----------
    public List<StaffRow> listByWorkArea(String workArea) {
        List<StaffRow> list = new ArrayList<>();
        String sql = "SELECT user_id, user_name, work_area, gender, address, age " +
                "FROM users WHERE user_type = 'employee' AND work_area = ?";

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
                    s.setAge(rs.getInt("age"));
                    list.add(s);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error in listByWorkArea: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }


    // --------- LIST ALL EMPLOYEES ----------
    public List<StaffRow> listAll() {
        List<StaffRow> list = new ArrayList<>();
        String sql = "SELECT user_id, user_name, work_area, gender, address, age " +
                "FROM users WHERE user_type = 'employee' ORDER BY user_id ASC";

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
                s.setAge(rs.getInt("age"));
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
        AND (completed IS NULL OR completed = 0)
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


    // --------- CREATE EMPLOYEE ----------
    /**
     * Creates a new employee with all details, including gender, age, and work area.
     */
    public boolean createEmployee(String userName, String rawPassword, String gender,
                                  String address, String workArea, int age) {

        if (userName == null || userName.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            System.err.println("⚠️ Invalid input for createEmployee: empty username/password");
            return false;
        }

        // Convert gender from "Male"/"Female" to consistent DB format
        String genderFormatted = (gender != null && gender.equalsIgnoreCase("female")) ? "Female" : "Male";

        // Securely hash password
        String hashed = PasswordUtil.hashPassword(rawPassword);

        // Generate unique user code
        String code = "KC-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        String sql = "INSERT INTO users (user_type, user_name, password, gender, address, work_area, age, user_code) " +
                "VALUES ('employee', ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userName.trim());
            ps.setString(2, hashed);
            ps.setString(3, genderFormatted);
            ps.setString(4, address == null ? "" : address.trim());
            ps.setString(5, workArea == null ? "" : workArea.trim());
            ps.setInt(6, age);
            ps.setString(7, code);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("✅ Employee created successfully: " + userName);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error in createEmployee: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
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

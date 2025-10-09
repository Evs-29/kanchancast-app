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
        String sql = "SELECT user_id, user_name, work_area, gender, address " +
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
        String sql = "SELECT user_id, user_name, work_area, gender, address " +
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
                list.add(s);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching employees: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // --------- CREATE EMPLOYEE ----------
    public boolean createEmployee(String userName, String rawPassword, String workArea,
                                  String address, String gender) {
        if (userName == null || userName.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            System.err.println("⚠️ Invalid input for createEmployee: empty username/password");
            return false;
        }

        String hashed = PasswordUtil.hashPassword(rawPassword);
        String code = "KC-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        String sql = "INSERT INTO users (user_type, user_name, password, address, gender, user_code, work_area) " +
                "VALUES ('employee', ?, ?, ?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, userName.trim());
            ps.setString(2, hashed);
            ps.setString(3, address == null ? "" : address.trim());
            ps.setString(4, gender == null ? "" : gender.trim());
            ps.setString(5, code);
            ps.setString(6, workArea == null ? "" : workArea.trim());

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
}

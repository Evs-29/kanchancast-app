package com.jewelleryapp.dao;

import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StageRow;
import com.kanchancast.model.AssignedTask;

import java.sql.*;
import java.util.*;

public class OrderDAO {

    // ---------- CREATE ORDER ----------
    public boolean createOrder(int userId, int productId, String status) {
        String sql = """
            INSERT INTO orders (user_id, product_id, date_ordered, status, progress)
            VALUES (?, ?, DATE('now'), ?, 0)
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setString(3, status);

            int rows = ps.executeUpdate();
            System.out.println(rows == 1 ? "✅ Order inserted successfully" : "⚠️ Order insert failed");
            return rows == 1;

        } catch (SQLException e) {
            System.err.println("❌ Error inserting order: " + e.getMessage());
            return false;
        }
    }

    // ---------- FETCH ORDERS FOR SPECIFIC USER ----------
    public List<OrderSummary> getOrdersForUser(int userId) {
        List<OrderSummary> list = new ArrayList<>();
        String sql = """
            SELECT o.order_id, p.name AS product_name, o.date_ordered, o.status, o.progress
            FROM orders o
            JOIN products p ON o.product_id = p.product_id
            WHERE o.user_id = ?
            ORDER BY o.order_id DESC
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                OrderSummary os = new OrderSummary();
                os.setOrderId(rs.getInt("order_id"));
                os.setProductName(rs.getString("product_name"));
                os.setDateOrdered(rs.getString("date_ordered"));
                os.setStatus(rs.getString("status"));
                os.setProgressPercent(rs.getInt("progress"));
                list.add(os);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching user orders: " + e.getMessage());
        }
        return list;
    }

    // ---------- FETCH ALL ORDERS (ADMIN / OWNER VIEW) ----------
    public List<OrderSummary> listAll() {
        List<OrderSummary> list = new ArrayList<>();
        String sql = """
            SELECT o.order_id, u.user_name AS customer_name, p.name AS product_name,
                   o.date_ordered, o.status, o.progress
            FROM orders o
            JOIN users u ON o.user_id = u.user_id
            JOIN products p ON o.product_id = p.product_id
            ORDER BY o.order_id DESC
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OrderSummary os = new OrderSummary();
                os.setOrderId(rs.getInt("order_id"));
                os.setCustomerName(rs.getString("customer_name"));
                os.setProductName(rs.getString("product_name"));
                os.setDateOrdered(rs.getString("date_ordered"));
                os.setStatus(rs.getString("status"));
                os.setProgressPercent(rs.getInt("progress"));
                list.add(os);
            }
        } catch (SQLException e) {
            System.err.println("❌ listAll(): " + e.getMessage());
        }
        return list;
    }

    // ---------- ENSURE ORDER STAGES EXIST ----------
    public boolean ensureOrderStagesExist(int orderId) {
        String[] stages = {
                "Raw Material Procurement", "Design & CAD Modelling", "Wax Model Creation",
                "Investment Casting", "Cleaning & Devesting", "Filing & Pre-Polishing",
                "Stone Setting", "Final Polishing", "Plating", "Quality Control", "Packaging & Dispatch"
        };

        String checkSQL = "SELECT COUNT(*) FROM order_stages WHERE order_id = ?";
        String insertSQL = "INSERT INTO order_stages (order_id, stage_name, employee_id, completed) VALUES (?, ?, NULL, 'No')";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement check = c.prepareStatement(checkSQL)) {

            check.setInt(1, orderId);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true;

            try (PreparedStatement ins = c.prepareStatement(insertSQL)) {
                for (String s : stages) {
                    ins.setInt(1, orderId);
                    ins.setString(2, s);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return true;

        } catch (SQLException e) {
            System.err.println("⚠️ ensureOrderStagesExist: " + e.getMessage());
            return false;
        }
    }

    // ---------- ASSIGN EMPLOYEE TO STAGE ----------
    /**
     * Assigns (or re-assigns) an employee to a specific stage for an order.
     * Creates the stage record if it doesn't already exist.
     */
    public boolean assignEmployeeToStage(int orderId, String stageName, int employeeId) {
        String sql = """
        INSERT INTO order_stages (order_id, stage_name, employee_id, completed)
        VALUES (?, ?, ?, 0)
        ON CONFLICT(order_id, stage_name)
        DO UPDATE SET employee_id = excluded.employee_id
    """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setString(2, stageName);
            ps.setInt(3, employeeId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Assigned employee " + employeeId +
                        " to stage '" + stageName + "' for order " + orderId);
                return true;
            } else {
                System.out.println("⚠️ No rows updated for order " + orderId + " stage " + stageName);
            }
        } catch (SQLException e) {
            System.err.println("❌ assignEmployeeToStage error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ---------- MARK STAGE COMPLETION ----------
    public boolean setStageCompletion(int orderId, String stageName, boolean completed) {
        String sql = "UPDATE order_stages SET completed = ? WHERE order_id = ? AND stage_name = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, completed ? "Yes" : "No");
            ps.setInt(2, orderId);
            ps.setString(3, stageName);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ setStageCompletion: " + e.getMessage());
            return false;
        }
    }

    // ---------- FETCH EMPLOYEE ASSIGNMENTS (for Admin / Owner) ----------
    public Map<String, String> getAssignedEmployeesDetailed(int orderId) {
        Map<String, String> assigned = new LinkedHashMap<>();
        String sql = """
            SELECT s.stage_name, u.user_name
            FROM order_stages s
            LEFT JOIN users u ON s.employee_id = u.user_id
            WHERE s.order_id = ?
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assigned.put(
                        rs.getString("stage_name"),
                        rs.getString("user_name") != null ? rs.getString("user_name") : "Unassigned"
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ getAssignedEmployeesDetailed: " + e.getMessage());
        }
        return assigned;
    }

    // ---------- FETCH STAGE STATUS ----------
    public List<StageRow> listStagesForOrder(int orderId) {
        List<StageRow> stages = new ArrayList<>();
        String sql = "SELECT stage_name, employee_id, completed FROM order_stages WHERE order_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StageRow s = new StageRow();
                s.setStage(rs.getString("stage_name"));
                s.setEmployeeName(String.valueOf(rs.getInt("employee_id")));
                s.setCompletedText(rs.getString("completed"));
                stages.add(s);
            }
        } catch (SQLException e) {
            System.err.println("❌ listStagesForOrder: " + e.getMessage());
        }
        return stages;
    }

    // ---------- USED BY OWNER DASHBOARD ----------
    public List<OrderSummary> listAllWithAssignments() {
        List<OrderSummary> list = new ArrayList<>();
        String sql = """
            SELECT o.order_id, u.user_name AS customer_name, p.name AS product_name,
                   o.date_ordered, o.status,
                   COUNT(s.employee_id) AS assigned_stages
            FROM orders o
            JOIN users u ON o.user_id = u.user_id
            JOIN products p ON o.product_id = p.product_id
            LEFT JOIN order_stages s ON o.order_id = s.order_id
            GROUP BY o.order_id
            ORDER BY o.date_ordered DESC
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OrderSummary os = new OrderSummary();
                os.setOrderId(rs.getInt("order_id"));
                os.setCustomerName(rs.getString("customer_name"));
                os.setProductName(rs.getString("product_name"));
                os.setDateOrdered(rs.getString("date_ordered"));
                os.setStatus(rs.getString("status"));
                os.setProgressPercent(rs.getInt("assigned_stages"));
                list.add(os);
            }

        } catch (SQLException e) {
            System.err.println("❌ listAllWithAssignments: " + e.getMessage());
        }
        return list;
    }

    // ---------- EMPLOYEE VIEW ----------
    public List<AssignedTask> listTasksAssignedToEmployee(int employeeId) {
        List<AssignedTask> list = new ArrayList<>();
        String sql = """
            SELECT o.order_id, s.stage_name, p.name AS product_name,
                   u.user_name AS customer_name, s.completed
            FROM order_stages s
            JOIN orders o ON s.order_id = o.order_id
            JOIN products p ON o.product_id = p.product_id
            JOIN users u ON o.user_id = u.user_id
            WHERE s.employee_id = ?
            ORDER BY o.order_id DESC
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AssignedTask t = new AssignedTask();
                t.setOrderId(rs.getInt("order_id"));
                t.setStage(rs.getString("stage_name"));
                t.setProductName(rs.getString("product_name"));
                t.setCustomerName(rs.getString("customer_name"));
                t.setCompleted("Yes".equalsIgnoreCase(rs.getString("completed")));
                list.add(t);
            }
        } catch (SQLException e) {
            System.err.println("❌ listTasksAssignedToEmployee: " + e.getMessage());
        }
        return list;
    }
    public Map<String, Integer> getAssignedEmployeesForOrder(int orderId) {
        Map<String, Integer> assigned = new HashMap<>();
        String sql = "SELECT stage_name, employee_id FROM order_stages WHERE order_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assigned.put(rs.getString("stage_name"), rs.getInt("employee_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error in getAssignedEmployeesForOrder: " + e.getMessage());
            e.printStackTrace();
        }
        return assigned;
    }
    // Returns count of orders per product (for PieChart)
    public Map<String, Integer> getProductSalesCount() {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT p.name, COUNT(o.order_id) AS count FROM orders o JOIN products p ON o.product_id = p.product_id GROUP BY p.name";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("name"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("❌ getProductSalesCount: " + e.getMessage());
        }
        return data;
    }

    // Returns number of orders per customer (for BarChart)
    public Map<String, Integer> getOrdersPerCustomer() {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT u.user_name, COUNT(o.order_id) AS count FROM orders o JOIN users u ON o.user_id = u.user_id GROUP BY u.user_name";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("user_name"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("❌ getOrdersPerCustomer: " + e.getMessage());
        }
        return data;
    }
    // ✅ Mark a specific stage as completed (sets completed = 1)
    public boolean markStageAsCompleted(int orderId, String stageName) {
        String sql = "UPDATE order_stages SET completed = 1 WHERE order_id = ? AND stage_name = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, stageName);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Stage " + stageName + " for Order #" + orderId + " marked completed.");
                return true;
            } else {
                System.out.println("⚠️ No matching stage found to mark completed.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ markStageAsCompleted error: " + e.getMessage());
            return false;
        }
    }

    public boolean markStageAsIncomplete(int orderId, String stageName) {
        String sql = "UPDATE order_stages SET completed = 0 WHERE order_id = ? AND stage_name = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, stageName);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Stage " + stageName + " for Order #" + orderId + " marked incomplete.");
                return true;
            } else {
                System.out.println("⚠️ No matching stage found to mark incomplete.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ markStageAsIncomplete error: " + e.getMessage());
            return false;
        }
    }
    public Map<String, Integer> getAssignedEmployeeIdsForOrder(int orderId) {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT stage_name, employee_id FROM order_stages WHERE order_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("stage_name"), rs.getInt("employee_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ getAssignedEmployeeIdsForOrder: " + e.getMessage());
            e.printStackTrace();
        }

        return map;
    }
}

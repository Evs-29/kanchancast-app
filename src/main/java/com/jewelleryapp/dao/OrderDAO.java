package com.jewelleryapp.dao;

import com.kanchancast.model.AssignedTask;
import com.kanchancast.model.OrderSummary;
import com.kanchancast.model.StageRow;

import java.sql.*;
import java.util.*;

public class OrderDAO {

    // Single source of truth: 11 stages
    private static final String[] STAGES = com.kanchancast.model.StageEnum.labels();

    // ✅ Delivery date calculation (uses products.duration_amount + products.duration_unit)
    // - DAYS:   +N days
    // - WEEKS:  +N*7 days
    // - MONTHS: +N months
    // - if duration missing/0: delivery_date = date_ordered
    private static final String DELIVERY_DATE_EXPR = """
        CASE
          WHEN p.duration_amount IS NULL OR p.duration_amount <= 0 THEN o.date_ordered
          WHEN UPPER(COALESCE(p.duration_unit,'DAYS')) = 'DAYS'
            THEN DATE(o.date_ordered, '+' || p.duration_amount || ' days')
          WHEN UPPER(COALESCE(p.duration_unit,'DAYS')) = 'WEEKS'
            THEN DATE(o.date_ordered, '+' || (p.duration_amount * 7) || ' days')
          WHEN UPPER(COALESCE(p.duration_unit,'DAYS')) = 'MONTHS'
            THEN DATE(o.date_ordered, '+' || p.duration_amount || ' months')
          ELSE DATE(o.date_ordered, '+' || p.duration_amount || ' days')
        END
    """;

    // Normalize stage names that already exist in your DB/UI
    private String canonicalStageName(String name) {
        if (name == null) return "";
        String s = name.trim();

        if (s.equalsIgnoreCase("Raw Material Procurement and Management")) return "Raw Material Procurement";
        if (s.equalsIgnoreCase("Raw material procurement and management")) return "Raw Material Procurement";
        if (s.equalsIgnoreCase("Raw material procedure and management")) return "Raw Material Procurement";

        return s;
    }

    // ---------- CREATE ORDER ----------
    public boolean createOrder(int userId, int productId, String status) {
        String sql = """
            INSERT INTO orders (user_id, product_id, date_ordered, status, progress)
            VALUES (?, ?, DATE('now'), ?, 0)
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setString(3, status);

            int rows = ps.executeUpdate();
            if (rows != 1) return false;

            int orderId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) orderId = keys.getInt(1);
            }

            if (orderId > 0) {
                ensureOrderStagesExist(orderId);
                recalculateAndUpdateOrderProgress(orderId);
            }

            return true;

        } catch (SQLException e) {
            System.err.println("❌ createOrder: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ---------- FETCH ORDERS FOR SPECIFIC USER ----------
    public List<OrderSummary> getOrdersForUser(int userId) {
        List<OrderSummary> list = new ArrayList<>();

        String sql = """
            SELECT o.order_id, o.product_id, o.user_id,
                   p.name AS product_name,
                   o.date_ordered,
                   %s AS delivery_date,
                   o.status, o.progress
            FROM orders o
            JOIN products p ON o.product_id = p.product_id
            WHERE o.user_id = ?
            ORDER BY (CASE WHEN o.progress >= 100 THEN 1 ELSE 0 END) ASC, o.order_id DESC
        """.formatted(DELIVERY_DATE_EXPR);

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderSummary os = new OrderSummary();
                    os.setOrderId(rs.getInt("order_id"));
                    os.setProductId(rs.getInt("product_id"));
                    os.setUserId(rs.getInt("user_id"));
                    os.setProductName(rs.getString("product_name"));
                    os.setDateOrdered(rs.getString("date_ordered"));

                    // ✅ NEW
                    os.setDeliveryDate(rs.getString("delivery_date"));

                    int progress = rs.getInt("progress");
                    String status = rs.getString("status");

                    // Safety-net: if DB has old status text, reconcile it from progress.
                    String canonical = canonicalStatusFromProgress(progress);
                    if (canonical != null && (status == null || !canonical.equalsIgnoreCase(status))) {
                        updateOrderStatusOnly(rs.getInt("order_id"), canonical);
                        status = canonical;
                    }

                    os.setStatus(status);
                    os.setProgressPercent(progress);
                    list.add(os);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ getOrdersForUser: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // ---------- FETCH ALL ORDERS (ADMIN / OWNER VIEW) ----------
    public List<OrderSummary> listAll() {
        List<OrderSummary> list = new ArrayList<>();

        String sql = """
            SELECT o.order_id, o.product_id, o.user_id,
                   u.user_name AS customer_name,
                   p.name AS product_name,
                   o.date_ordered,
                   %s AS delivery_date,
                   o.status, o.progress
            FROM orders o
            JOIN users u ON o.user_id = u.user_id
            JOIN products p ON o.product_id = p.product_id
            ORDER BY (CASE WHEN o.progress >= 100 THEN 1 ELSE 0 END) ASC, o.order_id DESC
        """.formatted(DELIVERY_DATE_EXPR);

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OrderSummary os = new OrderSummary();
                os.setOrderId(rs.getInt("order_id"));
                os.setProductId(rs.getInt("product_id"));
                os.setUserId(rs.getInt("user_id"));
                os.setCustomerName(rs.getString("customer_name"));
                os.setProductName(rs.getString("product_name"));
                os.setDateOrdered(rs.getString("date_ordered"));

                // ✅ NEW
                os.setDeliveryDate(rs.getString("delivery_date"));

                int progress = rs.getInt("progress");
                String status = rs.getString("status");

                // Safety-net: reconcile old status text for existing completed orders.
                String canonical = canonicalStatusFromProgress(progress);
                if (canonical != null && (status == null || !canonical.equalsIgnoreCase(status))) {
                    updateOrderStatusOnly(rs.getInt("order_id"), canonical);
                    status = canonical;
                }

                os.setStatus(status);
                os.setProgressPercent(progress);
                list.add(os);
            }

        } catch (SQLException e) {
            System.err.println("❌ listAll: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    // ---------- ENSURE ALL 11 STAGES EXIST ----------
    // IMPORTANT: inserts missing stages even if some rows already exist
    public boolean ensureOrderStagesExist(int orderId) {
        String fetchSql = "SELECT stage_name FROM order_stages WHERE order_id = ?";
        String insertSql = "INSERT INTO order_stages (order_id, stage_name, employee_id, completed) VALUES (?, ?, NULL, 'No')";

        try (Connection c = DatabaseConnection.getConnection()) {

            Set<String> existing = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement(fetchSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        existing.add(canonicalStageName(rs.getString("stage_name")));
                    }
                }
            }

            // Insert missing official stages
            try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                for (String stage : STAGES) {
                    if (!existing.contains(stage)) {
                        ins.setInt(1, orderId);
                        ins.setString(2, stage);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }

            return true;

        } catch (SQLException e) {
            System.err.println("⚠️ ensureOrderStagesExist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ---------- ASSIGN EMPLOYEE TO STAGE ----------
    public boolean assignEmployeeToStage(int orderId, String stageName, int employeeId) {
        ensureOrderStagesExist(orderId);
        stageName = canonicalStageName(stageName);

        String updateSql = """
            UPDATE order_stages
            SET employee_id = ?
            WHERE order_id = ? AND stage_name = ?
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(updateSql)) {

            ps.setInt(1, employeeId);
            ps.setInt(2, orderId);
            ps.setString(3, stageName);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ assignEmployeeToStage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ---------- MARK STAGE COMPLETION ----------
    public boolean setStageCompletion(int orderId, String stageName, boolean completed) {
        ensureOrderStagesExist(orderId);
        stageName = canonicalStageName(stageName);

        String sql = "UPDATE order_stages SET completed = ? WHERE order_id = ? AND stage_name = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, completed ? "Yes" : "No");
            ps.setInt(2, orderId);
            ps.setString(3, stageName);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                recalculateAndUpdateOrderProgress(orderId);
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ setStageCompletion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean markStageAsCompleted(int orderId, String stageName) {
        return setStageCompletion(orderId, stageName, true);
    }

    public boolean markStageAsIncomplete(int orderId, String stageName) {
        return setStageCompletion(orderId, stageName, false);
    }

    // ---------- STAGES FOR CUSTOMER TRACKING (NO EMPLOYEE) ----------
    public List<StageRow> listStagesForCustomerTracking(int orderId) {
        ensureOrderStagesExist(orderId);
        try {
            recalculateAndUpdateOrderProgress(orderId);
        } catch (SQLException e) {
            System.err.println("❌ Progress recalc failed for order #" + orderId + ": " + e.getMessage());
            e.printStackTrace();
        }

        Map<String, String> doneByStage = new HashMap<>();
        String sql = "SELECT stage_name, completed FROM order_stages WHERE order_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String stage = canonicalStageName(rs.getString("stage_name"));
                    String completed = rs.getString("completed");

                    // If duplicates exist, keep Yes if any row is Yes
                    String existing = doneByStage.get(stage);
                    if ("Yes".equalsIgnoreCase(existing)) continue;

                    doneByStage.put(stage, completed);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ listStagesForCustomerTracking: " + e.getMessage());
            e.printStackTrace();
        }

        List<StageRow> out = new ArrayList<>();
        for (String stageName : STAGES) {
            StageRow r = new StageRow();
            r.setStage(stageName);
            r.setEmployeeName("");
            r.setCompletedText(doneByStage.getOrDefault(stageName, "No"));
            out.add(r);
        }
        return out;
    }

    // ---------- STAGES FOR ADMIN/OWNER (shows employee name) ----------
    public List<StageRow> listStagesForOrder(int orderId) {
        ensureOrderStagesExist(orderId);

        List<StageRow> stages = new ArrayList<>();
        String sql = """
            SELECT s.stage_name,
                   COALESCE(u.user_name, 'Unassigned') AS employee_name,
                   s.completed
            FROM order_stages s
            LEFT JOIN users u ON s.employee_id = u.user_id
            WHERE s.order_id = ?
            ORDER BY s.stage_id
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StageRow s = new StageRow();
                    s.setStage(rs.getString("stage_name"));
                    s.setEmployeeName(rs.getString("employee_name"));
                    s.setCompletedText(rs.getString("completed"));
                    stages.add(s);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ listStagesForOrder: " + e.getMessage());
            e.printStackTrace();
        }
        return stages;
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
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AssignedTask t = new AssignedTask();
                    t.setOrderId(rs.getInt("order_id"));
                    t.setStage(rs.getString("stage_name"));
                    t.setProductName(rs.getString("product_name"));
                    t.setCustomerName(rs.getString("customer_name"));
                    t.setCompleted("Yes".equalsIgnoreCase(rs.getString("completed")));
                    list.add(t);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ listTasksAssignedToEmployee: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // Progress out of 11 calculation
    private void recalculateAndUpdateOrderProgress(int orderId) throws SQLException {

        try (Connection c = DatabaseConnection.getConnection()) {

            // Make sure all 11 official stages exist (same connection)
            ensureOrderStagesExist(c, orderId);

            // Read stage completion (same connection)
            Map<String, String> doneByStage = new HashMap<>();
            String readSql = "SELECT stage_name, completed FROM order_stages WHERE order_id = ?";

            try (PreparedStatement ps = c.prepareStatement(readSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String stage = canonicalStageName(rs.getString("stage_name"));
                        String completed = rs.getString("completed");

                        // if duplicates exist, keep Yes if any row is Yes
                        String existing = doneByStage.get(stage);
                        if ("Yes".equalsIgnoreCase(existing)) continue;

                        doneByStage.put(stage, completed);
                    }
                }
            }

            int done = 0;
            for (String s : STAGES) {
                if ("Yes".equalsIgnoreCase(doneByStage.getOrDefault(s, "No"))) done++;
            }

            int progress = (int) Math.round((done * 100.0) / STAGES.length);

            // Canonical status derived from progress
            String status = canonicalStatusFromProgress(progress);

            // Update orders.progress + orders.status (same connection)
            try (PreparedStatement up = c.prepareStatement("UPDATE orders SET progress = ?, status = ? WHERE order_id = ?")) {
                up.setInt(1, progress);
                up.setString(2, status);
                up.setInt(3, orderId);
                up.executeUpdate();
            }
        }
    }

    // ---------- STATUS HELPERS ----------
    private String canonicalStatusFromProgress(int progress) {
        if (progress >= 100) return "COMPLETED";
        if (progress <= 0) return "PENDING";
        return "PROCESSING";
    }

    private void updateOrderStatusOnly(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ updateOrderStatusOnly: " + e.getMessage());
        }
    }

    public Map<String, Integer> getAssignedEmployeeIdsForOrder(int orderId) {
        ensureOrderStagesExist(orderId);

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

    private boolean ensureOrderStagesExist(Connection c, int orderId) throws SQLException {
        String fetchSql = "SELECT stage_name FROM order_stages WHERE order_id = ?";
        String insertSql = "INSERT INTO order_stages (order_id, stage_name, employee_id, completed) VALUES (?, ?, NULL, 'No')";

        Set<String> existing = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(fetchSql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.add(canonicalStageName(rs.getString("stage_name")));
                }
            }
        }

        try (PreparedStatement ins = c.prepareStatement(insertSql)) {
            boolean any = false;
            for (String stage : STAGES) {
                if (!existing.contains(stage)) {
                    ins.setInt(1, orderId);
                    ins.setString(2, stage);
                    ins.addBatch();
                    any = true;
                }
            }
            if (any) ins.executeBatch();
        }

        return true;
    }

    // DELETE ORDER
    public boolean deleteOrder(int orderId) {
        String delStages = "DELETE FROM order_stages WHERE order_id = ?";
        String delOrder  = "DELETE FROM orders WHERE order_id = ?";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement ps1 = c.prepareStatement(delStages);
                 PreparedStatement ps2 = c.prepareStatement(delOrder)) {

                ps1.setInt(1, orderId);
                ps1.executeUpdate();

                ps2.setInt(1, orderId);
                int rows = ps2.executeUpdate();

                c.commit();
                return rows > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("❌ deleteOrder: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
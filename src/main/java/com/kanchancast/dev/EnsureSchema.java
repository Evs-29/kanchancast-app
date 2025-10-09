package com.kanchancast.dev;

import com.jewelleryapp.dao.DatabaseConnection;

import java.security.SecureRandom;
import java.sql.*;

/**
 * Safe to re-run:
 * - Ensures base tables exist
 * - Adds users.user_code and users.work_area if missing
 * - Backfills user_code
 * - Creates order_assignments (order_id, stage TEXT, employee_id) with PK(order_id, stage)
 * - UNIQUE index on users.user_code
 */
public class EnsureSchema {

    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");

                // USERS, PRODUCTS, ORDERS creation (unchanged)
                st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_type TEXT NOT NULL,
                        user_name TEXT NOT NULL UNIQUE,
                        password  TEXT NOT NULL,
                        address   TEXT,
                        gender    TEXT
                    )
                """);

                st.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        product_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name       TEXT,
                        type       TEXT,
                        karat      REAL,
                        weight     REAL,
                        price      REAL,
                        image_path TEXT
                    )
                """);

                st.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        order_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id      INTEGER NOT NULL,
                        product_id   INTEGER NOT NULL,
                        status       TEXT,
                        date_ordered TEXT,
                        FOREIGN KEY(user_id)    REFERENCES users(user_id),
                        FOREIGN KEY(product_id) REFERENCES products(product_id)
                    )
                """);
            }

            // ensure columns
            ensureColumn(conn, "users", "user_code", "ALTER TABLE users ADD COLUMN user_code TEXT");
            backfillMissingUserCodes(conn);
            ensureUniqueIndexOnUserCode(conn);
            ensureColumn(conn, "users", "work_area", "ALTER TABLE users ADD COLUMN work_area TEXT");

            // assignments table (admin picks which employee per stage)
            ensureOrderAssignmentsTable(conn);

            // NEW: simple progress table (one row per (order,stage) when the employee marks it)
            ensureOrderProgressTable(conn);

            System.out.println("✅ Schema verified: user_code, work_area, order_assignments, order_progress ready.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- existing helpers (ensureColumn, backfillMissingUserCodes, ensureUniqueIndexOnUserCode, ensureOrderAssignmentsTable) stay the same ---

    private static void ensureOrderProgressTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS order_progress (
                    order_id     INTEGER NOT NULL,
                    stage        TEXT    NOT NULL,
                    completed    INTEGER NOT NULL DEFAULT 0,  -- 0/1
                    completed_at TEXT,
                    PRIMARY KEY (order_id, stage),
                    FOREIGN KEY(order_id) REFERENCES orders(order_id) ON DELETE CASCADE
                )
            """);
        }
    }


    private static void ensureColumn(Connection conn, String table, String column, String alterSql) throws SQLException {
        boolean has = false;
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) { has = true; break; }
        }
        if (!has) {
            try (Statement st = conn.createStatement()) {
                st.execute(alterSql);
            }
            System.out.println("Added column " + table + "." + column);
        }
    }

    private static void backfillMissingUserCodes(Connection conn) throws SQLException {
        String selectSql = "SELECT user_id FROM users WHERE user_code IS NULL OR user_code = ''";
        String checkSql  = "SELECT 1 FROM users WHERE user_code = ? LIMIT 1";
        String updateSql = "UPDATE users SET user_code = ? WHERE user_id = ?";

        try (PreparedStatement select = conn.prepareStatement(selectSql);
             PreparedStatement check  = conn.prepareStatement(checkSql);
             PreparedStatement update = conn.prepareStatement(updateSql);
             ResultSet rs = select.executeQuery()) {

            while (rs.next()) {
                int uid = rs.getInt("user_id");
                String code;
                do {
                    code = generateUserCode();
                    check.setString(1, code);
                } while (check.executeQuery().next());

                update.setString(1, code);
                update.setInt(2, uid);
                update.executeUpdate();
                System.out.println("Backfilled user_code for user_id=" + uid + " -> " + code);
            }
        }
    }

    private static void ensureUniqueIndexOnUserCode(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_users_user_code
                ON users(user_code)
            """);
        }
    }

    private static void ensureOrderAssignmentsTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS order_assignments (
                    order_id    INTEGER NOT NULL,
                    stage       TEXT    NOT NULL,
                    employee_id INTEGER NOT NULL,
                    PRIMARY KEY (order_id, stage),
                    FOREIGN KEY(order_id)    REFERENCES orders(order_id)    ON DELETE CASCADE,
                    FOREIGN KEY(employee_id) REFERENCES users(user_id)      ON DELETE RESTRICT
                )
            """);
        }
    }

    // KC- + 12 base62 chars
    private static final char[] ALPH = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();
    public static String generateUserCode() {
        StringBuilder sb = new StringBuilder("KC-");
        for (int i = 0; i < 12; i++) sb.append(ALPH[RNG.nextInt(ALPH.length)]);
        return sb.toString();
    }
}

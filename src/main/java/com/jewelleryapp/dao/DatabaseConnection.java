package com.jewelleryapp.dao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * Centralized database connection manager for KanchanCast.
 * Uses root-level kanchancast.db and applies safe PRAGMAs.
 * Adds retry logic to handle SQLITE_BUSY (database locked).
 */
public class DatabaseConnection {

    private static final Path DB_PATH = Paths.get(System.getProperty("user.dir"), "kanchancast.db");
    private static final String URL = "jdbc:sqlite:" + DB_PATH.toAbsolutePath();

    private static volatile boolean bootstrapped = false;
    private static final Object BOOTSTRAP_LOCK = new Object();

    public static Connection getConnection() throws SQLException {
        return connect();
    }

    public static Connection connect() throws SQLException {
        final int maxAttempts = 10;
        final long sleepMs = 200;
        SQLException last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(URL);
                applyPragmas(conn);

                System.out.println("✅ Connected to SQLite database: " + DB_PATH.toAbsolutePath());

                // Bootstrap once (but ONLY mark successful after it actually succeeds)
                ensureBootstrapped();

                return conn;

            } catch (SQLException e) {
                last = e;

                // close partially opened connection
                if (conn != null) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }

                if (!isBusyLock(e)) {
                    throw e;
                }

                // Busy/locked: retry after small sleep
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw last != null ? last : new SQLException("Failed to connect to DB (unknown error).");
    }

    private static void applyPragmas(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 8000"); // helps avoid SQLITE_BUSY
            // WAL greatly reduces read/write contention in SQLite
            try { st.execute("PRAGMA journal_mode = WAL"); } catch (SQLException ignored) {}
        }
    }

    private static void ensureBootstrapped() {
        if (bootstrapped) return;

        synchronized (BOOTSTRAP_LOCK) {
            if (bootstrapped) return;

            boolean ok = false;

            // IMPORTANT: do bootstrap using a fresh short-lived connection
            // so you don’t keep a long-running app connection holding locks.
            try (Connection c = DriverManager.getConnection(URL)) {
                applyPragmas(c);

                // Do schema updates as a transaction (reduces locking window)
                c.setAutoCommit(false);

                try (Statement st = c.createStatement()) {

                    st.execute("""
                        CREATE TABLE IF NOT EXISTS categories (
                            name TEXT PRIMARY KEY
                        )
                    """);

                    st.execute("""
                        INSERT OR IGNORE INTO categories(name)
                        VALUES ('ring'), ('necklace'), ('bracelet'), ('earrings')
                    """);

                    // Safe schema upgrades (ignore "duplicate column" errors)
                    try { st.execute("ALTER TABLE products ADD COLUMN stone_weight REAL DEFAULT 0"); }
                    catch (SQLException ignored) {}

                    try { st.execute("ALTER TABLE products ADD COLUMN duration_amount INTEGER DEFAULT 0"); }
                    catch (SQLException ignored) {}

                    try { st.execute("ALTER TABLE products ADD COLUMN duration_unit TEXT DEFAULT 'DAYS'"); }
                    catch (SQLException ignored) {}

                    c.commit();
                    ok = true;

                } catch (SQLException e) {
                    try { c.rollback(); } catch (SQLException ignored) {}
                    System.err.println("⚠️ DB bootstrap skipped/failed: " + e.getMessage());
                } finally {
                    try { c.setAutoCommit(true); } catch (SQLException ignored) {}
                }

            } catch (SQLException e) {
                System.err.println("⚠️ DB bootstrap skipped/failed: " + e.getMessage());
            }

            bootstrapped = ok; // ONLY true if success; otherwise will retry later
        }
    }

    private static boolean isBusyLock(SQLException e) {
        String msg = (e.getMessage() == null) ? "" : e.getMessage().toUpperCase();
        return msg.contains("SQLITE_BUSY")
                || msg.contains("DATABASE IS LOCKED")
                || msg.contains("LOCKED");
    }
}

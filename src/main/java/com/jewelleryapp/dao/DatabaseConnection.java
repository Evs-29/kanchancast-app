package com.jewelleryapp.dao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Centralized database connection manager for KanchanCast.
 * Ensures all DAOs connect to the same SQLite database located
 * at the project root (kanchancast.db).
 */
public class DatabaseConnection {

    // ✅ Always points to the root-level DB file (not inside src/)
    private static final Path DB_PATH = Paths.get(System.getProperty("user.dir"), "kanchancast.db");
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    /**
     * Opens a connection to the SQLite database.
     * Automatically enables foreign keys for relational integrity.
     */
    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        System.out.println("✅ Connected to SQLite database: " + DB_PATH.toAbsolutePath());
        try (Statement st = conn.createStatement()) {
            // Ensure categories table exists (safe if already exists)
            st.execute("""
        CREATE TABLE IF NOT EXISTS categories (
            name TEXT PRIMARY KEY
        )
    """);

            // Insert default categories (ignore if already there)
            st.execute("""
        INSERT OR IGNORE INTO categories(name)
        VALUES ('ring'), ('necklace'), ('bracelet'), ('earrings')
    """);
        } catch (SQLException e) {
            System.err.println("⚠️ DB bootstrap failed: " + e.getMessage());
        }
        return conn;
    }

    /**
     * Legacy alias used by older DAO classes.
     * Calls connect() internally for backward compatibility.
     */
    public static Connection getConnection() throws SQLException {
        return connect();
    }
}

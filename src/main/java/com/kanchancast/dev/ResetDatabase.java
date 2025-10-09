package com.kanchancast.dev;

import com.jewelleryapp.dao.DatabaseConnection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

public class ResetDatabase {
    public static void main(String[] args) {
        try {
            // This path must match DatabaseConnection
            Path dbPath = Paths.get("").toAbsolutePath().resolve("kanchancast.db");
            System.out.println("DB path: " + dbPath);

            // Make sure no process is using the DB
            System.out.println("Closing any eager connection check (no-op)...");
            try (Connection ignored = DatabaseConnection.connect()) {
                // just to print the exact path in your console
            } catch (Exception ignore) {}

            // Delete the file (if exists)
            if (Files.exists(dbPath)) {
                Files.delete(dbPath);
                System.out.println("✅ Deleted old database file.");
            } else {
                System.out.println("ℹ️ No existing DB file found; nothing to delete.");
            }

            System.out.println("Done. Now run EnsureSchema to recreate tables.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.kanchancast.nav;

import javafx.stage.Stage;
import com.kanchancast.auth.LoginScreen;
import com.kanchancast.auth.SignupScreen;
import com.kanchancast.dashboard.AdminDashboard;
import com.kanchancast.dashboard.EmployeeDashboard;
import com.kanchancast.dashboard.OwnerDashboard;
import com.kanchancast.dashboard.CustomerDashboard;
import com.kanchancast.model.User;

/**
 * Central class for navigating between all top-level screens in the app.
 * Handles login, signup, and routing to role-based dashboards.
 */
public final class ScreenRouter {

    private ScreenRouter() {} // prevent instantiation

    // ----------------------------
    // BASIC NAVIGATION
    // ----------------------------

    /** Navigate to the Login screen. */
    public static void goToLogin(Stage stage) {
        LoginScreen.show(stage);
    }

    /** Navigate to the Signup screen. */
    public static void goToSignup(Stage stage) {
        SignupScreen.show(stage);
    }

    // Legacy aliases (for backward compatibility)
    public static void showLogin(Stage stage) { goToLogin(stage); }
    public static void showSignup(Stage stage) { goToSignup(stage); }

    // ----------------------------
    // DASHBOARD ROUTING
    // ----------------------------

    /**
     * Opens the correct dashboard based on the user's role.
     * Supported roles: admin, employee, owner, customer
     */
    public static void showDashboard(Stage stage, User user) {
        if (user == null || user.getUserType() == null) {
            System.err.println("❌ Cannot route to dashboard: user or userType is null.");
            goToLogin(stage);
            return;
        }

        String role = user.getUserType().trim().toLowerCase();

        try {
            switch (role) {
                case "admin" -> {
                    System.out.println("🔹 Routing to Admin Dashboard...");
                    AdminDashboard.show(stage, user);
                }
                case "employee" -> {
                    System.out.println("🔹 Routing to Employee Dashboard...");
                    EmployeeDashboard.show(stage, user);
                }
                case "owner" -> {
                    System.out.println("🔹 Routing to Owner Dashboard...");
                    OwnerDashboard.show(stage, user);
                }
                case "customer" -> {
                    System.out.println("🔹 Routing to Customer Dashboard...");
                    CustomerDashboard.show(stage, user);
                }
                default -> {
                    System.err.println("⚠️ Unknown role: " + role + ". Returning to login.");
                    goToLogin(stage);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error while routing to dashboard: " + e.getMessage());
            e.printStackTrace();
            goToLogin(stage);
        }
    }
}

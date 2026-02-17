package com.kanchancast.nav;

import com.kanchancast.auth.LoginScreen;
import com.kanchancast.auth.SignupScreen;
import com.kanchancast.dashboard.AdminDashboard;
import com.kanchancast.dashboard.CustomerDashboard;
import com.kanchancast.dashboard.EmployeeDashboard;
import com.kanchancast.dashboard.OwnerDashboard;
import com.kanchancast.model.User;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;

/**
 * Central class for navigating between all top-level screens in the app.
 * Handles login, signup, and routing to role-based dashboards.
 *
 * IMPORTANT (macOS fullscreen fix):
 * Setting a new Scene can cause JavaFX to exit fullscreen on macOS.
 * So we preserve fullscreen/maximized state across navigation.
 */
public final class ScreenRouter {

    private ScreenRouter() {
    }

    // ----------------------------
    // WINDOW STATE PRESERVER
    // ----------------------------

    // ----------------------------
    // SCENE REPLACEMENT (Full Screen Fix)
    // ----------------------------

    /**
     * Replaces the root of the current Scene if it exists, otherwise creates a new
     * Scene.
     * This preserves the Window/Stage state (Full Screen, Maximized, Dimensions).
     */
    public static void replaceSceneContent(Stage stage, javafx.scene.Parent root, double width, double height) {
        if (stage == null)
            return;

        Scene currentScene = stage.getScene();
        if (currentScene != null) {
            // ✅ REUSE existing scene to avoid exiting full screen
            currentScene.setRoot(root);
        } else {
            // First time initialization
            stage.setScene(new Scene(root, width, height));
        }

        // Ensure title/show is handled if needed, though usually just setting root is
        // enough
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    private static void navigatePreserveWindow(Stage stage, Runnable navigationAction) {
        if (stage == null)
            return;

        // With replaceSceneContent, we don't strictly need the complex logic below,
        // but we keep it for safety if navigationAction still does setScene()
        final boolean wasFullScreen = stage.isFullScreen();
        final boolean wasMaximized = stage.isMaximized();

        navigationAction.run();

        Platform.runLater(() -> {
            try {
                if (wasFullScreen && !stage.isFullScreen()) {
                    stage.setFullScreen(true);
                } else if (!wasFullScreen && wasMaximized && !stage.isMaximized()) {
                    stage.setMaximized(true);
                }
            } catch (Exception ignored) {
            }
        });
    }

    // ----------------------------
    // BASIC NAVIGATION
    // ----------------------------

    /** Navigate to the Login screen. */
    public static void goToLogin(Stage stage) {
        navigatePreserveWindow(stage, () -> LoginScreen.show(stage));
    }

    /** Navigate to the Signup screen. */
    public static void goToSignup(Stage stage) {
        navigatePreserveWindow(stage, () -> SignupScreen.show(stage));
    }

    // Legacy aliases (for backward compatibility)
    public static void showLogin(Stage stage) {
        goToLogin(stage);
    }

    public static void showSignup(Stage stage) {
        goToSignup(stage);
    }

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

        navigatePreserveWindow(stage, () -> {
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
                        LoginScreen.show(stage);
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error while routing to dashboard: " + e.getMessage());
                e.printStackTrace();
                LoginScreen.show(stage);
            }
        });
    }
}

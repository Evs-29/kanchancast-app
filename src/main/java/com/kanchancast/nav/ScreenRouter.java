package com.kanchancast.nav;

import javafx.stage.Stage;
import com.kanchancast.auth.LoginScreen;
import com.kanchancast.auth.SignupScreen;

/** Central place to navigate between top-level screens. */
public final class ScreenRouter {

    private ScreenRouter() {} // no instances

    /** Navigate to the Login screen. */
    public static void goToLogin(Stage stage) {
        LoginScreen.show(stage);
    }

    /** Navigate to the Signup screen. */
    public static void goToSignup(Stage stage) {
        SignupScreen.show(stage);
    }

    // ---- Legacy aliases to keep older calls compiling ----
    public static void showLogin(Stage stage) { goToLogin(stage); }
    public static void showSignup(Stage stage) { goToSignup(stage); }
}

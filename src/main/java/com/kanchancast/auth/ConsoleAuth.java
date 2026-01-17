package com.kanchancast.auth;

import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.User;

import java.sql.Connection;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleAuth {
    public static void main(String[] args) throws Exception {
        try (Connection c = com.jewelleryapp.dao.DatabaseConnection.getConnection()) {
            System.out.println("DB OK: " + c.getMetaData().getURL());
        }

        Scanner sc = new Scanner(System.in);
        System.out.print("User code: ");
        String code = sc.nextLine().trim();
        System.out.print("Username: ");
        String user = sc.nextLine().trim();
        System.out.print("Password: ");
        String pass = sc.nextLine().trim();

        UserDAO dao = new UserDAO();
        Optional<User> out = dao.authenticateFull(code, user, pass);

        System.out.println(out.isPresent()
                ? "✅ AUTH OK: " + out.get().getUserType() + " (" + out.get().getUserName() + ")"
                : "❌ AUTH FAIL");
    }
}

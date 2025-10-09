package com.kanchancast.dev;

import com.jewelleryapp.dao.UserDAO;
import com.kanchancast.model.User;

public class SeedUsers {

    public static void main(String[] args) {
        UserDAO dao = new UserDAO();

        // OWNER
        seedOwner(dao, "owner1", "OwnerPass123!", "HQ Office", "Other");

        // ADMIN
        seedAdmin(dao, "admin1", "AdminPass123!", "Front Desk", "Female");

        // EMPLOYEES (with work areas)
        seedEmployee(dao, "emp_raw", "EmpPass123!", "Raw Material Procurement and Management", "Warehouse A", "Male");
        seedEmployee(dao, "emp_cad", "EmpPass123!", "Design & CAD Modelling", "Design Lab", "Female");
    }

    private static void seedOwner(UserDAO dao, String username, String pass, String addr, String gender) {
        if (dao.findByUsername(username).isEmpty()) {
            dao.createUserInternalReturn(username, pass, "owner", addr, gender, null)
                    .ifPresentOrElse(
                            u -> System.out.println("OWNER " + username + " created (id=" + u.getUserId() + ", code=" + u.getUserCode() + ")"),
                            () -> System.out.println("Failed to create OWNER " + username)
                    );
        } else {
            System.out.println("OWNER " + username + " already exists.");
        }
    }

    private static void seedAdmin(UserDAO dao, String username, String pass, String addr, String gender) {
        if (dao.findByUsername(username).isEmpty()) {
            dao.createStaffReturn(username, pass, "admin", addr, gender, null)
                    .ifPresentOrElse(
                            u -> System.out.println("ADMIN " + username + " created (id=" + u.getUserId() + ", code=" + u.getUserCode() + ")"),
                            () -> System.out.println("Failed to create ADMIN " + username)
                    );
        } else {
            System.out.println("ADMIN " + username + " already exists.");
        }
    }

    private static void seedEmployee(UserDAO dao, String username, String pass, String workArea, String addr, String gender) {
        if (dao.findByUsername(username).isEmpty()) {
            dao.createStaffReturn(username, pass, "employee", addr, gender, workArea)
                    .ifPresentOrElse(
                            u -> System.out.println("EMPLOYEE " + username + " created (" + workArea + ", id=" + u.getUserId() + ", code=" + u.getUserCode() + ")"),
                            () -> System.out.println("Failed to create EMPLOYEE " + username)
                    );
        } else {
            System.out.println("EMPLOYEE " + username + " already exists.");
        }
    }
}

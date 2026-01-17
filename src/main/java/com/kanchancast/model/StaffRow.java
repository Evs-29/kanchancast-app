package com.kanchancast.model;

/**
 * Represents an employee (staff) record for Admin and Owner dashboards.
 * Includes basic profile info, work area, and performance stats.
 * Compatible with AdminTabs and analytics charts.
 */
public class StaffRow {

    private int userId;
    private String userName;
    private String workArea;
    private String gender;
    private String address;
    private int age;
    private int ordersDone;

    // --- Constructors ---
    public StaffRow() {}

    public StaffRow(int userId, String userName, String workArea,
                    String gender, String address, int age, int ordersDone) {
        this.userId = userId;
        this.userName = userName;
        this.workArea = workArea;
        this.gender = gender;
        this.address = address;
        this.age = age;
        this.ordersDone = ordersDone;
    }

    // --- Getters and Setters ---
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getWorkArea() {
        return workArea;
    }

    public void setWorkArea(String workArea) {
        this.workArea = workArea;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getOrdersDone() {
        return ordersDone;
    }

    public void setOrdersDone(int ordersDone) {
        this.ordersDone = ordersDone;
    }

    /**
     * Returns the staff member’s role or position for UI display.
     * In this system, workArea doubles as the "Role" field.
     */
    public String getRole() {
        return (workArea != null && !workArea.isBlank()) ? workArea : "Employee";
    }

    @Override
    public String toString() {
        return userName + " (" + getRole() + ")";
    }
}

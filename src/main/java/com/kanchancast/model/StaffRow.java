package com.kanchancast.model;

public class StaffRow {

    private int userId;
    private String userName;
    private String workArea;
    private String gender;
    private String address;
    private int age;
    private int ordersDone;

    // ✅ NEW
    private String dob;

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

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getWorkArea() { return workArea; }
    public void setWorkArea(String workArea) { this.workArea = workArea; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public int getOrdersDone() { return ordersDone; }
    public void setOrdersDone(int ordersDone) { this.ordersDone = ordersDone; }

    // ✅ NEW
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getRole() {
        return (workArea != null && !workArea.isBlank()) ? workArea : "Employee";
    }

    @Override
    public String toString() {
        return userName + " (" + getRole() + ")";
    }
}

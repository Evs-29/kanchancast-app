package com.kanchancast.model;

public class AssignedTask {
    private int orderId;
    private String stageName;
    private String productName;
    private String customerName;
    private boolean completed;

    // ----- Getters -----
    public int getOrderId() {
        return orderId;
    }

    public String getStage() {
        return stageName;
    }

    public String getProductName() {
        return productName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public boolean isCompleted() {
        return completed;
    }

    // ----- Setters -----
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public void setStage(String stageName) {
        this.stageName = stageName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // Optional helper for displaying as "Yes"/"No"
    public String getCompletedText() {
        return completed ? "Yes" : "No";
    }

    public void setCompletedText(String completedText) {
        this.completed = "Yes".equalsIgnoreCase(completedText);
    }
}

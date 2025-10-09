package com.kanchancast.model;

/**
 * OrderSummary
 * -------------
 * Represents an order record used in dashboards.
 * Contains product name, customer name, date, status, and progress percent.
 */
public class OrderSummary {

    private int orderId;
    private int userId;
    private int productId;
    private String customerName;
    private String productName;
    private String dateOrdered;
    private String status;
    private int progressPercent;

    // ---------- Getters ----------
    public int getOrderId() { return orderId; }
    public int getUserId() { return userId; }
    public int getProductId() { return productId; }
    public String getCustomerName() { return customerName; }
    public String getProductName() { return productName; }
    public String getDateOrdered() { return dateOrdered; }
    public String getStatus() { return status; }
    public int getProgressPercent() { return progressPercent; }

    // ---------- Setters ----------
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setDateOrdered(String dateOrdered) { this.dateOrdered = dateOrdered; }
    public void setStatus(String status) { this.status = status; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
}

package com.kanchancast.model;

public class Order {
    private int orderId;
    private String customerId;
    private int productId;
    private String paymentMethod;
    private String dateOrdered;
    private int progress;
    private String currentStage;

    public Order(int orderId, String customerId, int productId, String paymentMethod, String dateOrdered, int progress, String currentStage) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.paymentMethod = paymentMethod;
        this.dateOrdered = dateOrdered;
        this.progress = progress;
        this.currentStage = currentStage;
    }

    public int getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public int getProductId() { return productId; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getDateOrdered() { return dateOrdered; }
    public int getProgress() { return progress; }
    public String getCurrentStage() { return currentStage; }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    @Override
    public String toString() {
        return "Order #" + orderId + " - Stage: " + currentStage + " (" + progress + "%)";
    }
}

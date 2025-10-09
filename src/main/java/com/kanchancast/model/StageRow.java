package com.kanchancast.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * StageRow
 * ---------
 * Represents one stage in an order’s production flow.
 * Used in OrderTrackingDialog and OrderDetailsDialog.
 */
public class StageRow {

    private final StringProperty stage;
    private final StringProperty employeeName;
    private final StringProperty completedText;

    public StageRow() {
        this.stage = new SimpleStringProperty("");
        this.employeeName = new SimpleStringProperty("");
        this.completedText = new SimpleStringProperty("");
    }

    // ---------- Getters ----------
    public String getStage() {
        return stage.get();
    }

    public String getEmployeeName() {
        return employeeName.get();
    }

    public String getCompletedText() {
        return completedText.get();
    }

    // ---------- Setters ----------
    public void setStage(String value) {
        stage.set(value);
    }

    public void setEmployeeName(String value) {
        employeeName.set(value);
    }

    public void setCompletedText(String value) {
        completedText.set(value);
    }

    // ---------- JavaFX Properties ----------
    public StringProperty stageProperty() {
        return stage;
    }

    public StringProperty employeeNameProperty() {
        return employeeName;
    }

    public StringProperty completedTextProperty() {
        return completedText;
    }
}

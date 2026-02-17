package com.kanchancast.model;

public enum StageEnum {
    RAW_MATERIAL_PROCUREMENT("Raw Material Procurement"),
    DESIGN_CAD("Design & CAD Modelling"),
    WAX_MODEL("Wax Model Creation"),
    INVESTMENT_CASTING("Investment Casting"),
    CLEANING_DEVESTING("Cleaning & Devesting"),
    FILING_PREPOLISH("Filing & Pre-Polishing"),
    STONE_SETTING("Stone Setting"),
    FINAL_POLISHING("Final Polishing"),
    PLATING("Plating"),
    QUALITY_CONTROL("Quality Control"),
    PACKAGING_DISPATCH("Packaging & Dispatch");

    private final String label;

    StageEnum(String label) { this.label = label; }

    public String label() { return label; }

    public static String[] labels() {
        StageEnum[] values = values();
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) out[i] = values[i].label;
        return out;
    }
}

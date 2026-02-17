package com.kanchancast.model;

import java.util.Objects;

/**
 * Product domain model used by DAOs and dashboards.
 */
public class Product {

    private int id;                    // products.product_id
    private String name;               // products.name
    private String type;               // products.type
    private double price;              // products.price
    private Double goldWeight;         // products.karat
    private Double diamondWeight;      // products.weight
    private Double stoneWeight;        // ✅ products.stone_weight
    private String imagePath;          // products.image_path
    private String description;        // products.description

    // Duration fields (stored in products table)
    private int durationAmount;        // products.duration_amount
    private String durationUnit;       // products.duration_unit (DAYS/WEEKS/MONTHS)

    public Product() {}

    public Product(int id, String name, String type, double price,
                   Double goldWeight, Double diamondWeight, Double stoneWeight,
                   String imagePath, String description,
                   int durationAmount, String durationUnit) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.goldWeight = goldWeight;
        this.diamondWeight = diamondWeight;
        this.stoneWeight = stoneWeight;
        this.imagePath = imagePath;
        this.description = description;
        this.durationAmount = durationAmount;
        this.durationUnit = durationUnit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return id; }
    public void setProductId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategoryName() { return type; }

    public Double getGoldWeight() { return goldWeight; }
    public void setGoldWeight(Double goldWeight) { this.goldWeight = goldWeight; }

    public Double getDiamondWeight() { return diamondWeight; }
    public void setDiamondWeight(Double diamondWeight) { this.diamondWeight = diamondWeight; }

    public Double getStoneWeight() { return stoneWeight; }
    public void setStoneWeight(Double stoneWeight) { this.stoneWeight = stoneWeight; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDurationAmount() { return durationAmount; }
    public void setDurationAmount(int durationAmount) { this.durationAmount = durationAmount; }

    public String getDurationUnit() { return durationUnit; }
    public void setDurationUnit(String durationUnit) { this.durationUnit = durationUnit; }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", price=" + price +
                ", goldWeight=" + goldWeight +
                ", diamondWeight=" + diamondWeight +
                ", stoneWeight=" + stoneWeight +
                ", imagePath='" + imagePath + '\'' +
                ", description='" + description + '\'' +
                ", durationAmount=" + durationAmount +
                ", durationUnit='" + durationUnit + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

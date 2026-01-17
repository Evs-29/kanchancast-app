package com.kanchancast.model;

import java.util.Objects;

/**
 * Product domain model used by DAOs and dashboards.
 * - price is kept as double (DB stores DECIMAL; DAO converts).
 * - gold/diamond/stone weights are nullable Doubles.
 * - includes description, imagePath, and id with both getId()/setId() and getProductId()/setProductId().
 */
public class Product {

    // ----- fields -----
    private int id;                    // maps to products.product_id
    private String name;               // products.product_name
    private String type;               // products.product_type (aka category)
    private double price;              // products.price (DECIMAL in DB)
    private Double goldWeight;         // products.gold_weight  (nullable)
    private Double diamondWeight;      // products.diamond_weight (nullable)
    private Double stoneWeight;        // products.stone_weight (nullable)
    private String imagePath;          // products.image_path
    private String description;        // products.description

    // ----- constructors -----
    public Product() {}

    public Product(int id, String name, String type, double price,
                   Double goldWeight, Double diamondWeight, Double stoneWeight,
                   String imagePath, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.goldWeight = goldWeight;
        this.diamondWeight = diamondWeight;
        this.stoneWeight = stoneWeight;
        this.imagePath = imagePath;
        this.description = description;
    }

    // ----- id (aliases provided for convenience) -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Some parts of the code may call these:
    public int getProductId() { return id; }
    public void setProductId(int id) { this.id = id; }

    // ----- basic props -----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    // ----- optional alias for category -----
    public String getCategoryName() {
        return type; // alias for compatibility with dashboards
    }

    // ----- weights (nullable) -----
    public Double getGoldWeight() { return goldWeight; }
    public void setGoldWeight(Double goldWeight) { this.goldWeight = goldWeight; }

    public Double getDiamondWeight() { return diamondWeight; }
    public void setDiamondWeight(Double diamondWeight) { this.diamondWeight = diamondWeight; }

    public Double getStoneWeight() { return stoneWeight; }
    public void setStoneWeight(Double stoneWeight) { this.stoneWeight = stoneWeight; }

    // ----- media / description -----
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ----- util -----
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

package com.example.projectakhirbismillah.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Product implements Serializable {
    // Tambahkan serialVersionUID untuk versioning
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String description;
    private double price;
    private double discountPercentage;
    private double rating;
    private int stock;
    private String brand;
    private String category;
    private String thumbnail;
    private List<String> images;

    // Constructor default (jika diperlukan)
    public Product() {
        // Default constructor
    }

    // Copy constructor
    public Product(Product other) {
        this.id = other.id;
        this.title = other.title;
        this.description = other.description;
        this.price = other.price;
        this.discountPercentage = other.discountPercentage;
        this.rating = other.rating;
        this.stock = other.stock;
        this.brand = other.brand;
        this.category = other.category;
        this.thumbnail = other.thumbnail;
        this.images = other.images;
    }

    public Product(int id, String title, String description, double price, double discountPercentage, double rating, int stock, String brand, String category, String thumbnail, String image) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.discountPercentage = discountPercentage;
        this.rating = rating;
        this.stock = stock;
        this.brand = brand;
        this.category = category;
        this.thumbnail = thumbnail;
        // Set images jika diperlukan
    }

    // TAMBAHAN: Override equals dan hashCode untuk membandingkan objek Product
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return getId() == product.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    // Helper method untuk mengatasi error di ProductDetailActivity
    public String getImage() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return thumbnail;
    }

    // Semua getter dan setter yang sudah ada
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
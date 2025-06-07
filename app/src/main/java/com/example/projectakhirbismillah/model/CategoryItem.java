package com.example.projectakhirbismillah.model;

public class CategoryItem {
    private String id;
    private String name;
    private int iconResource;
    private boolean selected;

    public CategoryItem(String id, String name, int iconResource) {
        this.id = id;
        this.name = name;
        this.iconResource = iconResource;
        this.selected = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getIconResource() {
        return iconResource;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
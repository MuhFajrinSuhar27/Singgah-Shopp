package com.example.projectakhirbismillah.util;

import com.example.projectakhirbismillah.model.Product;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteManager {
    private static FavoriteManager instance;
    private final Map<Integer, Product> favoriteProducts;
    private final List<FavoriteChangeListener> listeners;

    public interface FavoriteChangeListener {
        void onFavoriteChanged();
    }

    private FavoriteManager() {
        favoriteProducts = new HashMap<>();
        listeners = new ArrayList<>();
    }

    public static synchronized FavoriteManager getInstance() {
        if (instance == null) {
            instance = new FavoriteManager();
        }
        return instance;
    }

    public void toggleFavorite(Product product) {
        if (product == null || product.getId() == 0) return;

        if (favoriteProducts.containsKey(product.getId())) {
            favoriteProducts.remove(product.getId());
        } else {
            favoriteProducts.put(product.getId(), product);
        }

        // Notify listeners
        notifyListeners();
    }

    public boolean isFavorite(int productId) {
        return favoriteProducts.containsKey(productId);
    }

    public List<Product> getFavoriteProducts() {
        return new ArrayList<>(favoriteProducts.values());
    }

    public int getFavoriteCount() {
        return favoriteProducts.size();
    }

    public void addListener(FavoriteChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(FavoriteChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (FavoriteChangeListener listener : listeners) {
            listener.onFavoriteChanged();
        }
    }
}
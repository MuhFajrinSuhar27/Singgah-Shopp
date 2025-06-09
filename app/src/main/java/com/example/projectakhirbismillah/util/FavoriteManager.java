package com.example.projectakhirbismillah.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.projectakhirbismillah.model.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoriteManager {
    private static final String TAG = "FavoriteManager";
    private static FavoriteManager instance;
    private List<Product> favoriteProducts;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "favorite_prefs";
    private static final String FAVORITES_KEY = "favorites";
    private List<FavoriteChangeListener> listeners = new ArrayList<>();
    private boolean isInitialized = false;

    public interface FavoriteChangeListener {
        void onFavoriteChanged();
    }

    private FavoriteManager() {
        favoriteProducts = new ArrayList<>();
    }

    public static synchronized FavoriteManager getInstance() {
        if (instance == null) {
            instance = new FavoriteManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "Context is null in initialize()");
                return;
            }

            if (sharedPreferences == null) {
                sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                isInitialized = true;
            }

            // SELALU reload favorites untuk memastikan data terbaru
            loadFavorites();
            Log.d(TAG, "FavoriteManager (re)initialized with " + favoriteProducts.size() + " products");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FavoriteManager: " + e.getMessage(), e);
        }
    }

    public void addListener(FavoriteChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Listener added, count: " + listeners.size());
        }
    }

    public void removeListener(FavoriteChangeListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "Listener removed, count: " + listeners.size());
    }

    private void notifyListeners() {
        Log.d(TAG, "Notifying " + listeners.size() + " listeners about changes");
        for (FavoriteChangeListener listener : listeners) {
            try {
                listener.onFavoriteChanged();
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener: " + e.getMessage(), e);
            }
        }
    }

    public List<Product> getFavoriteProducts() {
        return favoriteProducts;
    }

    public void addToFavorites(Product product) {
        try {
            if (!isFavorite(product)) {
                // Log sebelum menambahkan
                Log.d(TAG, "Trying to add product to favorites: " + product.getTitle() + " (ID: " + product.getId() + ")");

                // Deep copy product untuk menghindari reference issues
                Product productCopy = new Product(
                        product.getId(),
                        product.getTitle(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getDiscountPercentage(),
                        product.getRating(),
                        product.getStock(),
                        product.getBrand(),
                        product.getCategory(),
                        product.getThumbnail(),
                        product.getImage()
                );

                favoriteProducts.add(productCopy);

                // Simpan dan notifikasi
                saveFavorites();
                notifyListeners();

                Log.d(TAG, "Product added to favorites: " + product.getTitle() + ", total favorites: " + favoriteProducts.size());

                // Validasi apakah berhasil ditambahkan
                if (isFavorite(product)) {
                    Log.d(TAG, "Verified product is now in favorites");
                } else {
                    Log.e(TAG, "Failed to add product to favorites! Not found after add operation.");
                }
            } else {
                Log.d(TAG, "Product already in favorites: " + product.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding product to favorites: " + e.getMessage(), e);
        }
    }

    public void removeFromFavorites(Product product) {
        try {
            Product toRemove = null;
            for (Product p : favoriteProducts) {
                if (p.getId() == product.getId()) {
                    toRemove = p;
                    break;
                }
            }

            if (toRemove != null) {
                favoriteProducts.remove(toRemove);
                saveFavorites();
                notifyListeners();
                Log.d(TAG, "Product removed from favorites: " + product.getTitle());
            } else {
                Log.w(TAG, "Product not found in favorites: " + product.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing product from favorites: " + e.getMessage(), e);
        }
    }

   public boolean isFavorite(Product product) {
    if (product == null) return false;
    
    for (Product p : favoriteProducts) {
        // Gunakan equals() yang sudah dioverride di model Product
        if (p != null && p.getId() == product.getId()) {
            return true;
        }
    }
    return false;
}

    private void saveFavorites() {
        try {
            if (sharedPreferences == null) {
                Log.e(TAG, "Cannot save favorites: SharedPreferences not initialized");
                return;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            Gson gson = new Gson();
            String json = gson.toJson(favoriteProducts);

            // PENTING: Gunakan commit() bukan apply() untuk memastikan perubahan langsung tersimpan
            boolean success = editor.putString(FAVORITES_KEY, json).commit();

            if (success) {
                Log.d(TAG, "Favorites saved successfully: " + favoriteProducts.size() + " products");
            } else {
                Log.e(TAG, "Failed to save favorites!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving favorites: " + e.getMessage(), e);
        }
    }

    private void loadFavorites() {
        try {
            if (sharedPreferences == null) {
                Log.e(TAG, "Cannot load favorites: SharedPreferences not initialized");
                return;
            }

            Gson gson = new Gson();
            String json = sharedPreferences.getString(FAVORITES_KEY, null);

            if (json != null && !json.isEmpty()) {
                Type type = new TypeToken<ArrayList<Product>>() {}.getType();
                List<Product> loadedFavorites = gson.fromJson(json, type);

                favoriteProducts.clear();

                if (loadedFavorites != null) {
                    favoriteProducts.addAll(loadedFavorites);
                    Log.d(TAG, "Loaded " + favoriteProducts.size() + " favorite products");

                    // Debugging: cetak semua produk yang dimuat
                    if (!favoriteProducts.isEmpty()) {
                        for (Product p : favoriteProducts) {
                            Log.d(TAG, "Loaded favorite product: ID=" + p.getId() + ", Title=" + p.getTitle());
                        }
                    }
                } else {
                    Log.w(TAG, "Loaded favorites list is null");
                }
            } else {
                Log.d(TAG, "No saved favorites found in SharedPreferences");
                favoriteProducts.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites: " + e.getMessage(), e);
            // Bersihkan list jika terjadi error saat loading
            favoriteProducts.clear();
        }
    }

    
    public void printFavoritesStatus() {
        Log.d(TAG, "=== FAVORITES STATUS ===");
        Log.d(TAG, "Total favorites: " + favoriteProducts.size());
        Log.d(TAG, "SharedPreferences initialized: " + (sharedPreferences != null));
        Log.d(TAG, "Total listeners: " + listeners.size());

        if (!favoriteProducts.isEmpty()) {
            for (Product p : favoriteProducts) {
                Log.d(TAG, "Product: ID=" + p.getId() + ", Title=" + p.getTitle());
            }
        }
        Log.d(TAG, "======================");
    }
}
package com.example.projectakhirbismillah.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.model.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static final String TAG = "CartManager";
    private static final String PREFS_NAME = "CartPrefs";
    private static final String CART_ITEMS_KEY = "cart_items";

    private static CartManager instance;
    private final Map<Integer, CartItem> cartItems;
    private final List<CartChangeListener> listeners;

    public interface CartChangeListener {
        void onCartChanged();
    }

    private CartManager() {
        cartItems = new HashMap<>();
        listeners = new ArrayList<>();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addToCart(Product product) {
        if (product == null) {
            Log.e(TAG, "Cannot add null product to cart");
            return;
        }

        int productId = product.getId();
        if (cartItems.containsKey(productId)) {
            // Product already in cart, increase quantity
            cartItems.get(productId).incrementQuantity();
        } else {
            // Add new product to cart with quantity 1
            cartItems.put(productId, new CartItem(product, 1));
        }
        notifyListeners();
    }

    public void removeFromCart(int productId) {
        cartItems.remove(productId);
        notifyListeners();
    }

    public void updateQuantity(int productId, int quantity) {
        if (cartItems.containsKey(productId)) {
            if (quantity <= 0) {
                cartItems.remove(productId);
            } else {
                cartItems.get(productId).setQuantity(quantity);
            }
            notifyListeners();
        }
    }

    public void incrementQuantity(int productId) {
        if (cartItems.containsKey(productId)) {
            cartItems.get(productId).incrementQuantity();
            notifyListeners();
        }
    }

    public void decrementQuantity(int productId) {
        if (cartItems.containsKey(productId)) {
            CartItem item = cartItems.get(productId);
            if (item.getQuantity() > 1) {
                item.decrementQuantity();
            } else {
                cartItems.remove(productId);
            }
            notifyListeners();
        }
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    public int getItemCount() {
        return cartItems.size();
    }

    public int getTotalQuantity() {
        int total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getQuantity();
        }
        return total;
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public void clearCart() {
        cartItems.clear();
        notifyListeners();
    }

    public void addListener(CartChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CartChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (CartChangeListener listener : listeners) {
            listener.onCartChanged();
        }
    }

    // Save cart to SharedPreferences
    public void saveCartToPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            Gson gson = new Gson();
            List<CartItem> itemsList = new ArrayList<>(cartItems.values());
            String json = gson.toJson(itemsList);
            editor.putString(CART_ITEMS_KEY, json);
            editor.apply();
            Log.d(TAG, "Cart saved to preferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving cart to preferences: " + e.getMessage());
        }
    }

    // Load cart from SharedPreferences
    public void loadCartFromPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(CART_ITEMS_KEY, null);
            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<CartItem>>() {}.getType();
                List<CartItem> itemsList = gson.fromJson(json, type);

                cartItems.clear();
                if (itemsList != null) {
                    for (CartItem item : itemsList) {
                        cartItems.put(item.getProduct().getId(), item);
                    }
                }
                notifyListeners();
                Log.d(TAG, "Cart loaded from preferences: " + cartItems.size() + " items");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cart from preferences: " + e.getMessage());
        }
    }
}
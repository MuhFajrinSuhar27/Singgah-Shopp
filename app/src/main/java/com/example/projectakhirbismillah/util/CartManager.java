package com.example.projectakhirbismillah.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.model.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;
    private List<CartChangeListener> listeners = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "cart_prefs";
    private static final String CART_ITEMS_KEY = "cart_items";

    // Private constructor untuk Singleton pattern
    private CartManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadCart();
    }

    // Singleton instance
    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context.getApplicationContext());
        }
        return instance;
    }

    // Mendapatkan singleton instance tanpa context (gunakan setelah inisialisasi pertama)
    public static synchronized CartManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CartManager must be initialized with context first");
        }
        return instance;
    }

    // Interface untuk listener perubahan cart
    public interface CartChangeListener {
        void onCartChanged();
    }

    // Menambahkan listener
    public void addListener(CartChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // Menghapus listener
    public void removeListener(CartChangeListener listener) {
        listeners.remove(listener);
    }

    // Notifikasi ke semua listener
    private void notifyCartChanged() {
        for (CartChangeListener listener : listeners) {
            listener.onCartChanged();
        }
    }

    // Mendapatkan semua item di cart
    public List<CartItem> getCartItems() {
        return cartItems;
    }

    // Menambahkan produk ke cart
    public void addToCart(Product product, int quantity) {
        // Cek jika produk sudah ada di cart
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == product.getId()) {
                // Update quantity jika produk sudah ada
                item.setQuantity(item.getQuantity() + quantity);
                saveCart();
                notifyCartChanged();
                return;
            }
        }

        // Tambahkan sebagai item baru jika belum ada
        cartItems.add(new CartItem(product, quantity));
        saveCart();
        notifyCartChanged();
    }

    // Mengubah kuantitas item di cart
    public void updateItemQuantity(Product product, int newQuantity) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == product.getId()) {
                item.setQuantity(newQuantity);
                saveCart();
                notifyCartChanged();
                return;
            }
        }
    }

    // Menghapus item dari cart
    public void removeFromCart(Product product) {
        CartItem itemToRemove = null;
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == product.getId()) {
                itemToRemove = item;
                break;
            }
        }

        if (itemToRemove != null) {
            cartItems.remove(itemToRemove);
            saveCart();
            notifyCartChanged();
        }
    }

    // Membersihkan semua item di cart
    public void clearCart() {
        cartItems.clear();
        saveCart();
        notifyCartChanged();
    }

    // Mendapatkan total harga semua item di cart
    public double getTotalPrice() {
        double totalPrice = 0;
        for (CartItem item : cartItems) {
            totalPrice += item.getProduct().getPrice() * item.getQuantity();
        }
        return totalPrice;
    }

    // Mendapatkan jumlah item di cart
    public int getCartSize() {
        int size = 0;
        for (CartItem item : cartItems) {
            size += item.getQuantity();
        }
        return size;
    }

    // Menyimpan cart ke SharedPreferences
    private void saveCart() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(cartItems);
        editor.putString(CART_ITEMS_KEY, json);
        editor.apply();
    }

    // Memuat cart dari SharedPreferences
    private void loadCart() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(CART_ITEMS_KEY, null);
        Type type = new TypeToken<ArrayList<CartItem>>() {}.getType();

        if (json != null) {
            cartItems = gson.fromJson(json, type);
        } else {
            cartItems = new ArrayList<>();
        }
    }
}
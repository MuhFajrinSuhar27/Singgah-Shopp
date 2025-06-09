package com.example.projectakhirbismillah.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.CurrencyUtil;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private static final String TAG = "CartAdapter";
    private final Context context;
    private final List<CartItem> cartItems;
    private final CartManager cartManager;
    private OnCartItemChangedListener listener;

    public interface OnCartItemChangedListener {
        void onQuantityChanged();
        void onItemRemoved();
    }

    public void setOnCartItemChangedListener(OnCartItemChangedListener listener) {
        this.listener = listener;
    }

    public CartAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
        this.cartManager = CartManager.getInstance(context);
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        // Validasi posisi untuk keamanan
        if (position >= cartItems.size()) {
            Log.e(TAG, "Invalid position: " + position + ", size: " + cartItems.size());
            return;
        }
        
        CartItem cartItem = cartItems.get(position);
        Product product = cartItem.getProduct();
        
        // Set product name
        if (holder.titleTextView != null) {
            holder.titleTextView.setText(product.getTitle());
        }
        
        // Format price in Rupiah
        if (holder.priceTextView != null) {
            holder.priceTextView.setText(CurrencyUtil.formatToRupiah(product.getPrice()));
        }
        
        // Set quantity
        if (holder.quantityTextView != null) {
            holder.quantityTextView.setText(String.valueOf(cartItem.getQuantity()));
        }
        
        // Calculate and set total price
        if (holder.totalPriceTextView != null) {
            double totalPrice = product.getPrice() * cartItem.getQuantity();
            holder.totalPriceTextView.setText("Total: " + CurrencyUtil.formatToRupiah(totalPrice));
        }
        
        // Load product image
        if (holder.productImageView != null) {
            Glide.with(context)
                    .load(product.getThumbnail() != null ? product.getThumbnail() : product.getImage())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.productImageView);
        }
        
        // Set increment button click listener
        if (holder.increaseButton != null) {
            holder.increaseButton.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;
                
                int currentQuantity = cartItem.getQuantity();
                cartItem.setQuantity(currentQuantity + 1);
                cartManager.updateItemQuantity(product, currentQuantity + 1);
                
                if (holder.quantityTextView != null) {
                    holder.quantityTextView.setText(String.valueOf(cartItem.getQuantity()));
                }
                
                // Calculate and set new total price
                if (holder.totalPriceTextView != null) {
                    double newTotalPrice = product.getPrice() * cartItem.getQuantity();
                    holder.totalPriceTextView.setText("Total: " + CurrencyUtil.formatToRupiah(newTotalPrice));
                }
                
                // Notify listener
                if (listener != null) {
                    listener.onQuantityChanged();
                }
            });
        }
        
        // Set decrement button click listener
        if (holder.decreaseButton != null) {
            holder.decreaseButton.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;
                
                int currentQuantity = cartItem.getQuantity();
                if (currentQuantity > 1) {
                    cartItem.setQuantity(currentQuantity - 1);
                    cartManager.updateItemQuantity(product, currentQuantity - 1);
                    
                    if (holder.quantityTextView != null) {
                        holder.quantityTextView.setText(String.valueOf(cartItem.getQuantity()));
                    }
                    
                    // Calculate and set new total price
                    if (holder.totalPriceTextView != null) {
                        double newTotalPrice = product.getPrice() * cartItem.getQuantity();
                        holder.totalPriceTextView.setText("Total: " + CurrencyUtil.formatToRupiah(newTotalPrice));
                    }
                    
                    // Notify listener
                    if (listener != null) {
                        listener.onQuantityChanged();
                    }
                }
            });
        }
        
        // Set remove button click listener - PERBAIKAN DI SINI
    if (holder.removeButton != null) {
    holder.removeButton.setOnClickListener(v -> {
        try {
            // Get current position again to avoid stale references
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                Log.d(TAG, "Cannot remove item: no position available");
                return;
            }
            
            // TAMBAHAN PENGECEKAN BARU: pastikan cartItems tidak null dan adapterPosition valid
            if (cartItems == null || adapterPosition >= cartItems.size()) {
                Log.e(TAG, "Cannot remove item: invalid state - cartItems is null or position invalid");
                return;
            }
            
            // Ambil produk sebelum menghapus dari list
            Product productToRemove = cartItems.get(adapterPosition).getProduct();
            
            // Hapus dari list lokal dulu
            cartItems.remove(adapterPosition);
            
            // Beri tahu adapter (ini akan memperbaharui UI)
            notifyItemRemoved(adapterPosition);
            
            // Baru hapus dari CartManager (ini menghindari race condition)
            cartManager.removeFromCart(productToRemove);
            
            // Beri tahu listener
            if (listener != null) {
                listener.onItemRemoved();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing item from cart: " + e.getMessage(), e);
        }
    });
}
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView titleTextView;
        TextView priceTextView;
        TextView quantityTextView;
        TextView totalPriceTextView;
        ImageButton increaseButton;
        ImageButton decreaseButton;
        ImageButton removeButton;
        
        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            removeButton = itemView.findViewById(R.id.btn_remove);
            titleTextView = itemView.findViewById(R.id.cart_item_name);
            priceTextView = itemView.findViewById(R.id.cart_item_price);
            increaseButton = itemView.findViewById(R.id.btn_increase);
            decreaseButton = itemView.findViewById(R.id.btn_decrease);
            quantityTextView = itemView.findViewById(R.id.cart_item_quantity);
            totalPriceTextView = itemView.findViewById(R.id.cart_total_price);
            productImageView = itemView.findViewById(R.id.cart_item_image);

        }
    }
}
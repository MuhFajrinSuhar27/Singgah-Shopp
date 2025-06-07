package com.example.projectakhirbismillah.adapter;

import android.content.Context;
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
import com.example.projectakhirbismillah.util.CartManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> cartItems;
    private final Context context;
    private final CartManager cartManager;
    private final NumberFormat currencyFormatter;

    public CartAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = new ArrayList<>(cartItems);
        this.cartManager = CartManager.getInstance();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);

        // Set product details
        holder.productName.setText(cartItem.getProduct().getTitle());
        String brand = cartItem.getProduct().getBrand();
        if (brand != null && !brand.isEmpty()) {
            holder.productBrand.setVisibility(View.VISIBLE);
            holder.productBrand.setText(brand);
        } else {
            holder.productBrand.setVisibility(View.GONE);
        }

        // Set price
        String formattedPrice = currencyFormatter.format(cartItem.getProduct().getPrice());
        holder.productPrice.setText(formattedPrice);

        // Load product image
        if (cartItem.getProduct().getThumbnail() != null && !cartItem.getProduct().getThumbnail().isEmpty()) {
            Glide.with(context)
                    .load(cartItem.getProduct().getThumbnail())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set quantity
        holder.quantityText.setText(String.valueOf(cartItem.getQuantity()));

        // Set quantity control buttons
        holder.decrementButton.setOnClickListener(v -> {
            cartManager.decrementQuantity(cartItem.getProduct().getId());
        });

        holder.incrementButton.setOnClickListener(v -> {
            cartManager.incrementQuantity(cartItem.getProduct().getId());
        });

        // Set item removal button
        holder.removeButton.setOnClickListener(v -> {
            cartManager.removeFromCart(cartItem.getProduct().getId());
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public void updateCart(List<CartItem> newCartItems) {
        this.cartItems.clear();
        if (newCartItems != null) {
            this.cartItems.addAll(newCartItems);
        }
        notifyDataSetChanged();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productBrand, productPrice, quantityText;
        ImageButton decrementButton, incrementButton, removeButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.cart_item_image);
            productName = itemView.findViewById(R.id.cart_item_name);
            productBrand = itemView.findViewById(R.id.cart_item_brand);
            productPrice = itemView.findViewById(R.id.cart_item_price);
            quantityText = itemView.findViewById(R.id.cart_item_quantity);
            decrementButton = itemView.findViewById(R.id.cart_item_decrement);
            incrementButton = itemView.findViewById(R.id.cart_item_increment);
            removeButton = itemView.findViewById(R.id.cart_item_remove);
        }
    }
}
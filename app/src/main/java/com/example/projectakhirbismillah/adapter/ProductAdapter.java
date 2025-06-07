package com.example.projectakhirbismillah.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.FavoriteManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> products;
    private final Context context;
    private final boolean isGridMode;
    private final FavoriteManager favoriteManager;
    private final CartManager cartManager;

    public ProductAdapter(Context context, List<Product> products, boolean isGridMode) {
        this.context = context;
        this.products = products != null ? products : new ArrayList<>();
        this.isGridMode = isGridMode;
        this.favoriteManager = FavoriteManager.getInstance();
        this.cartManager = CartManager.getInstance();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isGridMode) {
            view = LayoutInflater.from(context).inflate(R.layout.item_product_grid, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_product_list, parent, false);
        }
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);

        // Set basic info - with null checks
        if (holder.textTitle != null) {
            holder.textTitle.setText(product.getTitle());
        }

        if (holder.textBrand != null) {
            holder.textBrand.setText(product.getBrand());
        }

        // Format price and display
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        String formattedPrice = currencyFormatter.format(product.getPrice());
        if (holder.textPrice != null) {
            holder.textPrice.setText(formattedPrice);
        }

        
        if (product.getDiscountPercentage() > 0) {
            // Check if textDiscount exists in this layout
            if (holder.textDiscount != null) {
                holder.textDiscount.setVisibility(View.VISIBLE);
                holder.textDiscount.setText("-" + Math.round(product.getDiscountPercentage()) + "%");
            }

            // Check if textOriginalPrice exists in this layout
            if (holder.textOriginalPrice != null) {
                // Calculate original price
                double originalPrice = product.getPrice() * 100 / (100 - product.getDiscountPercentage());
                String formattedOriginalPrice = currencyFormatter.format(originalPrice);
                holder.textOriginalPrice.setVisibility(View.VISIBLE);
                holder.textOriginalPrice.setText(formattedOriginalPrice);
                holder.textOriginalPrice.setPaintFlags(holder.textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        } else {
            // Check nulls before setting visibility
            if (holder.textDiscount != null) {
                holder.textDiscount.setVisibility(View.GONE);
            }
            if (holder.textOriginalPrice != null) {
                holder.textOriginalPrice.setVisibility(View.GONE);
            }
        }

        // Load product image using Glide
        if (holder.imageProduct != null) {
            if (product.getThumbnail() != null && !product.getThumbnail().isEmpty()) {
                Glide.with(context)
                        .load(product.getThumbnail())
                        .placeholder(R.drawable.placeholder_image)
                        .into(holder.imageProduct);
            } else {
                holder.imageProduct.setImageResource(R.drawable.placeholder_image);
            }
        }

        // Set initial favorite state
        if (holder.buttonFavorite != null && favoriteManager != null) {
            boolean isFavorite = favoriteManager.isFavorite(product.getId());
            holder.buttonFavorite.setImageResource(isFavorite ?
                    R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            // Setup favorite button click
            holder.buttonFavorite.setOnClickListener(v -> {
                // Toggle favorite status in manager
                favoriteManager.toggleFavorite(product);

                // Update UI to reflect new state
                boolean isNowFavorite = favoriteManager.isFavorite(product.getId());
                holder.buttonFavorite.setImageResource(isNowFavorite ?
                        R.drawable.ic_favorite : R.drawable.ic_favorite_border);

                Toast.makeText(context,
                        isNowFavorite ? "Added to favorites" : "Removed from favorites",
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Add to cart button click - with null check
        if (holder.buttonAdd != null) {
            holder.buttonAdd.setOnClickListener(v -> {
                // Add to CartManager
                cartManager.addToCart(product);
                Toast.makeText(context, "Added to cart: " + product.getTitle(),
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView textTitle, textBrand, textPrice, textDiscount;
        TextView textOriginalPrice; // Might be null in grid layout
        ImageButton buttonFavorite;
        View buttonAdd; // Generic View type to handle both Button and ImageButton

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.image_product);
            textTitle = itemView.findViewById(R.id.text_title);
            textBrand = itemView.findViewById(R.id.text_brand);
            textPrice = itemView.findViewById(R.id.text_price);
            textDiscount = itemView.findViewById(R.id.text_discount);
            textOriginalPrice = itemView.findViewById(R.id.text_original_price); // Might be null
            buttonFavorite = itemView.findViewById(R.id.button_favorite);
            buttonAdd = itemView.findViewById(R.id.button_add);
        }
    }

    public void updateProducts(List<Product> newProducts) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }
}
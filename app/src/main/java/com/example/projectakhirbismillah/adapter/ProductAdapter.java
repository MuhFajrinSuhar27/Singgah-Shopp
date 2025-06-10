package com.example.projectakhirbismillah.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.activity.ProductDetailActivity;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.CurrencyUtil;
import com.example.projectakhirbismillah.util.FavoriteManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ProductAdapter";

    // Constants for view types
    public static final int VIEW_TYPE_GRID = 0;
    public static final int VIEW_TYPE_LIST = 1;
    public static final int VIEW_TYPE_CARD = 2;

    private Context context;
    private List<Product> products;
    private FavoriteManager favoriteManager;
    private CartManager cartManager;
    private int viewType;

    public ProductAdapter(Context context, List<Product> products, int viewType) {
        this.context = context;
        this.products = products;
        this.viewType = viewType;
        this.favoriteManager = FavoriteManager.getInstance();
        this.cartManager = CartManager.getInstance(context);
    }

    public ProductAdapter(Context context, List<Product> products) {
        this(context, products, VIEW_TYPE_CARD); // Default to card view
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (this.viewType == VIEW_TYPE_GRID) {
            View view = inflater.inflate(R.layout.item_product_grid, parent, false);
            return new GridViewHolder(view);
        } else if (this.viewType == VIEW_TYPE_LIST) {
            View view = inflater.inflate(R.layout.item_product_list, parent, false);
            return new ListViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_product, parent, false);
            return new CardViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Product product = products.get(position);

        if (holder instanceof GridViewHolder) {
            bindGridViewHolder((GridViewHolder) holder, product, position);
        } else if (holder instanceof ListViewHolder) {
            bindListViewHolder((ListViewHolder) holder, product, position);
        } else if (holder instanceof CardViewHolder) {
            bindCardViewHolder((CardViewHolder) holder, product, position);
        }
    }

    private void bindGridViewHolder(GridViewHolder holder, Product product, int position) {
        // Set product details
        holder.titleTextView.setText(product.getTitle());
        holder.brandTextView.setText(product.getBrand());
        holder.priceTextView.setText(CurrencyUtil.formatToRupiah(product.getPrice()));

        // Set discount badge if applicable
        if (product.getDiscountPercentage() > 0) {
            holder.discountTextView.setVisibility(View.VISIBLE);
            holder.discountTextView.setText("-" + Math.round(product.getDiscountPercentage()) + "%");
        } else {
            holder.discountTextView.setVisibility(View.GONE);
        }

        // Load product image
        Glide.with(context)
                .load(product.getThumbnail())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.productImageView);

        // Set item click listener to open product detail
        holder.itemView.setOnClickListener(v -> openProductDetail(product));

        // Set click listener for add to cart button
        holder.addToCartButton.setOnClickListener(v -> addToCart(product));

        // Update favorite button state and set listener
        updateFavoriteButton(holder.favoriteButton, product);
        holder.favoriteButton.setOnClickListener(v -> toggleFavorite(holder.favoriteButton, product));
    }

    private void bindListViewHolder(ListViewHolder holder, Product product, int position) {
        // Similar to grid holder but with different IDs
        holder.titleTextView.setText(product.getTitle());
        holder.brandTextView.setText(product.getBrand());
        holder.priceTextView.setText(CurrencyUtil.formatToRupiah(product.getPrice()));

        // Discount handling
        if (product.getDiscountPercentage() > 0) {
            holder.discountTextView.setVisibility(View.VISIBLE);
            holder.discountTextView.setText("-" + Math.round(product.getDiscountPercentage()) + "%");
        } else {
            holder.discountTextView.setVisibility(View.GONE);
        }

        // Load product image
        Glide.with(context)
                .load(product.getThumbnail())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.productImageView);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> openProductDetail(product));
        holder.addButton.setOnClickListener(v -> addToCart(product));

        // Update favorite button state and set listener
        updateFavoriteButton(holder.favoriteButton, product);
        holder.favoriteButton.setOnClickListener(v -> toggleFavorite(holder.favoriteButton, product));
    }

    private void bindCardViewHolder(CardViewHolder holder, Product product, int position) {
        // Bind data to card view layout (item_product.xml)
        holder.titleTextView.setText(product.getTitle());
        holder.priceTextView.setText(CurrencyUtil.formatToRupiah(product.getPrice()));

        // Set rating if available
        if (holder.ratingBar != null) {
            holder.ratingBar.setRating((float) product.getRating());
        }
        if (holder.ratingTextView != null) {
            holder.ratingTextView.setText(String.format("%.1f", product.getRating()));
        }

        // Load product image
        Glide.with(context)
                .load(product.getThumbnail())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.productImageView);

        // Click listeners
        holder.itemView.setOnClickListener(v -> openProductDetail(product));
        if (holder.addToCartButton != null) {
            holder.addToCartButton.setOnClickListener(v -> addToCart(product));
        }

        // Update favorite button state
        updateFavoriteButton(holder.favoriteButton, product);
        holder.favoriteButton.setOnClickListener(v -> toggleFavorite(holder.favoriteButton, product));
    }

    // Helper methods
    private void openProductDetail(Product product) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra("product", product);
        context.startActivity(intent);
    }

    private void addToCart(Product product) {
        cartManager.addToCart(product, 1);
        Toast.makeText(context, product.getTitle() + " added to cart", Toast.LENGTH_SHORT).show();
    }

    // METODE YANG DIPERBAIKI
    private void toggleFavorite(ImageButton favoriteButton, Product product) {
        try {
            Log.d(TAG, "Toggle favorite for: " + product.getTitle() + " (ID: " + product.getId() + ")");

            boolean currentState = favoriteManager.isFavorite(product);
            Log.d(TAG, "Current favorite state: " + currentState);

            if (currentState) {
                favoriteManager.removeFromFavorites(product);
            } else {
                favoriteManager.addToFavorites(product);
            }

            // Verify state changed
            boolean newState = favoriteManager.isFavorite(product);
            Log.d(TAG, "New favorite state: " + newState + " (was: " + currentState + ")");

            // Show toast feedback
            if (newState) {
                Toast.makeText(context, product.getTitle() + " added to favorites", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, product.getTitle() + " removed from favorites", Toast.LENGTH_SHORT).show();
            }

            // Update button appearance
            updateFavoriteButton(favoriteButton, product);
        } catch (Exception e) {
            Log.e(TAG, "Error toggling favorite: " + e.getMessage(), e);
        }
    }

    private void updateFavoriteButton(ImageButton favoriteButton, Product product) {
        try {
            boolean isFavorite = favoriteManager.isFavorite(product);
            Log.d(TAG, "Checking favorite status for product ID " + product.getId() + ": " + isFavorite);

            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite);
                favoriteButton.setColorFilter(context.getResources().getColor(R.color.favorite_red));
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                favoriteButton.setColorFilter(context.getResources().getColor(R.color.favorite_icon_tint));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating favorite button: " + e.getMessage(), e);
            // Default to unfavorited state on error
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        // TAMBAHKAN DEBUG LOG
        Log.d("ProductAdapter", "Updating products: " + newProducts.size() + " items");

        // PENTING: Gunakan REFERENSI YANG SAMA dari List products
        this.products.clear();

        if (newProducts != null) {
            this.products.addAll(newProducts);
        }

        // NOTIFIKASI DENGAN TEPAT
        notifyDataSetChanged();

        // TAMBAHKAN LOG KONFIRMASI
        Log.d("ProductAdapter", "Products updated, new size: " + this.products.size());
    }

    // ViewHolder classes
    public static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView titleTextView;
        TextView brandTextView;
        TextView priceTextView;
        TextView discountTextView;
        ImageButton favoriteButton;
        ImageButton addToCartButton;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.image_product);
            titleTextView = itemView.findViewById(R.id.text_title);
            brandTextView = itemView.findViewById(R.id.text_brand);
            priceTextView = itemView.findViewById(R.id.text_price);
            discountTextView = itemView.findViewById(R.id.text_discount);
            favoriteButton = itemView.findViewById(R.id.button_favorite);
            addToCartButton = itemView.findViewById(R.id.button_add);
        }
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView titleTextView;
        TextView brandTextView;
        TextView priceTextView;
        TextView discountTextView;
        ImageButton favoriteButton;
        Button addButton;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.image_product);
            titleTextView = itemView.findViewById(R.id.text_title);
            brandTextView = itemView.findViewById(R.id.text_brand);
            priceTextView = itemView.findViewById(R.id.text_price);
            discountTextView = itemView.findViewById(R.id.text_discount);
            favoriteButton = itemView.findViewById(R.id.button_favorite);
            addButton = itemView.findViewById(R.id.button_add);
        }
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView titleTextView;
        TextView priceTextView;
        RatingBar ratingBar;
        TextView ratingTextView;
        ImageButton favoriteButton;
        MaterialButton addToCartButton;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.product_image);
            titleTextView = itemView.findViewById(R.id.product_title);
            priceTextView = itemView.findViewById(R.id.product_price);
            ratingBar = itemView.findViewById(R.id.product_rating);
            ratingTextView = itemView.findViewById(R.id.product_rating_text);
            favoriteButton = itemView.findViewById(R.id.button_favorite);
            addToCartButton = itemView.findViewById(R.id.button_add_to_cart);
        }
    }
}
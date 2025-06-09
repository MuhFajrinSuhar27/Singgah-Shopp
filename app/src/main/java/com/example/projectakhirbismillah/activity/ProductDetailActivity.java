package com.example.projectakhirbismillah.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.CurrencyUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class ProductDetailActivity extends AppCompatActivity {

    private Product product;
    private ImageView productImage;
    private TextView productTitle, productPrice, productPriceRange, productDescription, productStock;
    private TextView discountPercentage, quantityText;
    private LinearLayout storageOptionsContainer;
    private Button addToBagButton;
    private ImageButton decreaseButton, increaseButton;
    private int quantity = 1;
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Product Details");
        }

        // Initialize UI elements
        initViews();

        // Get product from intent
        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize cart manager
        cartManager = CartManager.getInstance();

        // Set up product details
        displayProductDetails();

        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        productImage = findViewById(R.id.product_image);
        productTitle = findViewById(R.id.product_title);
        productPrice = findViewById(R.id.product_price);
        productPriceRange = findViewById(R.id.product_price_range);
        productDescription = findViewById(R.id.product_description);
        productStock = findViewById(R.id.product_stock);
        discountPercentage = findViewById(R.id.discount_percentage);
        storageOptionsContainer = findViewById(R.id.storage_options_container);
        addToBagButton = findViewById(R.id.button_add_to_bag);
        decreaseButton = findViewById(R.id.btn_decrease);
        increaseButton = findViewById(R.id.btn_increase);
        quantityText = findViewById(R.id.text_quantity);
    }

    private void displayProductDetails() {
        // Set product title
        productTitle.setText(product.getTitle());
    
        // Set product image
        Glide.with(this)
                .load(product.getThumbnail())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(productImage);
    
        // Set product price dengan pembulatan ke ribuan
        double price = product.getPrice();
        productPrice.setText(CurrencyUtil.formatToRupiah(price));
    
        // Jika perlu range harga, gunakan juga pembulatan
        double maxPrice = price * 2;
        productPriceRange.setText(CurrencyUtil.formatToRupiah(price) + " - " + CurrencyUtil.formatToRupiah(maxPrice));
    
        // Set product description
        String description = product.getDescription();
        if (description != null && !description.isEmpty()) {
            productDescription.setText(description);
        } else {
            productDescription.setText("No description available for this product.");
        }
    
        // Set discount jika ada
        if (product.getDiscountPercentage() > 0) {
            int discountValue = (int) Math.round(product.getDiscountPercentage());
            discountPercentage.setText(discountValue + "% OFF");
            discountPercentage.setVisibility(View.VISIBLE);
        } else {
            discountPercentage.setVisibility(View.GONE);
        }
    
        // Add storage options dynamically
        addStorageOptions();
    }

    private void addStorageOptions() {
        // For this example, we'll simulate storage options
        String[] storageOptions = {"128 GB", "256 GB"};

        for (String option : storageOptions) {
            Chip chip = new Chip(this);
            chip.setText(option);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Add layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(16);

            // Add to container
            storageOptionsContainer.addView(chip, params);

            // Set first option as selected by default
            if (option.equals(storageOptions[0])) {
                chip.setChecked(true);
            }
        }
    }

    private void setupClickListeners() {
        // Add to bag button
        addToBagButton.setOnClickListener(v -> {
            addToCart();
            Toast.makeText(this, "Added to cart: " + product.getTitle(), Toast.LENGTH_SHORT).show();
        });

        // Decrease quantity button
        decreaseButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityDisplay();
            }
        });

        // Increase quantity button
        increaseButton.setOnClickListener(v -> {
            quantity++;
            updateQuantityDisplay();
        });
    }

    private void updateQuantityDisplay() {
        quantityText.setText(String.valueOf(quantity));
    }

    private void addToCart() {
        cartManager.addToCart(product, quantity);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
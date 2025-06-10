package com.example.projectakhirbismillah.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.CurrencyUtil;
import com.example.projectakhirbismillah.util.MidtransHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";
    private static final int PAYMENT_REQUEST_CODE = 100;

    private Product product;
    private ImageView productImage;
    private TextView productTitle, productPrice, productPriceRange, productDescription, productStock;
    private TextView discountPercentage, quantityText, productRating, productReviews;
    private Button addToBagButton, buyNowButton;
    private ImageButton decreaseButton, increaseButton;
    private int quantity = 1;
    private CartManager cartManager;
    private ProgressBar progressBar;

    // Thread management
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detail Produk");
        }

        // Initialize UI elements
        initViews();

        // Get product from intent
        product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            Toast.makeText(this, "Error memuat produk", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize cart manager
        cartManager = CartManager.getInstance(getApplicationContext());

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
        productRating = findViewById(R.id.product_rating);
        productReviews = findViewById(R.id.product_reviews);
        discountPercentage = findViewById(R.id.discount_percentage);
        addToBagButton = findViewById(R.id.button_add_to_bag);
        buyNowButton = findViewById(R.id.button_buy_now);
        decreaseButton = findViewById(R.id.btn_decrease);
        increaseButton = findViewById(R.id.btn_increase);
        quantityText = findViewById(R.id.text_quantity);

        // Inisialisasi ProgressBar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = android.view.Gravity.CENTER;
        progressBar.setLayoutParams(params);
        progressBar.setVisibility(View.GONE);

        // Tambahkan ProgressBar ke layout root
        View rootView = findViewById(android.R.id.content);
        if (rootView instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) rootView).addView(progressBar);
        }
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
        double maxPrice = price * 1.2; // Contoh range harga +20%
        productPriceRange.setText(CurrencyUtil.formatToRupiah(price) + " - " + CurrencyUtil.formatToRupiah(maxPrice));

        // Set product description dalam bahasa Indonesia (PENDEK)
        String description = getShortIndonesianDescription(product);
        productDescription.setText(description);

        // Set product rating
        if (productRating != null) {
            productRating.setText(String.format("★ %.1f/5.0", product.getRating()));
        }

        // Set customer reviews
        if (productReviews != null) {
            productReviews.setText(generateCustomerReviews(product));
        }

        // Set stock
        productStock.setText("Stok: " + product.getStock() + " tersedia");

        // Set discount jika ada
        if (product.getDiscountPercentage() > 0) {
            int discountValue = (int) Math.round(product.getDiscountPercentage());
            discountPercentage.setText("DISKON " + discountValue + "%");
            discountPercentage.setVisibility(View.VISIBLE);
        } else {
            discountPercentage.setVisibility(View.GONE);
        }

        // Update quantity display
        updateQuantityDisplay();
    }

    /**
     * Menghasilkan deskripsi produk PENDEK dalam bahasa Indonesia berdasarkan kategori
     */
    private String getShortIndonesianDescription(Product product) {
        String category = product.getCategory();
        StringBuilder description = new StringBuilder();

        // Deskripsi pendek berdasarkan kategori
        switch (category.toLowerCase()) {
            case "smartphones":
                description.append("📱 Smartphone berkualitas tinggi dengan kamera jernih dan performa cepat. ");
                description.append("Dilengkapi fitur canggih untuk kebutuhan sehari-hari. ");
                description.append("Cocok untuk profesional dan content creator.");
                break;

            case "womens-dresses":
                description.append("👗 Dress wanita dengan desain trendy dan bahan berkualitas premium. ");
                description.append("Nyaman dipakai untuk berbagai acara formal maupun kasual. ");
                description.append("Tersedia dalam berbagai ukuran.");
                break;

            case "mens-shoes":
                description.append("👞 Sepatu pria berkualitas dengan bahan premium dan sol yang nyaman. ");
                description.append("Desain stylish yang cocok untuk formal dan kasual. ");
                description.append("Tahan lama dan mudah dirawat.");
                break;

            case "fragrances":
                description.append("🌸 Parfum premium dengan aroma tahan lama dan kemasan elegant. ");
                description.append("Kombinasi notes yang memikat dan cocok untuk pria maupun wanita. ");
                description.append("Perfect untuk gift atau penggunaan sehari-hari.");
                break;

            default:
                description.append("🛍️ Produk berkualitas premium dengan harga terjangkau. ");
                description.append("Garansi resmi dan pengiriman cepat. ");
                description.append("Trusted seller dengan kualitas terjamin.");
                break;
        }

        return description.toString();
    }

    /**
     * Generate customer reviews berdasarkan rating produk
     */
    private String generateCustomerReviews(Product product) {
        double rating = product.getRating();
        StringBuilder reviews = new StringBuilder();

        // Generate reviews berdasarkan rating
        if (rating >= 4.5) {
            reviews.append("⭐⭐⭐⭐⭐ Sarah K.\n");
            reviews.append("\"Produk sangat bagus! Kualitas sesuai ekspektasi dan pengiriman cepat. Highly recommended!\"\n\n");

            reviews.append("⭐⭐⭐⭐⭐ Budi S.\n");
            reviews.append("\"Pelayanan memuaskan, barang original. Pasti beli lagi di sini.\"\n\n");

            reviews.append("⭐⭐⭐⭐⭐ Maya R.\n");
            reviews.append("\"Packaging rapi, produk sesuai deskripsi. Terima kasih!\"\n\n");

        } else if (rating >= 4.0) {
            reviews.append("⭐⭐⭐⭐⭐ Dewi A.\n");
            reviews.append("\"Bagus banget! Worth the price. Pengiriman juga cepat.\"\n\n");

            reviews.append("⭐⭐⭐⭐☆ Andi P.\n");
            reviews.append("\"Produk oke, cuma pengiriman agak lama. Overall satisfied.\"\n\n");

            reviews.append("⭐⭐⭐⭐⭐ Lisa M.\n");
            reviews.append("\"Kualitas bagus, sesuai gambar. Recommended seller!\"\n\n");

        } else if (rating >= 3.5) {
            reviews.append("⭐⭐⭐⭐☆ Roni T.\n");
            reviews.append("\"Produk cukup bagus untuk harga segini. Ada sedikit minus tapi masih acceptable.\"\n\n");

            reviews.append("⭐⭐⭐⭐☆ Sari N.\n");
            reviews.append("\"Overall oke, tapi ada yang kurang sesuai ekspektasi. Still worth to buy.\"\n\n");

            reviews.append("⭐⭐⭐⭐⭐ Doni W.\n");
            reviews.append("\"Bagus! Seller responsif dan produk sesuai deskripsi.\"\n\n");

        } else {
            reviews.append("⭐⭐⭐☆☆ Ahmad F.\n");
            reviews.append("\"Produk standar untuk harga segini. Ada beberapa kekurangan tapi masih bisa dipakai.\"\n\n");

            reviews.append("⭐⭐⭐⭐☆ Nina K.\n");
            reviews.append("\"Lumayan, tapi ada yang perlu diperbaiki. Pengiriman oke.\"\n\n");

            reviews.append("⭐⭐⭐☆☆ Ferry L.\n");
            reviews.append("\"Biasa aja, sesuai harga. Ada plus minus.\"\n\n");
        }

        // Tambah info total review
        int totalReviews = (int) (rating * 100) + 50; // Simulasi jumlah review
        reviews.append("📊 Total ").append(totalReviews).append(" ulasan dari pembeli terverifikasi");

        return reviews.toString();
    }

    private void setupClickListeners() {
        // Add to bag button
        addToBagButton.setOnClickListener(v -> {
            addToCart();
            Toast.makeText(this, "Berhasil ditambahkan ke keranjang: " + product.getTitle(), Toast.LENGTH_SHORT).show();
        });

        // Buy now button
        buyNowButton.setOnClickListener(v -> {
            processBuyNow();
        });

        // Decrease quantity button
        decreaseButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityDisplay();
            } else {
                Toast.makeText(this, "Jumlah minimal adalah 1", Toast.LENGTH_SHORT).show();
            }
        });

        // Increase quantity button
        increaseButton.setOnClickListener(v -> {
            if (quantity < product.getStock()) {
                quantity++;
                updateQuantityDisplay();
            } else {
                Toast.makeText(this, "Stok tidak mencukupi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQuantityDisplay() {
        quantityText.setText(String.valueOf(quantity));
    }

    private void addToCart() {
        cartManager.addToCart(product, quantity);
    }

    private void processBuyNow() {
        // Validate stock
        if (quantity > product.getStock()) {
            Toast.makeText(this, "Stok tidak mencukupi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple taps
        buyNowButton.setEnabled(false);
        buyNowButton.setText("Memproses...");

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);

        // Initialize Midtrans
        MidtransHelper.initMidtrans(this);

        // Create a temporary cart item for direct checkout
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(product, quantity));

        // Process checkout in background thread
        executor.execute(() -> {
            try {
                // Get payment URL
                MidtransHelper.getPaymentUrl(this, items, new MidtransHelper.PaymentUrlCallback() {
                    @Override
                    public void onSuccess(String paymentUrl, String orderId) {
                        runOnUiThread(() -> {
                            try {
                                // Launch payment activity
                                Intent intent = new Intent(ProductDetailActivity.this, PaymentWebViewActivity.class);
                                intent.putExtra(PaymentWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
                                intent.putExtra(PaymentWebViewActivity.EXTRA_ORDER_ID, orderId);
                                startActivityForResult(intent, PAYMENT_REQUEST_CODE);

                                // Reset UI
                                resetBuyNowButton();
                            } catch (Exception e) {
                                Log.e(TAG, "Error starting payment", e);
                                handlePaymentError("Error memulai pembayaran: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        handlePaymentError("Gagal memproses pembayaran: " + message);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing payment", e);
                handlePaymentError("Error memproses pembayaran: " + e.getMessage());
            }
        });
    }

    private void resetBuyNowButton() {
        progressBar.setVisibility(View.GONE);
        buyNowButton.setEnabled(true);
        buyNowButton.setText("Beli Sekarang");
    }

    private void handlePaymentError(String message) {
        runOnUiThread(() -> {
            resetBuyNowButton();
            Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Payment error: " + message);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYMENT_REQUEST_CODE) {
            if (resultCode == PaymentWebViewActivity.PAYMENT_SUCCESS) {
                // Payment successful
                Toast.makeText(this, "Pembayaran berhasil! Terima kasih atas pembelian Anda.", Toast.LENGTH_LONG).show();
                finish(); // Return to previous screen
            } else if (resultCode == PaymentWebViewActivity.PAYMENT_PENDING) {
                Toast.makeText(this, "Pembayaran tertunda. Silakan selesaikan pembayaran Anda.",
                        Toast.LENGTH_LONG).show();
            } else if (resultCode == PaymentWebViewActivity.PAYMENT_FAILED) {
                Toast.makeText(this, "Pembayaran gagal. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Pembayaran dibatalkan.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
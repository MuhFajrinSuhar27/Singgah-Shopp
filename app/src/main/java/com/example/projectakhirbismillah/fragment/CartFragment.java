package com.example.projectakhirbismillah.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.activity.PaymentWebViewActivity;
import com.example.projectakhirbismillah.adapter.CartAdapter;
import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.MidtransHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment implements CartManager.CartChangeListener {
    private static final String TAG = "CartFragment";
    private static final int PAYMENT_REQUEST_CODE = 1001;

    private TextView textCart;
    private RecyclerView recyclerCart;
    private View cartBottomPanel;
    private View emptyCartLayout;
    private MaterialButton buttonStartShopping;
    private TextView textSubtotal;
    private TextView textTotalPrice;
    private TextView cartHeader;
    private CartAdapter cartAdapter;
    private CartManager cartManager;
    private MaterialButton buttonCheckout;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_cart_fragment, container, false);

        // Initialize views
        initViews(view);

        // Initialize cart manager
        cartManager = CartManager.getInstance();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup button click listeners
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        textCart = view.findViewById(R.id.text_cart);
        recyclerCart = view.findViewById(R.id.recycler_cart);
        cartBottomPanel = view.findViewById(R.id.cart_bottom_panel);
        emptyCartLayout = view.findViewById(R.id.empty_cart_layout);
        buttonStartShopping = view.findViewById(R.id.button_start_shopping);
        cartHeader = view.findViewById(R.id.cart_header);
        textSubtotal = view.findViewById(R.id.text_subtotal);
        textTotalPrice = view.findViewById(R.id.text_total_price);
        buttonCheckout = view.findViewById(R.id.button_checkout);
        progressBar = view.findViewById(R.id.progress_bar);

        // Jika progress bar tidak ada di layout, buat secara programmatik
        if (progressBar == null) {
            progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleSmall);
            progressBar.setId(View.generateViewId());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            progressBar.setLayoutParams(params);
            progressBar.setVisibility(View.GONE);

            // Tambahkan ke layout di dekat button checkout
            if (buttonCheckout.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) buttonCheckout.getParent();
                int index = parent.indexOfChild(buttonCheckout);
                parent.addView(progressBar, index + 1);
            }
        }
    }

    private void setupRecyclerView() {
        recyclerCart.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(getContext(), new ArrayList<>());
        // Hapus line berikut yang menyebabkan error
        // cartAdapter.setOnItemDeletedListener(position -> { ... });
        recyclerCart.setAdapter(cartAdapter);
    }

    private void setupClickListeners() {
        buttonStartShopping.setOnClickListener(v -> navigateToHome());
        buttonCheckout.setOnClickListener(v -> processCheckout());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateCartUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        cartManager.addListener(this);
        updateCartUI();

        // Make sure checkout button is enabled
        if (buttonCheckout != null) {
            buttonCheckout.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        cartManager.removeListener(this);
        super.onPause();
    }

    @Override
    public void onCartChanged() {
        updateCartUI();
    }

    /**
     * Update UI to reflect current cart state
     */
    private void updateCartUI() {
        if (!isAdded() || getContext() == null) return;

        List<CartItem> items = cartManager.getCartItems();
        boolean isEmpty = items.isEmpty();

        updateCartVisibility(isEmpty);

        if (!isEmpty) {
            cartAdapter.updateCart(items);
            updatePrices();

            // Update header text
            int totalItems = cartManager.getTotalQuantity();
            cartHeader.setText(String.format(Locale.getDefault(), "Your Items (%d)", totalItems));
        }
    }

    /**
     * Update price displays with current cart totals
     */
    private void updatePrices() {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        double subtotal = cartManager.getTotalPrice();

        // Display prices
        textSubtotal.setText(currencyFormatter.format(subtotal));
        textTotalPrice.setText(currencyFormatter.format(subtotal));
    }

    /**
     * Navigate to the home fragment
     */
    private void navigateToHome() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();

            // Update selected navigation item
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_home);
            }
        }
    }

    /**
     * Process checkout with WebView payment gateway
     */
    private void processCheckout() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        // Validate cart
        List<CartItem> cartItems = cartManager.getCartItems();
        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(), "Keranjang belanja Anda kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate total price
        double totalPrice = cartManager.getTotalPrice();

        // Debug info
        Log.d(TAG, "Starting checkout with " + cartItems.size() + " items");
        Log.d(TAG, "Total price: " + totalPrice);

        // Disable checkout button & show loading
        buttonCheckout.setEnabled(false);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Get payment URL
        MidtransHelper.getPaymentUrl(getContext(), cartItems, new MidtransHelper.PaymentUrlCallback() {
            @Override
            public void onSuccess(String paymentUrl, String orderId) {
                // Run on UI thread since callback might come from background thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Launch WebView activity
                            Intent intent = new Intent(getContext(), PaymentWebViewActivity.class);
                            intent.putExtra(PaymentWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
                            intent.putExtra(PaymentWebViewActivity.EXTRA_ORDER_ID, orderId);
                            startActivityForResult(intent, PAYMENT_REQUEST_CODE);

                            // Re-enable button after starting activity
                            buttonCheckout.setEnabled(true);
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Show error and re-enable button
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            buttonCheckout.setEnabled(true);
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Update visibility of cart views based on whether cart is empty
     */
    private void updateCartVisibility(boolean isEmpty) {
        if (isEmpty) {
            emptyCartLayout.setVisibility(View.VISIBLE);
            recyclerCart.setVisibility(View.GONE);
            cartBottomPanel.setVisibility(View.GONE);
            cartHeader.setVisibility(View.GONE);
        } else {
            emptyCartLayout.setVisibility(View.GONE);
            recyclerCart.setVisibility(View.VISIBLE);
            cartBottomPanel.setVisibility(View.VISIBLE);
            cartHeader.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handle payment result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYMENT_REQUEST_CODE) {
            String orderId = data != null ? data.getStringExtra(PaymentWebViewActivity.EXTRA_ORDER_ID) : null;

            switch (resultCode) {
                case PaymentWebViewActivity.PAYMENT_SUCCESS:
                    Toast.makeText(getContext(), "Pembayaran berhasil!", Toast.LENGTH_LONG).show();

                    // Clear cart after successful payment
                    cartManager.clearCart();
                    updateCartUI();

                    // You can handle order completion logic here
                    Log.d(TAG, "Payment successful for order: " + orderId);
                    break;

                case PaymentWebViewActivity.PAYMENT_PENDING:
                    Toast.makeText(getContext(),
                            "Pembayaran dalam proses. Silakan selesaikan pembayaran Anda.",
                            Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Payment pending for order: " + orderId);
                    break;

                case PaymentWebViewActivity.PAYMENT_FAILED:
                    Toast.makeText(getContext(), "Pembayaran gagal atau dibatalkan", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Payment failed for order: " + orderId);
                    break;
            }
        }
    }
    }
package com.example.projectakhirbismillah.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.activity.PaymentWebViewActivity;
import com.example.projectakhirbismillah.adapter.CartAdapter;
import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.util.CartManager;
import com.example.projectakhirbismillah.util.CurrencyUtil;
import com.example.projectakhirbismillah.util.MidtransHelper;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CartFragment extends Fragment implements CartManager.CartChangeListener {
    private static final int PAYMENT_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private LinearLayout emptyCartLayout;
    private TextView totalPriceText, cartHeaderText, subtotalText;
    private MaterialButton buttonCheckout, buttonStartShopping;
    private View cartBottomPanel;
    private ProgressBar progressBar;
    private CartManager cartManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_cart_fragment, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_cart);
        emptyCartLayout = view.findViewById(R.id.empty_cart_layout);
        totalPriceText = view.findViewById(R.id.text_total_price);
        subtotalText = view.findViewById(R.id.text_subtotal);
        buttonCheckout = view.findViewById(R.id.button_checkout);
        buttonStartShopping = view.findViewById(R.id.button_start_shopping);
        cartHeaderText = view.findViewById(R.id.cart_header);
        cartBottomPanel = view.findViewById(R.id.cart_bottom_panel);

        // Tambahkan ProgressBar dengan LayoutParams yang sesuai
        if (progressBar == null) {
            progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyle);
            progressBar.setVisibility(View.GONE);
            
            // Periksa tipe parent layout dan gunakan LayoutParams yang sesuai
            ViewGroup parentView = (ViewGroup) view;

            if (parentView instanceof RelativeLayout) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                progressBar.setLayoutParams(params);
            }
            else if (parentView instanceof ConstraintLayout) {
                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                progressBar.setLayoutParams(params);
            }
            else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                progressBar.setLayoutParams(params);
            }

            parentView.addView(progressBar);
        }

        // Initialize cart manager
        cartManager = CartManager.getInstance(requireContext());  // TAMBAHKAN CONTEXT DI SINI

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup adapter
        setupAdapter();

        // Setup checkout button
        buttonCheckout.setOnClickListener(v -> processCheckout());

        // Setup start shopping button
        buttonStartShopping.setOnClickListener(v -> {
            // Navigate to products/home fragment
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        // Load cart data
        loadCartData();

        return view;
    }

    private void setupAdapter() {
        List<CartItem> cartItems = cartManager.getCartItems();
        adapter = new CartAdapter(getContext(), cartItems);
        recyclerView.setAdapter(adapter);

        adapter.setOnCartItemChangedListener(new CartAdapter.OnCartItemChangedListener() {
            @Override
            public void onQuantityChanged() {
                updateTotalPrice();
            }

            @Override
            public void onItemRemoved() {
                loadCartData();
            }
        });
    }

    private void loadCartData() {
        List<CartItem> cartItems = cartManager.getCartItems();

        if (cartItems.isEmpty()) {
            showEmptyCart();
        } else {
            showCartWithItems(cartItems.size());
            updateTotalPrice();
        }
    }

    private void showEmptyCart() {
        recyclerView.setVisibility(View.GONE);
        cartHeaderText.setVisibility(View.GONE);
        cartBottomPanel.setVisibility(View.GONE);
        emptyCartLayout.setVisibility(View.VISIBLE);
    }

    private void showCartWithItems(int itemCount) {
        recyclerView.setVisibility(View.VISIBLE);
        cartHeaderText.setVisibility(View.VISIBLE);
        cartBottomPanel.setVisibility(View.VISIBLE);
        emptyCartLayout.setVisibility(View.GONE);

        // Update header dengan jumlah item
        cartHeaderText.setText("Your Items (" + itemCount + ")");
    }

    private void updateTotalPrice() {
        List<CartItem> cartItems = cartManager.getCartItems();
        double total = 0;
        for (CartItem item : cartItems) {
            // Gunakan harga yang sudah dibulatkan ke ribuan untuk tampilan
            total += CurrencyUtil.getIDRPriceRoundedToThousand(item.getProduct().getPrice()) / 15500.0 * item.getQuantity();
        }

        // Format harga dalam Rupiah
        subtotalText.setText(CurrencyUtil.formatToRupiah(total));
        
        totalPriceText.setText(CurrencyUtil.formatToRupiah(total));
    }

    private void processCheckout() {
        if (getContext() == null) return;

        // Validate cart
        List<CartItem> cartItems = cartManager.getCartItems();
        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(), "Keranjang belanja Anda kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        buttonCheckout.setEnabled(false);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Initialize Midtrans
        MidtransHelper.initMidtrans(getContext());

        // Get payment URL
        MidtransHelper.getPaymentUrl(getContext(), cartItems, new MidtransHelper.PaymentUrlCallback() {
            @Override
            public void onSuccess(String paymentUrl, String orderId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Intent intent = new Intent(getContext(), PaymentWebViewActivity.class);
                        intent.putExtra(PaymentWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
                        intent.putExtra(PaymentWebViewActivity.EXTRA_ORDER_ID, orderId);
                        startActivityForResult(intent, PAYMENT_REQUEST_CODE);

                        buttonCheckout.setEnabled(true);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                        buttonCheckout.setEnabled(true);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYMENT_REQUEST_CODE) {
            if (resultCode == PaymentWebViewActivity.PAYMENT_SUCCESS) {
                // Payment successful - clear cart
                cartManager.clearCart();
                loadCartData();
                Toast.makeText(getContext(), "Pembayaran berhasil!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == PaymentWebViewActivity.PAYMENT_PENDING) {
                Toast.makeText(getContext(), "Pembayaran pending, silakan selesaikan pembayaran Anda",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Pembayaran dibatalkan atau gagal", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register as listener
        cartManager.addListener(this);
        loadCartData();
    }

    @Override
    public void onPause() {
        // Unregister listener
        cartManager.removeListener(this);
        super.onPause();
    }

    @Override
    public void onCartChanged() {
        if (isAdded()) {
            loadCartData();
        }
    }
}
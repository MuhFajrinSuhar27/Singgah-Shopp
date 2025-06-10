package com.example.projectakhirbismillah.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.example.projectakhirbismillah.MainActivity;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.adapter.BannerAdapter;
import com.example.projectakhirbismillah.adapter.CategoryAdapter;
import com.example.projectakhirbismillah.adapter.ProductAdapter;
import com.example.projectakhirbismillah.api.ApiService;
import com.example.projectakhirbismillah.api.RetrofitClient;
import com.example.projectakhirbismillah.model.BannerItem;
import com.example.projectakhirbismillah.model.CategoryItem;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.model.ProductResponse;
import com.example.projectakhirbismillah.util.FavoriteManager;
import com.example.projectakhirbismillah.util.NetworkStateManager;
import com.example.projectakhirbismillah.util.ThemeHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Response;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private static final String TAG = "HomeFragment";
    private ViewPager2 bannerViewPager;
    private LinearLayout indicatorContainer;
    private RecyclerView recyclerProducts;
    private RecyclerView recyclerCategories;
    private TextView textViewAll;
    private List<BannerItem> bannerItems;
    private List<Product> allProducts = new ArrayList<>();
    private List<CategoryItem> categoryItems = new ArrayList<>();
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;

    // Initialize Handler dan Runnable di awal - bukan null
    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;

    private int currentBannerPosition = 0;
    private boolean isFragmentAttached = false;
    private Call<ProductResponse> apiCall;
    private String currentCategory = "all"; // Default category: all
    private int currentCategoryIndex = 0;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private ImageButton themeToggleButton; // For theme toggle
    private LinearLayout mainLinearLayout; // Root LinearLayout in NestedScrollView

    // Network error UI components
    private View networkErrorLayout;
    private Button buttonRefresh;
    private boolean isLoading = false;

    // Thread management
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Kategori yang ingin ditampilkan
    private final String[] categories = {
            "smartphones",     // Phone
            "womens-dresses",  // Fashion
            "mens-shoes",      // Shoes
            "fragrances"       // Parfum
    };

    // Initialize autoScrollRunnable in constructor or init block
    {
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerViewPager != null) {
                    if (currentBannerPosition == (bannerItems != null ? bannerItems.size() : 0) - 1) {
                        bannerViewPager.setCurrentItem(0);
                    } else {
                        bannerViewPager.setCurrentItem(currentBannerPosition + 1);
                    }
                }

                // Pastikan handler dan fragment masih aktif sebelum posting kembali
                if (autoScrollHandler != null && isFragmentAttached) {
                    autoScrollHandler.postDelayed(this, 3000);
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home_fragment, container, false);

        // Get main container (frame layout)
        ViewGroup rootView = view.findViewById(R.id.home_root_layout);

        // Initialize views
        recyclerProducts = view.findViewById(R.id.recycler_products);
        progressBar = view.findViewById(R.id.progress_bar);

        // Get the main LinearLayout inside NestedScrollView
        mainLinearLayout = view.findViewById(R.id.main_linear_layout);

        // Inflate network error layout
        networkErrorLayout = inflater.inflate(R.layout.network_error_layout, container, false);

        // Add network error layout to root
        rootView.addView(networkErrorLayout);

        // Hide network error initially
        networkErrorLayout.setVisibility(View.GONE);

        // Setup refresh button
        buttonRefresh = networkErrorLayout.findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(v -> retryLoading());

        // Initialize FavoriteManager in background thread
        executor.execute(() -> {
            try {
                FavoriteManager.getInstance().initialize(requireContext());
                Log.d(TAG, "FavoriteManager initialized in background thread");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing FavoriteManager: " + e.getMessage());
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi bannerItems untuk mencegah NPE
        bannerItems = new ArrayList<>();

        // Initialize views
        bannerViewPager = view.findViewById(R.id.banner_viewpager);
        indicatorContainer = view.findViewById(R.id.indicator_container);
        textViewAll = view.findViewById(R.id.text_view_all);
        recyclerCategories = view.findViewById(R.id.recycler_categories);

        // Setup UI components in background thread
        executor.execute(() -> {
            try {
                // Operasi yang dapat dilakukan di background
                final List<BannerItem> bannerItemsBackground = createBannerItems();
                final List<CategoryItem> categoryItemsBackground = createCategoryItemsInBackground();

                // Update UI di main thread dengan pengecekan isAdded
                mainHandler.post(() -> {
                    // Pastikan fragment masih attached ke context
                    if (!isAdded()) {
                        Log.d(TAG, "Fragment not attached. Skipping UI updates.");
                        return;
                    }

                    try {
                        // Setup carousel banner dengan data dari background thread
                        setupBannerCarousel(bannerItemsBackground);

                        // Create and add theme toggle button
                        addThemeToggleButton();

                        // Setup categories dengan data dari background thread
                        setupCategories(categoryItemsBackground);

                        // Setup products grid
                        setupProductsGrid();

                        // Set click listener for "View all"
                        textViewAll.setOnClickListener(v -> {
                            Toast.makeText(getContext(), "View all products", Toast.LENGTH_SHORT).show();
                        });

                        // Fetch products from specific categories
                        fetchAllProducts();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in UI updates: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in background processing: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Menambahkan tombol toggle theme dengan cara yang aman
     */
    private void addThemeToggleButton() {
        if (!isAdded() || mainLinearLayout == null) return;

        // Cari LinearLayout yang berisi categories section (judul "Categories")
        View categoriesHeaderView = null;

        // Cari parent dari TextView "Categories"
        for (int i = 0; i < mainLinearLayout.getChildCount(); i++) {
            View child = mainLinearLayout.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (textView.getText().toString().equals("Categories")) {
                    categoriesHeaderView = textView;
                    break;
                }
            }
        }

        if (categoriesHeaderView == null) {
            Log.e(TAG, "Could not find Categories header view");
            return;
        }

        // Buat container baru untuk tombol toggle
        RelativeLayout toggleContainer = new RelativeLayout(getContext());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Buat tombol toggle
        themeToggleButton = new ImageButton(getContext());
        themeToggleButton.setId(View.generateViewId());
        themeToggleButton.setBackgroundResource(R.drawable.circle_button_background);
        themeToggleButton.setPadding(16, 16, 16, 16);
        themeToggleButton.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Set ikon sesuai tema saat ini
        updateThemeToggleIcon();

        // Buat layout params untuk tombol
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        buttonParams.setMargins(0, 8, 16, 8);

        // Tambahkan tombol ke container
        toggleContainer.addView(themeToggleButton, buttonParams);

        // Tambahkan container sebelum categories header
        int index = mainLinearLayout.indexOfChild(categoriesHeaderView);
        mainLinearLayout.addView(toggleContainer, index, containerParams);

        // Tambahkan listener untuk toggle tema
        themeToggleButton.setOnClickListener(v -> toggleTheme());
    }

    /**
     * Update the theme toggle icon based on current theme
     */
    private void updateThemeToggleIcon() {
        if (themeToggleButton == null || !isAdded()) return;

        boolean isDarkMode = ThemeHelper.isDarkMode(getContext());
        themeToggleButton.setImageResource(isDarkMode ?
                R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
    }

    /**
     * Toggle between light and dark theme
     */
    private void toggleTheme() {
        if (!isAdded()) return;

        Log.d(TAG, "Toggle theme clicked. Current dark mode: " + ThemeHelper.isDarkMode(requireContext()));

        try {
            // Simpan dan terapkan tema baru
            boolean isDarkMode = ThemeHelper.isDarkMode(requireContext());
            int newThemeMode = isDarkMode ? ThemeHelper.MODE_LIGHT : ThemeHelper.MODE_DARK;

            // Simpan preferensi tema
            ThemeHelper.saveThemeMode(requireContext(), newThemeMode);

            // Terapkan tema baru
            ThemeHelper.applyTheme(newThemeMode);

            // Update icon toggle tema
            updateThemeToggleIcon();

            // Refresh UI dengan rekreasi activity
            if (getActivity() != null) {
                Log.d(TAG, "Recreating activity to apply theme changes");
                getActivity().recreate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling theme: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupProductsGrid() {
        if (!isAdded()) return;

        // Setup RecyclerView with GridLayoutManager for item_product.xml
        recyclerProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize adapter with VIEW_TYPE_CARD for item_product.xml
        productAdapter = new ProductAdapter(getContext(), new ArrayList<>(), ProductAdapter.VIEW_TYPE_CARD);
        recyclerProducts.setAdapter(productAdapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        isFragmentAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isFragmentAttached = false;

        // Cancel API call if it's still running
        if (apiCall != null && !apiCall.isCanceled()) {
            apiCall.cancel();
        }
    }

    private void setupBannerCarousel(List<BannerItem> bannerItems) {
        if (!isAdded() || bannerViewPager == null) {
            Log.d(TAG, "Cannot setup banner carousel: Fragment not attached or ViewPager is null");
            return;
        }

        // Set banner items list
        this.bannerItems = bannerItems;

        try {
            // Initialize banner adapter
            BannerAdapter bannerAdapter = new BannerAdapter(getContext(), bannerItems);
            bannerViewPager.setAdapter(bannerAdapter);

            setupIndicators();

            bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    currentBannerPosition = position;
                    updateIndicators(position);
                    super.onPageSelected(position);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up banner carousel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<BannerItem> createBannerItems() {
        List<BannerItem> items = new ArrayList<>();
        items.add(new BannerItem(R.drawable.promo_banner1, "", ""));
        items.add(new BannerItem(R.drawable.promo_banner2, "", ""));
        items.add(new BannerItem(R.drawable.promo_banner3, "", ""));
        items.add(new BannerItem(R.drawable.promo_banner4, "", ""));
        return items;
    }

    private void setupIndicators() {
        // Pastikan container dan bannerItems ada dan fragment masih attached
        if (!isAdded() || indicatorContainer == null || bannerItems == null) return;

        try {
            // Clear previous indicators
            indicatorContainer.removeAllViews();

            ImageView[] indicators = new ImageView[bannerItems.size()];
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);

            for (int i = 0; i < indicators.length; i++) {
                indicators[i] = new ImageView(getContext());
                indicators[i].setImageResource(R.drawable.indicator_inactive);
                indicators[i].setLayoutParams(params);
                indicatorContainer.addView(indicators[i]);
            }

            updateIndicators(0);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up indicators: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateIndicators(int position) {
        if (!isAdded() || indicatorContainer == null) return;

        try {
            int childCount = indicatorContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ImageView imageView = (ImageView) indicatorContainer.getChildAt(i);
                if (i == position) {
                    imageView.setImageResource(R.drawable.indicator_active);
                } else {
                    imageView.setImageResource(R.drawable.indicator_inactive);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating indicators: " + e.getMessage());
        }
    }

    private void setupCategories(List<CategoryItem> categoryItems) {
        if (!isAdded() || recyclerCategories == null) return;

        try {
            // Use the provided category items
            this.categoryItems = categoryItems;

            // Setup RecyclerView for categories
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    getContext(), LinearLayoutManager.HORIZONTAL, false);
            recyclerCategories.setLayoutManager(layoutManager);

            // Create and set adapter
            categoryAdapter = new CategoryAdapter(getContext(), categoryItems, this);
            recyclerCategories.setAdapter(categoryAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up categories: " + e.getMessage());
        }
    }

    private List<CategoryItem> createCategoryItemsInBackground() {
        List<CategoryItem> items = new ArrayList<>();

        // Add all categories
        items.add(new CategoryItem("all", "All", R.drawable.ic_category_all));
        items.add(new CategoryItem("smartphones", "Phones", R.drawable.smartphone));
        items.add(new CategoryItem("womens-dresses", "Fashion", R.drawable.fashionn));
        items.add(new CategoryItem("mens-shoes", "Shoes", R.drawable.shoes));
        items.add(new CategoryItem("fragrances", "Perfumes", R.drawable.perfume));

        return items;
    }

    private void fetchAllProducts() {
        if (!isAdded()) return;

        // Reset product list
        allProducts.clear();
        currentCategoryIndex = 0;
        isLoading = true;

        // Show progress bar, hide error layout
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        hideNetworkError();

        // Check for network connectivity in background thread
        executor.execute(() -> {
            try {
                final boolean networkAvailable = isNetworkAvailable();

                // Kembali ke UI thread untuk update UI
                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    if (!networkAvailable) {
                        showNetworkError();
                        isLoading = false;
                        return;
                    }

                    // Start fetching products from first category
                    fetchProductsByCategory(categories[currentCategoryIndex]);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking network: " + e.getMessage());
                mainHandler.post(this::showNetworkError);
            }
        });
    }

    private void fetchProductsByCategory(String category) {
        if (!isFragmentAttached || !isAdded()) return;

        Log.d(TAG, "Fetching products for category: " + category);

        // Execute network call in background thread
        executor.execute(() -> {
            try {
                // Check network again in background thread
                if (!isNetworkAvailable()) {
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        showNetworkError();
                        isLoading = false;
                    });
                    return;
                }

                ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
                apiCall = apiService.getProductsByCategory(category);

                // Synchronous call in background thread
                Response<ProductResponse> response = apiCall.execute();

                // Process response in main thread
                mainHandler.post(() -> {
                    if (!isFragmentAttached || !isAdded()) {
                        Log.d(TAG, "Fragment is detached, ignoring API response");
                        return;
                    }

                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Product> categoryProducts = response.body().getProducts();
                            Log.d(TAG, "Received " + categoryProducts.size() + " products for " + category);

                            // Add products from this category to the list
                            allProducts.addAll(categoryProducts);

                            // Fetch next category or update UI if all categories are done
                            fetchNextCategoryOrUpdate();
                        } else {
                            Log.e(TAG, "Failed to fetch products for category: " + category);
                            fetchNextCategoryOrUpdate();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing API response: " + e.getMessage());
                        fetchNextCategoryOrUpdate();
                    }
                });
            } catch (IOException e) {
                // Handle error in main thread
                mainHandler.post(() -> {
                    if (!isAdded()) return;

                    Log.e(TAG, "API call failed for " + category + ": " + e.getMessage());

                    // Show error only if no products loaded yet
                    if (allProducts.isEmpty()) {
                        showNetworkError();
                    } else {
                        // If some products loaded, continue with what we have
                        Toast.makeText(getContext(), "Gagal memuat beberapa produk", Toast.LENGTH_SHORT).show();
                    }

                    isLoading = false;
                });
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error: " + e.getMessage());
                mainHandler.post(() -> {
                    if (isAdded()) {
                        showNetworkError();
                        isLoading = false;
                    }
                });
            }
        });
    }

    private void fetchNextCategoryOrUpdate() {
        if (!isAdded()) return;

        currentCategoryIndex++;

        // Check if there are more categories to process
        if (currentCategoryIndex < categories.length) {
            fetchProductsByCategory(categories[currentCategoryIndex]);
        } else {
            // All categories done, update UI
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            isLoading = false;

            if (allProducts.isEmpty()) {
                // If no products loaded, show error
                showNetworkError();
            } else {
                // If products loaded, filter and display
                hideNetworkError();
                // Process filtering in background thread
                executor.execute(() -> {
                    final List<Product> filteredProducts = filterProductsInBackground(currentCategory);

                    // Update UI in main thread
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        updateProductsUI(filteredProducts, currentCategory);
                    });
                });
            }
        }
    }

    private List<Product> filterProductsInBackground(String categoryId) {
        if (allProducts.isEmpty()) return new ArrayList<>();

        if (categoryId.equals("all")) {
            // Show all products from our 4 specific categories
            return new ArrayList<>(allProducts);
        } else {
            // Filter by selected category
            return allProducts.stream()
                    .filter(product -> product.getCategory().equals(categoryId))
                    .collect(Collectors.toList());
        }
    }

    private void updateProductsUI(List<Product> filteredList, String categoryId) {
        if (!isAdded() || recyclerProducts == null) return;

        try {
            // Update adapter with filtered products using VIEW_TYPE_CARD
            productAdapter = new ProductAdapter(getContext(), filteredList, ProductAdapter.VIEW_TYPE_CARD);
            recyclerProducts.setAdapter(productAdapter);

            // Update title
            if (textViewAll != null && textViewAll.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) textViewAll.getParent();
                if (parent.getChildCount() > 0 && parent.getChildAt(0) instanceof TextView) {
                    TextView titleTextView = (TextView) parent.getChildAt(0);

                    // Set title with product count
                    String categoryName = getCategoryDisplayName(categoryId);
                    titleTextView.setText(categoryName + " (" + filteredList.size() + ")");
                }
            }

            if (textViewAll != null) {
                textViewAll.setText(filteredList.isEmpty() ? "No products" : "View all");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating product UI: " + e.getMessage());
        }
    }

    private String getCategoryDisplayName(String categoryId) {
        switch (categoryId) {
            case "all":
                return "All Products";
            case "smartphones":
                return "Phones";
            case "womens-dresses":
                return "Fashion";
            case "mens-shoes":
                return "Shoes";
            case "fragrances":
                return "Perfumes";
            default:
                return "Products";
        }
    }

    @Override
    public void onCategoryClick(String categoryId) {
        if (!isAdded()) return;

        // Save current category
        currentCategory = categoryId;

        // Process filtering in background thread
        executor.execute(() -> {
            try {
                final List<Product> filteredProducts = filterProductsInBackground(categoryId);

                // Update UI in main thread
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    updateProductsUI(filteredProducts, categoryId);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in category click handling: " + e.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Pastikan handler dan runnable tidak null sebelum dipanggil
        if (isAdded() && autoScrollHandler != null && autoScrollRunnable != null && bannerItems != null && !bannerItems.isEmpty()) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3000);
        }

        // Reload products if favoriteManager changed
        if (isAdded() && productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }

        // Update theme icon in case it was changed elsewhere
        if (isAdded() && themeToggleButton != null) {
            updateThemeToggleIcon();
        }
    }

    @Override
    public void onPause() {
        // Pastikan handler dan runnable tidak null sebelum dipanggil
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        super.onPause();
    }

    private void retryLoading() {
        if (!isAdded() || isLoading) return;

        hideNetworkError();
        fetchAllProducts();
    }

    private void showNetworkError() {
        if (!isAdded()) return;

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (networkErrorLayout != null) {
            networkErrorLayout.setVisibility(View.VISIBLE);
        }
        if (recyclerProducts != null) {
            recyclerProducts.setVisibility(View.GONE);
        }
    }

    private void hideNetworkError() {
        if (!isAdded()) return;

        if (networkErrorLayout != null) {
            networkErrorLayout.setVisibility(View.GONE);
        }
        if (recyclerProducts != null) {
            recyclerProducts.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        return NetworkStateManager.getInstance().isNetworkAvailable();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shutdown thread pool untuk mencegah memory leak
        executor.shutdown();
    }
}
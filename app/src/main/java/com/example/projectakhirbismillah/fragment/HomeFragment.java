package com.example.projectakhirbismillah.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
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
    private Handler autoScrollHandler;
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

    // Kategori yang ingin ditampilkan
    private final String[] categories = {
            "smartphones",     // Phone
            "womens-dresses",  // Fashion
            "mens-shoes",      // Shoes
            "fragrances"       // Parfum
    };

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

        // Initialize FavoriteManager
        FavoriteManager.getInstance().initialize(requireContext());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        bannerViewPager = view.findViewById(R.id.banner_viewpager);
        indicatorContainer = view.findViewById(R.id.indicator_container);
        textViewAll = view.findViewById(R.id.text_view_all);
        recyclerCategories = view.findViewById(R.id.recycler_categories);

        // Setup carousel banner
        setupBannerCarousel();

        // Create and add theme toggle button without trying to modify the NestedScrollView
        addThemeToggleButton();

        // Setup categories
        setupCategories();

        // Setup products grid
        setupProductsGrid();

        // Set click listener for "View all"
        textViewAll.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View all products", Toast.LENGTH_SHORT).show();
        });

        // Fetch products from specific categories
        fetchAllProducts();
    }

    /**
     * Menambahkan tombol toggle theme dengan cara yang aman
     */
    private void addThemeToggleButton() {
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
        RelativeLayout toggleContainer = new RelativeLayout(requireContext());
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Buat tombol toggle
        themeToggleButton = new ImageButton(requireContext());
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
        boolean isDarkMode = ThemeHelper.isDarkMode(requireContext());
        themeToggleButton.setImageResource(isDarkMode ?
                R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
    }

    /**
     * Toggle between light and dark theme
     */
    private void toggleTheme() {
        Log.d(TAG, "Toggle theme clicked. Current dark mode: " + ThemeHelper.isDarkMode(requireContext()));
        
        // Toggle tema
        ThemeHelper.toggleTheme(requireContext());
        
        // Update ikon
        updateThemeToggleIcon();
        
        // PENTING: Gunakan cara yang lebih kuat untuk restart activity
        Log.d(TAG, "Restarting activity to apply new theme");
        
        // Gunakan Handler untuk delay sedikit agar SharedPreferences tersimpan
        new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                // Cara lebih pasti untuk restart activity dan menerapkan tema baru
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        }, 150); // Delay sedikit lebih lama untuk memastikan perubahan tema tersimpan
    }

    private void setupProductsGrid() {
        // Setup RecyclerView with GridLayoutManager for item_product.xml
        recyclerProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize adapter with VIEW_TYPE_CARD for item_product.xml
        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), ProductAdapter.VIEW_TYPE_CARD);
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

    private void setupBannerCarousel() {
        // Create banner items list
        bannerItems = createBannerItems();

        // Initialize banner adapter
        BannerAdapter bannerAdapter = new BannerAdapter(requireContext(), bannerItems);
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

        autoScrollHandler = new Handler();
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentBannerPosition == bannerItems.size() - 1) {
                    bannerViewPager.setCurrentItem(0);
                } else {
                    bannerViewPager.setCurrentItem(currentBannerPosition + 1);
                }
                autoScrollHandler.postDelayed(this, 3000);
            }
        };
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
        ImageView[] indicators = new ImageView[bannerItems.size()];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(requireContext());
            indicators[i].setImageResource(R.drawable.indicator_inactive);
            indicators[i].setLayoutParams(params);
            indicatorContainer.addView(indicators[i]);
        }

        updateIndicators(0);
    }

    private void updateIndicators(int position) {
        int childCount = indicatorContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) indicatorContainer.getChildAt(i);
            if (i == position) {
                imageView.setImageResource(R.drawable.indicator_active);
            } else {
                imageView.setImageResource(R.drawable.indicator_inactive);
            }
        }
    }

    private void setupCategories() {
        // Initialize categories list
        createCategoryItems();

        // Setup RecyclerView for categories
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerCategories.setLayoutManager(layoutManager);

        // Create and set adapter
        categoryAdapter = new CategoryAdapter(requireContext(), categoryItems, this);
        recyclerCategories.setAdapter(categoryAdapter);
    }

    private void createCategoryItems() {
        categoryItems.clear();

        // Add all categories
        categoryItems.add(new CategoryItem("all", "All", R.drawable.ic_category_all));
        categoryItems.add(new CategoryItem("smartphones", "Phones", R.drawable.smartphone));
        categoryItems.add(new CategoryItem("womens-dresses", "Fashion", R.drawable.fashionn));
        categoryItems.add(new CategoryItem("mens-shoes", "Shoes", R.drawable.shoes));
        categoryItems.add(new CategoryItem("fragrances", "Perfumes", R.drawable.perfume));
    }

    private void fetchAllProducts() {
        // Reset product list
        allProducts.clear();
        currentCategoryIndex = 0;
        isLoading = true;

        // Show progress bar, hide error layout
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        hideNetworkError();

        // Check for network connectivity
        if (!isNetworkAvailable()) {
            showNetworkError();
            isLoading = false;
            return;
        }

        // Start fetching products from first category
        fetchProductsByCategory(categories[currentCategoryIndex]);
    }

    private void fetchProductsByCategory(String category) {
        if (!isFragmentAttached) return;

        Log.d(TAG, "Fetching products for category: " + category);

        // Double-check network connectivity before API call
        if (!isNetworkAvailable()) {
            showNetworkError();
            isLoading = false;
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiCall = apiService.getProductsByCategory(category);

        apiCall.enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (!isFragmentAttached) {
                    Log.d(TAG, "Fragment is detached, ignoring API response");
                    return;
                }

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
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                if (!isFragmentAttached || call.isCanceled()) return;

                Log.e(TAG, "API call failed for " + category + ": " + t.getMessage());

                // Show error only if no products loaded yet
                if (allProducts.isEmpty()) {
                    showNetworkError();
                } else {
                    // If some products loaded, continue with what we have
                    Toast.makeText(getContext(), "Gagal memuat beberapa produk", Toast.LENGTH_SHORT).show();
                }

                isLoading = false;
            }
        });
    }

    private void fetchNextCategoryOrUpdate() {
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
                filterProductsByCategory(currentCategory);
            }
        }
    }

    private void filterProductsByCategory(String categoryId) {
        if (allProducts.isEmpty()) return;

        List<Product> filteredList;

        if (categoryId.equals("all")) {
            // Show all products from our 4 specific categories
            filteredList = new ArrayList<>(allProducts);
        } else {
            // Filter by selected category
            filteredList = allProducts.stream()
                    .filter(product -> product.getCategory().equals(categoryId))
                    .collect(Collectors.toList());
        }

        // Update adapter with filtered products using VIEW_TYPE_CARD
        productAdapter = new ProductAdapter(requireContext(), filteredList, ProductAdapter.VIEW_TYPE_CARD);
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

        textViewAll.setText(filteredList.isEmpty() ? "No products" : "View all");
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
        // Save current category
        currentCategory = categoryId;

        // Filter products by the selected category
        filterProductsByCategory(categoryId);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start auto-scroll when fragment is visible
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000);

        // Reload products if favoriteManager changed
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }

        // Update theme icon in case it was changed elsewhere
        if (themeToggleButton != null) {
            updateThemeToggleIcon();
        }
    }

    @Override
    public void onPause() {
        // Stop auto-scroll when fragment is not visible
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
        super.onPause();
    }

    private void retryLoading() {
        if (!isLoading) {
            hideNetworkError();
            fetchAllProducts();
        }
    }

    private void showNetworkError() {
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
}
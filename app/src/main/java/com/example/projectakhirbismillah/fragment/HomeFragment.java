package com.example.projectakhirbismillah.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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

    // Kategori yang ingin ditampilkan
    private final String[] categories = {
            "smartphones",     // Phone
            "womens-dresses",  // Fashion
            "mens-shoes",      // Shoes
            "fragrances"       // Parfum
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_home_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        bannerViewPager = view.findViewById(R.id.banner_viewpager);
        indicatorContainer = view.findViewById(R.id.indicator_container);
        recyclerProducts = view.findViewById(R.id.recycler_products);
        recyclerCategories = view.findViewById(R.id.recycler_categories);
        textViewAll = view.findViewById(R.id.text_view_all);

        // Setup carousel banner
        setupBannerCarousel();

        // Setup categories
        setupCategories();

        // Setup products grid
        setupProductsGrid();

        // Set click listener for "View all"
        textViewAll.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View all products", Toast.LENGTH_SHORT).show();
            // Implement navigation to all products screen if needed
        });

        // Fetch products from specific categories
        fetchAllProducts();
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

    private void setupProductsGrid() {
        // Setup RecyclerView with empty adapter first
        recyclerProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>(), true);
        recyclerProducts.setAdapter(productAdapter);
    }

    private void fetchAllProducts() {
        // Reset daftar produk
        allProducts.clear();
        currentCategoryIndex = 0;

        // Mulai mengambil produk dari kategori pertama
        fetchProductsByCategory(categories[currentCategoryIndex]);
    }

    private void fetchProductsByCategory(String category) {
        if (!isFragmentAttached) return;

        Log.d(TAG, "Fetching products for category: " + category);

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

                    // Tambahkan produk dari kategori ini ke list
                    allProducts.addAll(categoryProducts);

                    // Lanjutkan ke kategori berikutnya atau update UI jika semua kategori selesai
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
                fetchNextCategoryOrUpdate();
            }
        });
    }

    private void fetchNextCategoryOrUpdate() {
        currentCategoryIndex++;

        // Cek apakah masih ada kategori yang perlu diproses
        if (currentCategoryIndex < categories.length) {
            fetchProductsByCategory(categories[currentCategoryIndex]);
        } else {
            // Semua kategori selesai, update UI
            filterProductsByCategory(currentCategory);
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

        // Update adapter with filtered products
        productAdapter = new ProductAdapter(requireContext(), filteredList, true);
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
    }

    @Override
    public void onPause() {
        // Stop auto-scroll when fragment is not visible
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
        super.onPause();
    }
}
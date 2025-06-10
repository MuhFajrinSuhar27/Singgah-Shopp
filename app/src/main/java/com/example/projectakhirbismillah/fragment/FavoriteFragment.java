package com.example.projectakhirbismillah.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.adapter.ProductAdapter;
import com.example.projectakhirbismillah.model.Product;
import com.example.projectakhirbismillah.util.FavoriteManager;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment implements FavoriteManager.FavoriteChangeListener {
    private static final String TAG = "FavoriteFragment";

    private RecyclerView recyclerFavorites;
    private View emptyFavoritesLayout;
    private TextView textFavorites;
    private ProductAdapter productAdapter;
    private FavoriteManager favoriteManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_favorite_fragment, container, false);

        // Initialize views
        recyclerFavorites = view.findViewById(R.id.recycler_favorites);
        emptyFavoritesLayout = view.findViewById(R.id.empty_favorites_layout);
        textFavorites = view.findViewById(R.id.text_favorites);

        // PENTING: Pastikan RecyclerView selalu visible di awal untuk testing
        recyclerFavorites.setVisibility(View.VISIBLE);

        // Setup RecyclerView
        recyclerFavorites.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Get FavoriteManager instance and initialize it
        favoriteManager = FavoriteManager.getInstance();
        favoriteManager.initialize(requireContext());

        // Debug log untuk melihat jumlah favorit saat memuat fragment
        List<Product> currentFavorites = favoriteManager.getFavoriteProducts();
        Log.d(TAG, "Current favorites count in onCreateView: " + (currentFavorites != null ? currentFavorites.size() : 0));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated - updating favorites list");
        updateFavoritesList();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Re-initializing FavoriteManager");

        // Force reload data untuk memastikan sinkronisasi terbaru
        favoriteManager.initialize(requireContext());

        // Dapatkan data favorit dan log
        List<Product> currentFavorites = favoriteManager.getFavoriteProducts();
        Log.d(TAG, "onResume: Current favorites count: " +
                (currentFavorites != null ? currentFavorites.size() : 0));

        if (currentFavorites != null && currentFavorites.size() > 0) {
            for (Product p : currentFavorites) {
                Log.d(TAG, "Favorite product: " + p.getId() + " - " + p.getTitle());
            }
        }

        // Register as listener
        favoriteManager.addListener(this);

        // PENTING: Perbarui UI dengan data favorit terbaru SECARA LANGSUNG
        if (productAdapter != null && currentFavorites != null) {
            productAdapter.updateProducts(new ArrayList<>(currentFavorites));
            productAdapter.notifyDataSetChanged();
            Log.d(TAG, "Direct adapter update in onResume with " + currentFavorites.size() + " items");
        } else {
            // Gunakan metode normal jika adapter belum dibuat
            updateFavoritesList();
        }
    }

    @Override
    public void onPause() {
        // Unregister listener
        favoriteManager.removeListener(this);
        Log.d(TAG, "onPause: Unregistered as listener");
        super.onPause();
    }

    private void updateFavoritesList() {
        try {
            List<Product> favoriteProducts = favoriteManager.getFavoriteProducts();
            Log.d(TAG, "updateFavoritesList: Found " + favoriteProducts.size() + " favorite products");

            // Detail setiap favorit untuk debug
            if (favoriteProducts.size() > 0) {
                for (Product p : favoriteProducts) {
                    Log.d(TAG, "Favorite product: ID=" + p.getId() + ", Title=" + p.getTitle());
                }
            }

            // Tampilkan favorit jika ada, atau empty state jika tidak ada
            if (favoriteProducts.size() > 0) {
                showFavoritesList(favoriteProducts);
            } else {
                showEmptyState();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating favorites list: " + e.getMessage(), e);
            showEmptyState();
        }
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        if (emptyFavoritesLayout != null) {
            emptyFavoritesLayout.setVisibility(View.VISIBLE);
        }
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(View.GONE);
        }
    }

    private void showFavoritesList(List<Product> favoriteProducts) {
        try {
            Log.d(TAG, "Showing favorites list with " + favoriteProducts.size() + " items");

            if (emptyFavoritesLayout != null) {
                emptyFavoritesLayout.setVisibility(View.GONE);
            }

            if (recyclerFavorites != null) {
                recyclerFavorites.setVisibility(View.VISIBLE);

                if (productAdapter == null) {
                    productAdapter = new ProductAdapter(getContext(), favoriteProducts, ProductAdapter.VIEW_TYPE_CARD);
                    recyclerFavorites.setAdapter(productAdapter);
                    Log.d(TAG, "New adapter created with " + favoriteProducts.size() + " items");
                } else {
                    // PERBAIKAN: Clone list untuk menghindari masalah referensi
                    List<Product> productsCopy = new ArrayList<>(favoriteProducts);
                    productAdapter.updateProducts(productsCopy);
                    // FORCE REFRESH
                    productAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Existing adapter updated with " + productsCopy.size() + " items");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing favorites list: " + e.getMessage(), e);
        }
    }

    @Override
    public void onFavoriteChanged() {
        if (isAdded()) {
            Log.d(TAG, "onFavoriteChanged: Updating favorites list");
            updateFavoritesList();
        }
    }
}
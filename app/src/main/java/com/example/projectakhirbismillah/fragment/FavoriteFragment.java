package com.example.projectakhirbismillah.fragment;

import android.os.Bundle;
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

import java.util.List;

public class FavoriteFragment extends Fragment implements FavoriteManager.FavoriteChangeListener {

    private TextView textFavorites;
    private RecyclerView recyclerFavorites;
    private ProductAdapter productAdapter;
    private FavoriteManager favoriteManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_favorite_fragment, container, false);

        textFavorites = view.findViewById(R.id.text_favorites);
        recyclerFavorites = view.findViewById(R.id.recycler_favorites);
        recyclerFavorites.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize FavoriteManager
        favoriteManager = FavoriteManager.getInstance();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateFavoritesList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register as listener for favorite changes
        favoriteManager.addListener(this);
        updateFavoritesList();
    }

    @Override
    public void onPause() {
        favoriteManager.removeListener(this);
        super.onPause();
    }

    @Override
    public void onFavoriteChanged() {
        updateFavoritesList();
    }

    private void updateFavoritesList() {
        List<Product> favoriteProducts = favoriteManager.getFavoriteProducts();

        if (favoriteProducts.isEmpty()) {
            textFavorites.setVisibility(View.VISIBLE);
            recyclerFavorites.setVisibility(View.GONE);
        } else {
            textFavorites.setVisibility(View.GONE);
            recyclerFavorites.setVisibility(View.VISIBLE);

            if (productAdapter == null) {
                productAdapter = new ProductAdapter(getContext(), favoriteProducts, true);
                recyclerFavorites.setAdapter(productAdapter);
            } else {
                productAdapter.updateProducts(favoriteProducts);
            }
        }
    }
}
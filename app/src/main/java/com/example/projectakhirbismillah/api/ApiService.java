package com.example.projectakhirbismillah.api;

import com.example.projectakhirbismillah.model.ProductResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("products")
    Call<ProductResponse> getProducts();

    @GET("products/category/{category}")
    Call<ProductResponse> getProductsByCategory(@Path("category") String category);
}
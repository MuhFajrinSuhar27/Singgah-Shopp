package com.example.projectakhirbismillah.util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.projectakhirbismillah.model.CartItem;
import com.example.projectakhirbismillah.model.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MidtransHelper {
    private static final String TAG = "MidtransHelper";

    // SANDBOX environment credentials dari dashboard Anda
    private static final String CLIENT_KEY = "SB-Mid-client-mB9Dg-YzrY67szdZ";
    private static final String SERVER_KEY = "SB-Mid-server-_t_2ij7abUz-xdFjNzrXtb8-";
    private static final String MERCHANT_ID = "G281034065";


    private static final String SNAP_API_URL = "https://app.sandbox.midtrans.com/snap/v1/transactions";


    private static final String CURRENCY = "IDR";
    private static final double USD_TO_IDR_RATE = 15500.0;

    // Network client for API calls
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();


    public static void initMidtrans(Context context) {
        Log.d(TAG, "Initializing Midtrans with client key: " + CLIENT_KEY);
    }


    public interface PaymentUrlCallback {
        void onSuccess(String paymentUrl, String orderId);
        void onError(String message);
    }

    public static void getPaymentUrl(Context context, List<CartItem> cartItems, PaymentUrlCallback callback) {
        // Create unique order ID
        String orderId = "ORDER-" + UUID.randomUUID().toString().substring(0, 8);

        try {

            int totalAmountIDR = 0;
            JSONArray itemDetailsArray = new JSONArray();

            for (CartItem item : cartItems) {
                Product product = item.getProduct();
                int quantity = item.getQuantity();

                // Convert USD to IDR
                double priceInIDR = product.getPrice() * USD_TO_IDR_RATE;
                int roundedPrice = (int) Math.round(priceInIDR);
                int itemTotal = roundedPrice * quantity;

                // Add to total
                totalAmountIDR += itemTotal;

                // Create item detail
                JSONObject itemDetail = new JSONObject();
                itemDetail.put("id", product.getId());
                itemDetail.put("name", product.getTitle());
                itemDetail.put("price", roundedPrice);
                itemDetail.put("quantity", quantity);
                itemDetailsArray.put(itemDetail);
            }

            Log.d(TAG, "Order ID: " + orderId);
            Log.d(TAG, "Total amount (IDR): " + totalAmountIDR);
            Log.d(TAG, "Items: " + itemDetailsArray.toString(2));

            // Create transaction details
            JSONObject transactionDetails = new JSONObject();
            transactionDetails.put("order_id", orderId);
            transactionDetails.put("gross_amount", totalAmountIDR);

            // Create customer details
            JSONObject customerDetails = new JSONObject();
            customerDetails.put("first_name", "John");
            customerDetails.put("last_name", "Doe");
            customerDetails.put("email", "john.doe@example.com");
            customerDetails.put("phone", "08123456789");

            // Create final request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_details", transactionDetails);
            requestBody.put("customer_details", customerDetails);
            requestBody.put("item_details", itemDetailsArray);

            // Add callback URL
            JSONObject callbacks = new JSONObject();
            callbacks.put("finish", "projectakhirbismillah://callback");
            requestBody.put("callbacks", callbacks);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, requestBody.toString());


            String authString = SERVER_KEY + ":";
            String base64Auth = Base64.encodeToString(
                    authString.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP);

            // Build request with headers
            Request request = new Request.Builder()
                    .url(SNAP_API_URL)
                    .addHeader("Authorization", "Basic " + base64Auth)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            Log.d(TAG, "Making API call to: " + SNAP_API_URL);
            Log.d(TAG, "Authorization: Basic " + base64Auth);

            // Execute request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String errorMessage = "Network error: " + e.getMessage();
                    Log.e(TAG, errorMessage, e);
                    callback.onError(errorMessage);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = "";
                    try {
                        responseBody = response.body().string();
                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, "Response body: " + responseBody);

                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (response.isSuccessful()) {
                            // Sukses! Buat URL redirect
                            if (jsonResponse.has("redirect_url")) {
                                String redirectUrl = jsonResponse.getString("redirect_url");
                                callback.onSuccess(redirectUrl, orderId);
                            } else if (jsonResponse.has("token")) {
                                String token = jsonResponse.getString("token");
                                String redirectUrl = "https://app.sandbox.midtrans.com/snap/v3/redirection/" + token;
                                callback.onSuccess(redirectUrl, orderId);
                            } else {
                                callback.onError("Invalid response format from payment gateway");
                            }
                        } else {
                            // Error - parse pesan error
                            String errorMessage = "Payment error: HTTP " + response.code();
                            if (jsonResponse.has("error_messages")) {
                                errorMessage = jsonResponse.getJSONArray("error_messages").toString();
                                Log.e(TAG, "API Error: " + errorMessage);
                            }
                            callback.onError(errorMessage);
                        }
                    } catch (JSONException e) {
                        String errorMessage = "Error parsing response: " + e.getMessage() + "\nResponse was: " + responseBody;
                        Log.e(TAG, errorMessage, e);
                        callback.onError(errorMessage);
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });

        } catch (Exception e) {
            String errorMessage = "Error preparing payment: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            callback.onError(errorMessage);
        }
    }

    /**
     * Test method to verify API credentials
     */
    public static void testMidtransConnection(PaymentUrlCallback callback) {
        try {
            // Create simple transaction for testing
            JSONObject transactionDetails = new JSONObject();
            transactionDetails.put("order_id", "TEST-" + System.currentTimeMillis());
            transactionDetails.put("gross_amount", 10000);

            JSONObject requestBody = new JSONObject();
            requestBody.put("transaction_details", transactionDetails);

            // Create OkHttp request body
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, requestBody.toString());

            // Create Basic Auth header - PENTING: Encode dengan benar
            String credentials = SERVER_KEY + ":";
            String base64Auth = android.util.Base64.encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP);

            // Build request with headers
            Request request = new Request.Builder()
                    .url(SNAP_API_URL)
                    .addHeader("Authorization", "Basic " + base64Auth)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            Log.d(TAG, "Testing API connection: " + SNAP_API_URL);
            Log.d(TAG, "Using Authorization: Basic " + base64Auth);

            // Execute request synchronously for immediate test result
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Connection test failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Test response code: " + response.code());
                    Log.d(TAG, "Test response body: " + responseBody);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (response.isSuccessful()) {
                            callback.onSuccess("Connection test successful!", "TEST-SUCCESS");
                        } else {
                            String error = "Connection test failed with HTTP " + response.code();
                            if (jsonResponse.has("error_messages")) {
                                error = jsonResponse.getJSONArray("error_messages").toString();
                            }
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        callback.onError("Error parsing test response: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Error setting up connection test: " + e.getMessage());
        }
    }
}
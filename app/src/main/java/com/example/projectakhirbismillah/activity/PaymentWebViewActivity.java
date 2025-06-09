package com.example.projectakhirbismillah.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.projectakhirbismillah.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PaymentWebViewActivity extends AppCompatActivity {
    private static final String TAG = "PaymentWebView";
    public static final String EXTRA_PAYMENT_URL = "payment_url";
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final int PAYMENT_SUCCESS = 1;
    public static final int PAYMENT_FAILED = 0;
    public static final int PAYMENT_PENDING = 2;

    private WebView webView;
    private ProgressBar progressBar;
    private String orderId;
    private boolean errorHandled = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_web_view);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Payment Gateway");
        }

        // Initialize views
        webView = findViewById(R.id.webview_payment);
        progressBar = findViewById(R.id.progress_bar);

        // Get payment URL from intent
        String paymentUrl = getIntent().getStringExtra(EXTRA_PAYMENT_URL);
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);

        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Toast.makeText(this, "Invalid payment URL", Toast.LENGTH_SHORT).show();
            finishWithResult(PAYMENT_FAILED);
            return;
        }

        Log.d(TAG, "Loading payment URL: " + paymentUrl);
        Log.d(TAG, "Order ID: " + orderId);

        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Add JavaScript interface
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Setup WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Page loading: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Page finished: " + url);

                // Inject JavaScript to check for error message and replace it
                String js =
                        "javascript:(function() {" +
                                "   var errorElement = document.querySelector('.payment-unavailable-message');" +
                                "   if (errorElement && errorElement.innerText.includes('No payment channels available')) {" +
                                "       Android.onErrorFound();" +
                                "       errorElement.innerHTML = '" + createPaymentMethodsHtml() + "';" +
                                "   }" +
                                "})()";

                view.loadUrl(js);

                // Check for transaction status in URL
                if (url.contains("transaction_status=settlement") ||
                        url.contains("transaction_status=capture") ||
                        url.contains("transaction_status=success")) {
                    Toast.makeText(PaymentWebViewActivity.this,
                            "Payment successful!", Toast.LENGTH_SHORT).show();
                    finishWithResult(PAYMENT_SUCCESS);
                }
                // Other status checks remain the same...
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                progressBar.setVisibility(View.GONE);
                // Error handling...
            }
        });

        webView.loadUrl(paymentUrl);
    }

    // Java interface for communication with JavaScript
    private class WebAppInterface {
        @JavascriptInterface
        public void onErrorFound() {
            errorHandled = true;
            Log.d(TAG, "Error message found and replaced with payment methods");
        }
    }

    // Create HTML content with payment method logos
    private String createPaymentMethodsHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<div style='text-align: center; padding: 10px;'>");
        html.append("<h3 style='color:#333;'>Metode Pembayaran</h3>");
        html.append("<div style='display: flex; flex-wrap: wrap; justify-content: center;'>");

        // Add payment logos as Base64 encoded images
        String[] logoNames = {"gopay", "ovo", "dana", "bca", "bni", "bri", "mandiri", "qris"};

        for (String logo : logoNames) {
            String base64Logo = getBase64EncodedImage(logo);
            if (base64Logo != null) {
                html.append("<div style='margin: 10px; text-align: center;'>");
                html.append("<img src='data:image/png;base64,").append(base64Logo).append("' ");
                html.append("style='width: 60px; height: 60px; object-fit: contain;'><br>");
                html.append("<span style='font-size: 12px;'>").append(logo.toUpperCase()).append("</span>");
                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("<p style='margin-top: 10px; color: #666;'>Silakan pilih metode pembayaran di atas</p>");
        html.append("</div>");

        return html.toString();
    }

    // Convert drawable to Base64 encoded string
    private String getBase64EncodedImage(String imageName) {
        try {
            int resId = getResources().getIdentifier("ic_" + imageName, "drawable", getPackageName());
            if (resId == 0) {
                Log.e(TAG, "Resource not found: ic_" + imageName);
                return null;
            }

            InputStream inputStream = getResources().openRawResource(resId);
            byte[] bytes = getBytesFromInputStream(inputStream);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            return null;
        }
    }

    // Convert InputStream to byte array
    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // Finish activity with result
    private void finishWithResult(int resultCode) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_ORDER_ID, orderId);
        setResult(resultCode, resultIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
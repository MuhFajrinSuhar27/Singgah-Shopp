package com.example.projectakhirbismillah.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
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

        // Configure WebView with improved settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Add console logging for debugging
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebView Console", consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });

        // Setup WebViewClient with improved URL handling
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Make sure WebView handles all URLs
                Log.d(TAG, "Navigating to URL: " + url);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Page loading: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Page finished: " + url);

                // Check if payment is complete
                if (url.contains("transaction_status=settlement") ||
                        url.contains("transaction_status=capture") ||
                        url.contains("transaction_status=success")) {

                    Toast.makeText(PaymentWebViewActivity.this,
                            "Payment successful!", Toast.LENGTH_SHORT).show();
                    finishWithResult(PAYMENT_SUCCESS);

                } else if (url.contains("transaction_status=pending")) {
                    Toast.makeText(PaymentWebViewActivity.this,
                            "Payment pending! Please complete your payment.",
                            Toast.LENGTH_SHORT).show();
                    finishWithResult(PAYMENT_PENDING);

                } else if (url.contains("transaction_status=deny") ||
                        url.contains("transaction_status=cancel") ||
                        url.contains("transaction_status=expire") ||
                        url.contains("transaction_status=failure")) {

                    Toast.makeText(PaymentWebViewActivity.this,
                            "Payment failed or cancelled", Toast.LENGTH_SHORT).show();
                    finishWithResult(PAYMENT_FAILED);
                }

                // Debug: inspect HTML content
                view.evaluateJavascript(
                        "(function() { return document.documentElement.outerHTML; })();",
                        value -> {
                            if (value != null && !value.equals("null")) {
                                String html = value.substring(1, value.length() - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\n", "\n");
                                Log.d(TAG, "Page HTML (partial): " +
                                        (html.length() > 500 ? html.substring(0, 500) + "..." : html));
                            }
                        }
                );
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "WebView error: " + error.toString() + " for URL: " + request.getUrl());

                Toast.makeText(PaymentWebViewActivity.this,
                        "Error loading payment page. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Load Midtrans URL
        webView.loadUrl(paymentUrl);
    }

    private void finishWithResult(int resultCode) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_ORDER_ID, orderId);
        setResult(resultCode, resultIntent);

        // Add slight delay before finishing to ensure toast is shown
        webView.postDelayed(() -> finish(), 1500);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Confirm if user wants to cancel payment
            Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
            finishWithResult(PAYMENT_FAILED);
        }
    }
}
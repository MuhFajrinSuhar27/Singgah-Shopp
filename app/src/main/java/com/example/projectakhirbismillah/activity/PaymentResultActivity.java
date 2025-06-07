package com.example.projectakhirbismillah.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectakhirbismillah.MainActivity;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.util.CartManager;

public class PaymentResultActivity extends AppCompatActivity {

    private static final String TAG = "PaymentResultActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_result);

        TextView textStatus = findViewById(R.id.text_payment_status);
        Button buttonContinue = findViewById(R.id.button_continue);

        // Get intent data
        Intent intent = getIntent();
        Uri data = intent.getData();

        // Handle deep link
        if (data != null) {
            Log.d(TAG, "Received deep link: " + data.toString());

            String transactionStatus = data.getQueryParameter("transaction_status");
            String orderId = data.getQueryParameter("order_id");

            // Handle status
            if ("settlement".equals(transactionStatus) ||
                    "capture".equals(transactionStatus) ||
                    "success".equals(transactionStatus)) {

                textStatus.setText("Pembayaran Berhasil!\nOrder ID: " + orderId);

                // Clear cart
                CartManager.getInstance().clearCart();

            } else if ("pending".equals(transactionStatus)) {
                textStatus.setText("Pembayaran Tertunda\nOrder ID: " + orderId +
                        "\n\nSilakan selesaikan pembayaran Anda.");
            } else {
                textStatus.setText("Pembayaran Gagal atau Dibatalkan\nOrder ID: " + orderId);
            }
        } else {
            textStatus.setText("Terjadi error dalam proses pembayaran");
        }

        // Continue button returns to main activity
        buttonContinue.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });
    }
}
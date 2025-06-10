package com.example.projectakhirbismillah;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectakhirbismillah.database.DatabaseHelper;
import com.example.projectakhirbismillah.databinding.ActivityLoginBinding;
import com.example.projectakhirbismillah.util.SessionManager;
import com.example.projectakhirbismillah.util.ThreadUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private ExecutorService executor;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Log untuk debugging
        Log.d(TAG, "onCreate called");

        // Inisialisasi executor dan handler untuk background thread
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        try {
            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize database helper
            databaseHelper = new DatabaseHelper(this);

            // Initialize session manager
            sessionManager = new SessionManager(this);

            // Check if user is already logged in - lakukan di background
            checkLoginStatusAsync();

            // Set up listeners
            binding.buttonLogin.setOnClickListener(v -> login());
            binding.textSignup.setOnClickListener(v -> navigateToRegister());
            binding.textForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service untuk mencegah memory leak
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Check login status di background thread
     */
    private void checkLoginStatusAsync() {
        // Tampilkan loading indicator jika perlu
        binding.progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            // Operasi di background thread
            final boolean isLoggedIn = sessionManager.isLoggedIn();

            // Kembali ke UI thread
            handler.post(() -> {
                binding.progressBar.setVisibility(View.GONE);
                if (isLoggedIn) {
                    Log.d(TAG, "User already logged in, navigating to MainActivity");
                    navigateToMainActivity();
                    finish();
                }
            });
        });
    }

    /**
     * Proses login di background thread
     */
    private void login() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        Log.d(TAG, "Attempting login with email: " + email);

        // Validasi input di UI thread
        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError("Please enter your email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError("Please enter your password");
            return;
        }

        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);

        // Tampilkan progress indicator
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonLogin.setEnabled(false);

        // Jalankan proses login di background thread
        executor.execute(() -> {
            boolean loginSuccess = false;
            String userId = null;
            String userName = null;
            String userEmail = null;
            Exception exception = null;

            try {
                // Proses autentikasi di background thread
                loginSuccess = databaseHelper.checkUser(email, password);

                if (loginSuccess) {
                    // Fetch user details
                    Cursor cursor = databaseHelper.getUserByEmail(email);
                    if (cursor != null && cursor.moveToFirst()) {
                        userId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                        userName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
                        userEmail = email;
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during login: " + e.getMessage(), e);
                exception = e;
            }

            // Final variables untuk lambda
            final boolean finalLoginSuccess = loginSuccess;
            final String finalUserId = userId;
            final String finalUserName = userName;
            final String finalUserEmail = userEmail;
            final Exception finalException = exception;

            // Kembali ke UI thread untuk memperbarui UI
            handler.post(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonLogin.setEnabled(true);

                if (finalException != null) {
                    Toast.makeText(LoginActivity.this,
                            "Login error: " + finalException.getMessage(),
                            Toast.LENGTH_SHORT).show();
                } else if (finalLoginSuccess) {
                    Log.d(TAG, "Login successful");

                    // Simpan sesi login di background thread lagi untuk menghindari blocking UI
                    ThreadUtils.runInBackground(() -> {
                        sessionManager.createLoginSession(finalUserId, finalUserName, finalUserEmail);
                        return null;
                    }, new ThreadUtils.Callback<Void>() {
                        @Override
                        public void onComplete(Void result) {
                            // Setelah sesi berhasil disimpan, navigasi ke MainActivity
                            navigateToMainActivity();
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(LoginActivity.this,
                                    "Error saving session: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.d(TAG, "Login failed - invalid credentials");
                    Toast.makeText(LoginActivity.this, "Invalid email or password",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void showForgotPasswordDialog() {
        Toast.makeText(this, "Password reset functionality would be implemented here", Toast.LENGTH_SHORT).show();
    }
}
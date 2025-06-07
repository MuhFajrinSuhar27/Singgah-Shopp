package com.example.projectakhirbismillah;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectakhirbismillah.database.DatabaseHelper;
import com.example.projectakhirbismillah.databinding.ActivityLoginBinding;
import com.example.projectakhirbismillah.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Log untuk debugging
        Log.d(TAG, "onCreate called");

        try {
            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Initialize database helper
            databaseHelper = new DatabaseHelper(this);

            // Initialize session manager
            sessionManager = new SessionManager(this);

            // Check if user is already logged in
            if (sessionManager.isLoggedIn()) {
                Log.d(TAG, "User already logged in, navigating to MainActivity");
                navigateToMainActivity();
                finish();
                return;
            }

            // Set up listeners
            binding.buttonLogin.setOnClickListener(v -> login());
            binding.textSignup.setOnClickListener(v -> navigateToRegister());
            binding.textForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void login() {
        try {
            String email = binding.inputEmail.getText().toString().trim();
            String password = binding.inputPassword.getText().toString().trim();

            Log.d(TAG, "Attempting login with email: " + email);

            // Validate inputs
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


            if (databaseHelper.checkUser(email, password)) {
                Log.d(TAG, "Login successful");

                // Fetch user details
                Cursor cursor = databaseHelper.getUserByEmail(email);
                if (cursor != null && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));

                    sessionManager.createLoginSession(id, name, email);

                    cursor.close();

                    navigateToMainActivity();
                    finish();
                }
            } else {
                Log.d(TAG, "Login failed - invalid credentials");
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during login: " + e.getMessage(), e);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
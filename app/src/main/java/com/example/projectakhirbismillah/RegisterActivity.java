package com.example.projectakhirbismillah;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectakhirbismillah.database.DatabaseHelper;
import com.example.projectakhirbismillah.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Set up listeners
        binding.buttonRegister.setOnClickListener(v -> registerUser());
        binding.textLogin.setOnClickListener(v -> finish()); // Go back to login

    }

    private void registerUser() {
        // Get input values
        String name = binding.inputName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String phone = binding.inputPhone.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();
        String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();
        boolean termsAccepted = binding.checkboxTerms.isChecked();

        // Clear previous errors
        binding.inputLayoutName.setError(null);
        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPhone.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.inputLayoutConfirmPassword.setError(null);

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            binding.inputLayoutName.setError("Please enter your name");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError("Please enter your email");
            return;
        }

        if (!isValidEmail(email)) {
            binding.inputLayoutEmail.setError("Please enter a valid email");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            binding.inputLayoutPhone.setError("Please enter your phone number");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError("Please enter a password");
            return;
        }

        if (password.length() < 6) {
            binding.inputLayoutPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.inputLayoutConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (!termsAccepted) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if email already exists
        if (databaseHelper.checkUserEmail(email)) {
            binding.inputLayoutEmail.setError("Email already exists");
            return;
        }

        // Save user to database
        long id = databaseHelper.addUser(name, email, password, phone);
        if (id > 0) {
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
            finish(); // Go back to login
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
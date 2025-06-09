package com.example.projectakhirbismillah.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.projectakhirbismillah.LoginActivity;
import com.example.projectakhirbismillah.MainActivity;
import com.example.projectakhirbismillah.R;
import com.example.projectakhirbismillah.util.SessionManager;

public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private TextView memberSinceDate;
    private LinearLayout logoutSection;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile_fragment, container, false);


        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        memberSinceDate = view.findViewById(R.id.member_since_date);


        sessionManager = new SessionManager(requireContext());

        // Set user data from session
        profileName.setText(sessionManager.getUserName());
        profileEmail.setText(sessionManager.getUserEmail());


        memberSinceDate.setText("June 2023"); // Modify according to your needs

        // Setup logout button
        LinearLayout logoutSection = view.findViewById(R.id.layout_logout);
        if (logoutSection != null) {
            logoutSection.setOnClickListener(v -> handleLogout());
        }

        // Setup other profile options
        setupProfileMenuOptions(view);

        return view;
    }

    private void handleLogout() {
        try {
            // Gunakan sessionManager yang sudah diinisialisasi langsung
            sessionManager.logoutUser();

            // Redirect ke LoginActivity
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error during logout: " + e.getMessage());
            Toast.makeText(getContext(), "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupProfileMenuOptions(View view) {
        // Edit Profile
        LinearLayout editProfileSection = view.findViewById(R.id.layout_edit_profile);
        if (editProfileSection != null) {
            editProfileSection.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show());
        }

        // My Orders
        LinearLayout myOrdersSection = view.findViewById(R.id.layout_my_orders);
        if (myOrdersSection != null) {
            myOrdersSection.setOnClickListener(v ->
                    Toast.makeText(getContext(), "My Orders clicked", Toast.LENGTH_SHORT).show());
        }

        // Settings
        LinearLayout settingsSection = view.findViewById(R.id.layout_settings);
        if (settingsSection != null) {
            settingsSection.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show());
        }
    }
}
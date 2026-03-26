package com.musicapp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    private static final String PREF_NAME = "MusicApp";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(PREF_NAME, getActivity().MODE_PRIVATE);
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        String role = sharedPreferences.getString(KEY_ROLE, "ROLE_USER");
        
        TextView textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewUsername.setText("Username: " + username);
        
        setupAdminButton(view, role);
        setupLogoutButton(view, sharedPreferences);
        
        return view;
    }

    private void setupAdminButton(View view, String role) {
        Button buttonAdmin = view.findViewById(R.id.buttonAdmin);
        if (buttonAdmin != null) {
            if (ROLE_ADMIN.equals(role)) {
                buttonAdmin.setVisibility(View.VISIBLE);
                buttonAdmin.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), AdminActivity.class);
                    startActivity(intent);
                });
            } else {
                buttonAdmin.setVisibility(View.GONE);
            }
        }
    }

    private void setupLogoutButton(View view, SharedPreferences sharedPreferences) {
        Button buttonLogout = view.findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
    }
}

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
    private TextView textViewUsername;
    private Button buttonLogout;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        textViewUsername = view.findViewById(R.id.textViewUsername);
        buttonLogout = view.findViewById(R.id.buttonLogout);
        
        sharedPreferences = getActivity().getSharedPreferences("MusicApp", getActivity().MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "");
        String role = sharedPreferences.getString("role", "ROLE_USER");
        textViewUsername.setText("Username: " + username);
        
        // Chỉ hiển thị nút Admin nếu là admin
        Button buttonAdmin = view.findViewById(R.id.buttonAdmin);
        if (buttonAdmin != null) {
            if ("ROLE_ADMIN".equals(role)) {
                buttonAdmin.setVisibility(View.VISIBLE);
                buttonAdmin.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), AdminActivity.class);
                    startActivity(intent);
                });
            } else {
                buttonAdmin.setVisibility(View.GONE);
            }
        }
        
        buttonLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
        
        return view;
    }
}

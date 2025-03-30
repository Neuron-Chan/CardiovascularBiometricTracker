package com.example.physioscanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenu extends AppCompatActivity {

    private static final String TAG = "MainMenu";

    // Firebase Authentication
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // UI Elements
    TextView userDetails, heartRateValue, ecgValue, ppgValue, temperatureValue;
    CardView cardHeartRate, cardECG, card_ppg, cardTemperature, cardAllSensors, cardCloudDatabase;
    Button logoutButton, signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        userDetails       = findViewById(R.id.main_title);
        signInButton      = findViewById(R.id.signin);
        logoutButton      = findViewById(R.id.logout);
        heartRateValue    = findViewById(R.id.heart_rate_value);
        ecgValue          = findViewById(R.id.ecg_value);
        ppgValue          = findViewById(R.id.ppg_value);
        temperatureValue  = findViewById(R.id.temperature_value);

        // Initialize CardViews
        cardHeartRate     = findViewById(R.id.card_heart_rate);
        cardECG           = findViewById(R.id.card_ecg);
        card_ppg          = findViewById(R.id.card_ppg);
        cardTemperature   = findViewById(R.id.card_temperature);
        cardAllSensors    = findViewById(R.id.card_all_sensors);
        cardCloudDatabase = findViewById(R.id.card_cloud_database);

        // Check if user is logged in
        if (currentUser == null) {
            redirectToLogin();
        } else {
            userDetails.setText("Logged in as: " + currentUser.getEmail());
        }

        // Open single-ECG screen
        cardECG.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, MainActivity.class);
            intent.putExtra("dataType", "ecg");
            startActivity(intent);
        });

        // PPG screen
        card_ppg.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, PPGActivity.class);
            startActivity(intent);
        });

        // Temperature screen
        cardTemperature.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, TemperatureActivity.class);
            startActivity(intent);
        });

        // All Sensors
        cardAllSensors.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, AllSensorsActivity.class);
            startActivity(intent);
        });

        // Cloud Database
        cardCloudDatabase.setOnClickListener(v -> {
            if (currentUser == null) {
                redirectToLogin();
            } else {
                Intent intent = new Intent(MainMenu.this, CloudDatabaseActivity.class);
                startActivity(intent);
            }
        });

        // Sign-In button
        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, Login.class);
            startActivity(intent);
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            redirectToLogin();
        });
    }

    private void redirectToLogin() {
        Log.d(TAG, "User not logged in, redirecting to Login screen.");
        Intent intent = new Intent(MainMenu.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
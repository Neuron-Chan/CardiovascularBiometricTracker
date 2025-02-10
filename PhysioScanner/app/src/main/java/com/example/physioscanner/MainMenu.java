package com.example.physioscanner;

import android.content.Intent;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenu extends AppCompatActivity {

    // TextViews for the biometric readings (if needed)
    TextView heartRateValue;
    TextView ecgValue;
    TextView ppgValue;
    TextView temperatureValue;

    // CardViews for each sensor screen
    CardView cardHeartRate;
    CardView cardECG;
    CardView card_ppg;  // Renamed variable for clarity (this is our PPG card)
    CardView cardTemperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Initialize TextViews (if displaying current readings)
        heartRateValue = findViewById(R.id.heart_rate_value);
        ecgValue = findViewById(R.id.ecg_value);
        ppgValue = findViewById(R.id.ppg_value);
        temperatureValue = findViewById(R.id.temperature_value);

        // Initialize CardViews
        cardHeartRate = findViewById(R.id.card_heart_rate);
        cardECG = findViewById(R.id.card_ecg);
        card_ppg = findViewById(R.id.card_ppg); // This is our PPG card
        cardTemperature = findViewById(R.id.card_temperature);

        // Set click listeners for navigation

        // Launch live ECG view (MainActivity in ECG mode)
        cardECG.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, MainActivity.class);
            intent.putExtra("dataType", "ecg");
            startActivity(intent);
        });

        // Launch live PPG view (MainActivity in PPG mode)
        card_ppg.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, MainActivity.class);
            intent.putExtra("dataType", "ppg");
            startActivity(intent);
        });

        // (Optionally, add listeners for the other cards if needed.)
    }
}

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
    CardView card_ppg;
    CardView cardTemperature;
    CardView cardAllSensors; // NEW CARD

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
        card_ppg = findViewById(R.id.card_ppg);
        cardTemperature = findViewById(R.id.card_temperature);
        cardAllSensors = findViewById(R.id.card_all_sensors); // NEW

        // Set click listeners for navigation

        // Launch live ECG view (MainActivity is for ECG)
        cardECG.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, MainActivity.class);
            intent.putExtra("dataType", "ecg");
            startActivity(intent);
        });

        // Launch live PPG view (Use PPGActivity for PPG)
        card_ppg.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, PPGActivity.class);
            startActivity(intent);
        });

        // Launch Temperature view (TemperatureActivity)
        cardTemperature.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, TemperatureActivity.class);
            startActivity(intent);
        });

        // Launch All Sensors Activity
        cardAllSensors.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, AllSensorsActivity.class);
            startActivity(intent);
        });
    }
}

package com.example.physioscanner;

import android.content.Intent;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenu extends AppCompatActivity {

    // TextViews for the biometric readings
    TextView heartRateValue;
    TextView ecgValue;
    TextView ppgValue;
    TextView temperatureValue;

    // CardViews
    CardView cardHeartRate;
    CardView cardECG;
    CardView cardBloodPressure;
    CardView cardTemperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Initializing TextViews for values
        heartRateValue = findViewById(R.id.heart_rate_value);
        ecgValue = findViewById(R.id.ecg_value);
        ppgValue = findViewById(R.id.ppg_value);
        temperatureValue = findViewById(R.id.temperature_value);

        // Initializing CardViews
        cardHeartRate = findViewById(R.id.card_heart_rate);
        cardECG = findViewById(R.id.card_ecg);
        cardBloodPressure = findViewById(R.id.card_ppg);
        cardTemperature = findViewById(R.id.card_temperature);

//        // Set onClickListeners for each CardView
//        cardHeartRate.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, HeartRate.class);
//            startActivity(intent);
//        });

        cardECG.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, MainActivity.class);
            startActivity(intent);
        });

//        cardBloodPressure.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, PPG.class);
//            startActivity(intent);
//        });
//
//        cardTemperature.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, Temperature.class);
//            startActivity(intent);
//        });
    }
}

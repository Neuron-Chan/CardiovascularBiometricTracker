package com.example.cbt;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    // TextViews for the biometric readings
    TextView heartRateValue;
    TextView ecgValue;
    TextView bloodPressureValue;
    TextView temperatureValue;

    // ProgressBars for the readings
    ProgressBar heartRateProgress;
    ProgressBar ecgProgress;
    ProgressBar bloodPressureProgress;
    ProgressBar temperatureProgress;

    // Button to save data
    Button saveButton;

    // CardViews
    CardView cardHeartRate;
    CardView cardECG;
    CardView cardBloodPressure;
    CardView cardTemperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing TextViews for values
        heartRateValue = findViewById(R.id.heartRateValue);
        ecgValue = findViewById(R.id.ecgValue);
        bloodPressureValue = findViewById(R.id.bloodPressureValue);
        temperatureValue = findViewById(R.id.temperatureValue);

        // Initializing ProgressBars
        heartRateProgress = findViewById(R.id.heartRateProgress);
        ecgProgress = findViewById(R.id.ecgProgress);
        bloodPressureProgress = findViewById(R.id.bloodPressureProgress);
        temperatureProgress = findViewById(R.id.temperatureProgress);

        // Initializing Buttons
        saveButton = findViewById(R.id.saveButton);

        // Initializing CardViews (no need to cast as TextViews)
        cardHeartRate = findViewById(R.id.cardHeartRate);
        cardECG = findViewById(R.id.cardECG);
        cardBloodPressure = findViewById(R.id.cardBloodPressure);
        cardTemperature = findViewById(R.id.cardTemperature);

        // Sample data to set
        heartRateValue.setText("75 BPM");
        ecgValue.setText("Normal");
        bloodPressureValue.setText("120/80 mmHg");
        temperatureValue.setText("36°C");

        // Set the ProgressBars (example: values can be dynamic based on your sensor data)
        heartRateProgress.setProgress(75);
        ecgProgress.setProgress(60);
        bloodPressureProgress.setProgress(120); // Sample value
        temperatureProgress.setProgress(36); // Sample value

        // Set the button click listener if needed
        saveButton.setOnClickListener(view -> {
            // Save data logic here
        });
    }
}

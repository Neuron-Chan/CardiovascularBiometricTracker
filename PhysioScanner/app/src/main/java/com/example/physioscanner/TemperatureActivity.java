package com.example.physioscanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class TemperatureActivity extends AppCompatActivity {

    private String socketUrl = "http://172.20.10.2:5000";
    private Socket mSocket;
    private static final String TAG = "TemperatureActivity";
    private TextView timestampText, tempCText, tempFText;
    private Button viewDatabaseButton, homeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        timestampText = findViewById(R.id.timestamp_text);
        tempCText     = findViewById(R.id.temp_c_text);
        tempFText     = findViewById(R.id.temp_f_text);
        viewDatabaseButton = findViewById(R.id.view_database_button);
        homeButton        = findViewById(R.id.home_button);

        viewDatabaseButton.setText("View Temperature Database");
        viewDatabaseButton.setOnClickListener(v -> {
            mSocket.disconnect();
            Intent intent = new Intent(TemperatureActivity.this, DatabaseViewActivity.class);
            intent.putExtra("dataType", "temp");
            startActivity(intent);
        });

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(TemperatureActivity.this, MainMenu.class);
            startActivity(intent);
        });

        try {
            mSocket = IO.socket(socketUrl);
            Log.d(TAG, "TemperatureActivity socket created with URL: " + socketUrl);
            mSocket.connect();
            mSocket.on("tmp102_data", onNewTempData);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSocket != null && !mSocket.connected()) {
            mSocket.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off("tmp102_data", onNewTempData);
        }
    }

    private final Emitter.Listener onNewTempData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String timestamp = data.getString("timestamp");
            double tempC = data.getDouble("temperature_c");
            double tempF = data.has("temperature_f")
                    ? data.getDouble("temperature_f")
                    : tempC * 9.0 / 5.0 + 32.0;
            timestampText.setText("Timestamp: " + timestamp);
            tempCText.setText("Temperature (°C): " + tempC);
            tempFText.setText("Temperature (°F): " + tempF);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing temperature data", e);
        }
    });
}

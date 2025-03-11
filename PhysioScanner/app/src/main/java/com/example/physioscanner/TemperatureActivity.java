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

    private static final String SOCKET_URL = "http://10.100.242.3:5000";
    private static final String TAG = "TemperatureActivity";
    private TextView timestampText, tempCText, tempFText;
    private Button viewDatabaseButton, homeButton;
    private Socket mSocket;

    {
        try {
            mSocket = IO.socket(SOCKET_URL);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        timestampText = findViewById(R.id.timestamp_text);
        tempCText = findViewById(R.id.temp_c_text);
        tempFText = findViewById(R.id.temp_f_text);
        viewDatabaseButton = findViewById(R.id.view_database_button);
        homeButton = findViewById(R.id.home_button);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSocket.connect();
        // Subscribe to the "tmp102_data" event.
        mSocket.on("tmp102_data", onNewTempData);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSocket.disconnect();
        mSocket.off("tmp102_data", onNewTempData);
    }

    private Emitter.Listener onNewTempData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            runOnUiThread(() -> {
                try {
                    Log.d(TAG, "Received temperature data: " + data.toString());
                    String timestamp = data.getString("timestamp");
                    double tempC = data.getDouble("temperature_c");
                    double tempF;
                    // Use temperature_f if available; otherwise calculate it.
                    if (data.has("temperature_f")) {
                        tempF = data.getDouble("temperature_f");
                    } else {
                        tempF = tempC * 9.0 / 5.0 + 32.0;
                    }
                    timestampText.setText("Timestamp: " + timestamp);
                    tempCText.setText("Temperature (°C): " + tempC);
                    tempFText.setText("Temperature (°F): " + tempF);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing temperature data", e);
                }
            });
        }
    };
}

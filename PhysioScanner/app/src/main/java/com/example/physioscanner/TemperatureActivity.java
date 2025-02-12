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

    // Updated IP address
    private static final String SOCKET_URL = "http://192.168.2.94:5000";
    private TextView timestampText, tempValueText;
    private Button viewDatabaseButton, homeButton;
    private Socket mSocket;

    {
        try {
            mSocket = IO.socket(SOCKET_URL);
        } catch (URISyntaxException e) {
            Log.e("TempActivity", "Error creating socket", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use a layout that does NOT contain any chart view.
        setContentView(R.layout.activity_temperature);

        timestampText = findViewById(R.id.timestamp_text);
        tempValueText = findViewById(R.id.temp_value_text);
        viewDatabaseButton = findViewById(R.id.view_database_button);
        homeButton = findViewById(R.id.home_button);

        // Set the button text for clarity
        viewDatabaseButton.setText("View Temperature Database");

        viewDatabaseButton.setOnClickListener(v -> {
            mSocket.disconnect();
            // Launch the database view for temperature data
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
        mSocket.on("temp_data", onNewTempData);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSocket.disconnect();
        mSocket.off("temp_data", onNewTempData);
    }

    private Emitter.Listener onNewTempData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            runOnUiThread(() -> {
                try {
                    String timestamp = data.getString("timestamp");
                    double tempC = data.getDouble("temperature_c");
                    timestampText.setText("Timestamp: " + timestamp);
                    tempValueText.setText("Temperature: " + tempC + " °C");
                } catch (JSONException e) {
                    Log.e("TempActivity", "Error parsing temperature data", e);
                }
            });
        }
    };
}

package com.example.physioscanner;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PPGActivity extends AppCompatActivity {

    private static final String WEBSOCKET_URL = "ws://10.100.242.12:5000"; // Raspberry Pi WebSocket URL
    private static final String TAG = "PPGActivity";

    private LineChart ppgChart;
    private LineDataSet ppgDataSet;
    private LineData ppgLineData;
    private TextView timestampTextView, ppgValueTextView;
    private Button homeButton, viewDatabaseButton;
    private OkHttpClient client;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppg);

        // Initialize UI elements
        ppgChart = findViewById(R.id.ppg_chart);
        timestampTextView = findViewById(R.id.timestamp_text);
        ppgValueTextView = findViewById(R.id.ppg_value_text);
        homeButton = findViewById(R.id.home_button);
        viewDatabaseButton = findViewById(R.id.view_database_button);

        setupPPGChart();
        connectWebSocket();

        // Navigate back to Main Menu
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, MainMenu.class);
            startActivity(intent);
        });

        // Navigate to database view (if storing PPG data)
        viewDatabaseButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, DatabaseViewActivity.class);
            startActivity(intent);
        });
    }

    private void setupPPGChart() {
        ppgDataSet = new LineDataSet(new ArrayList<>(), "PPG Signal");
        ppgDataSet.setColor(Color.BLUE);
        ppgDataSet.setDrawCircles(false);
        ppgDataSet.setLineWidth(2f);

        ppgLineData = new LineData(ppgDataSet);
        ppgChart.setData(ppgLineData);

        Description desc = new Description();
        desc.setText("Live PPG Data");
        ppgChart.setDescription(desc);
    }

    private void connectWebSocket() {
        client = new OkHttpClient();
        Request request = new Request.Builder().url(WEBSOCKET_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> handleIncomingPPGData(text));
            }
        });
    }

    private void handleIncomingPPGData(String text) {
        try {
            JSONObject jsonObject = new JSONObject(text);
            double ppgValue = jsonObject.getDouble("ppg");
            long timestamp = jsonObject.getLong("timestamp");

            // Format timestamp into a readable date/time
            String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(timestamp * 1000));

            // Update UI
            timestampTextView.setText("Timestamp: " + formattedTimestamp);
            ppgValueTextView.setText("PPG Value: " + ppgValue);

            addPPGEntryToGraph((float) ppgValue);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing PPG data", e);
        }
    }

    private void addPPGEntryToGraph(float ppgValue) {
        long xValue = ppgDataSet.getEntryCount();
        ppgDataSet.addEntry(new Entry(xValue, ppgValue));
        ppgLineData.notifyDataChanged();
        ppgChart.notifyDataSetChanged();
        ppgChart.setVisibleXRangeMaximum(100);
        ppgChart.moveViewToX(ppgDataSet.getEntryCount());
        ppgChart.invalidate();
    }
}

package com.example.physioscanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {

    private static final String WEBSOCKET_URL = "ws://192.168.2.96:5000"; // Raspberry Pi WebSocket URL
    private LineChart ecgChart;
    private LineDataSet dataSet;
    private LineData ecgData;
    private TextView timestampText, voltageText;

    private OkHttpClient client;
    private WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ecgChart = findViewById(R.id.ecg_chart);
        timestampText = findViewById(R.id.timestamp_text);
        voltageText = findViewById(R.id.voltage_text);

        Button viewDatabaseButton = findViewById(R.id.view_database_button);
        viewDatabaseButton.setOnClickListener(v -> {
            closeWebSocket(); // Stop WebSocket communication when navigating away
            Intent intent = new Intent(MainActivity.this, DatabaseViewActivity.class);
            startActivity(intent);
        });

        setupChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reconnect WebSocket when returning to MainActivity
        connectWebSocket();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Close WebSocket communication when leaving MainActivity
        closeWebSocket();
    }

    private void setupChart() {
        dataSet = new LineDataSet(null, "ECG Data");
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_dark));

        ecgData = new LineData(dataSet);
        ecgChart.setData(ecgData);

        XAxis xAxis = ecgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0);

        YAxis leftAxis = ecgChart.getAxisLeft();
        leftAxis.setAxisMinimum(-5f);
        leftAxis.setAxisMaximum(5f);

        ecgChart.getAxisRight().setEnabled(false);
        ecgChart.getDescription().setEnabled(false);
        ecgChart.setTouchEnabled(false);
        ecgChart.invalidate();
    }

    private void connectWebSocket() {
        client = new OkHttpClient();
        Request request = new Request.Builder().url(WEBSOCKET_URL).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("WebSocket", "Connection opened");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> handleIncomingData(text));
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("WebSocket", "Connection failed", t);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d("WebSocket", "Connection closing: " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WebSocket", "Connection closed: " + reason);
            }
        });
    }

    private void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, null); // Gracefully close the WebSocket connection
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown(); // Shut down OkHttp client
        }
    }

    private void handleIncomingData(String text) {
        try {
            JSONObject jsonObject = new JSONObject(text);
            String timestamp = jsonObject.getString("timestamp");
            double voltage = jsonObject.getDouble("voltage");

            timestampText.setText("Timestamp: " + timestamp);
            voltageText.setText("Voltage: " + voltage + "V");

            addEntryToGraph((float) voltage);
        } catch (Exception e) {
            Log.e("WebSocket", "Error parsing data", e);
        }
    }

    private void addEntryToGraph(float voltage) {
        long xValue = dataSet.getEntryCount();
        dataSet.addEntry(new Entry(xValue, voltage));
        ecgData.notifyDataChanged();
        ecgChart.notifyDataSetChanged();
        ecgChart.setVisibleXRangeMaximum(100);
        ecgChart.moveViewToX(dataSet.getEntryCount());
        ecgChart.invalidate();
    }
}

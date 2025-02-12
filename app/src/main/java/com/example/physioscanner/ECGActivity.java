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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ECGActivity extends AppCompatActivity {

    private static final String SOCKET_URL = "http://192.168.2.94:5000"; // Use http:// for Socket.IO
    private LineChart lineChart;
    private LineDataSet dataSet;
    private LineData chartData;
    private TextView timestampText, valueText;
    private Socket mSocket;
    private String dataType = "ecg"; // default mode is ECG

    {
        try {
            mSocket = IO.socket(SOCKET_URL);
        } catch (URISyntaxException e) {
            Log.e("SocketIO", "Error creating socket", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg);

        // Check for the "dataType" extra; if absent, default to "ecg"
        Intent intent = getIntent();
        if (intent.hasExtra("dataType")) {
            dataType = intent.getStringExtra("dataType");
        }
        Log.d("MainActivity", "Data type: " + dataType);

        // Find UI elements (the layout uses the same IDs regardless of mode)
        lineChart = findViewById(R.id.ecg_chart);
        timestampText = findViewById(R.id.timestamp_text);
        valueText = findViewById(R.id.voltage_text);

        Button viewDatabaseButton = findViewById(R.id.view_database_button);
        viewDatabaseButton.setOnClickListener(v -> {
            mSocket.disconnect();
            Intent dbIntent = new Intent(ECGActivity.this, DatabaseViewActivity.class);
            dbIntent.putExtra("dataType", dataType);
            startActivity(dbIntent);
        });

        Button homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(ECGActivity.this, MainMenu.class);
            startActivity(homeIntent);
        });

        setupChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSocket.connect();
        if (dataType.equalsIgnoreCase("ppg")) {
            mSocket.on("ppg_data", onNewData);
        } else {
            mSocket.on("ecg_data", onNewData);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSocket.disconnect();
        if (dataType.equalsIgnoreCase("ppg")) {
            mSocket.off("ppg_data", onNewData);
        } else {
            mSocket.off("ecg_data", onNewData);
        }
    }

    private void setupChart() {
        if (dataType.equalsIgnoreCase("ppg")) {
            dataSet = new LineDataSet(new java.util.ArrayList<Entry>(), "PPG Data");
            dataSet.setColor(getResources().getColor(android.R.color.holo_green_dark));
            dataSet.setDrawValues(false); // Remove point labels
            dataSet.setDrawCircles(false);
            chartData = new LineData(dataSet);
            lineChart.setData(chartData);

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setAxisMinimum(0);

            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(200f);  // typical heart rate range

            lineChart.getAxisRight().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            lineChart.setTouchEnabled(false);
            lineChart.invalidate();
        } else {
            // ECG default mode
            dataSet = new LineDataSet(new java.util.ArrayList<Entry>(), "ECG Data");
            dataSet.setColor(getResources().getColor(android.R.color.holo_blue_dark));
            dataSet.setDrawValues(false); // Remove point labels
            dataSet.setDrawCircles(false);
            chartData = new LineData(dataSet);
            lineChart.setData(chartData);

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setAxisMinimum(0);

            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(5f);

            lineChart.getAxisRight().setEnabled(false);
            lineChart.getDescription().setEnabled(false);
            lineChart.setTouchEnabled(false);
            lineChart.invalidate();
        }
    }

    private Emitter.Listener onNewData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            runOnUiThread(() -> {
                try {
                    String timestamp = data.getString("timestamp");
                    double value;
                    if (dataType.equalsIgnoreCase("ppg")) {
                        value = data.getDouble("ppg");
                        timestampText.setText("Timestamp: " + timestamp);
                        valueText.setText("Heart Rate: " + value);
                    } else {
                        value = data.getDouble("voltage");
                        timestampText.setText("Timestamp: " + timestamp);
                        valueText.setText("Voltage: " + value + " V");
                    }
                    addEntryToGraph((float) value);
                } catch (JSONException e) {
                    Log.e("SocketIO", "Error parsing data", e);
                }
            });
        }
    };

    private void addEntryToGraph(float value) {
        int xValue = dataSet.getEntryCount();
        dataSet.addEntry(new Entry(xValue, value));
        chartData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        if (dataType.equalsIgnoreCase("ppg")) {
            lineChart.setVisibleXRangeMaximum(50);
        } else {
            lineChart.setVisibleXRangeMaximum(100);
        }
        // Instantaneously jump to the latest record
        lineChart.moveViewToX(dataSet.getEntryCount() - 1);
        lineChart.invalidate();
    }
}

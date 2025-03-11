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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class AllSensorsActivity extends AppCompatActivity {

    private static final String SOCKET_URL = "http://10.100.242.3:5000"; // Adjust if needed
    private static final String TAG = "AllSensorsActivity";

    // Charts
    private LineChart ecgChart;
    private LineChart ppgChart;

    // Data sets for the charts
    private LineDataSet ecgDataSet;
    private LineDataSet ppgDataSet;

    // LineData for each chart
    private LineData ecgLineData;
    private LineData ppgLineData;

    // TextViews for real-time data
    private TextView textHeartRate, textOxygen, textConfidence, textTemperature, textTimestamp;

    // Socket
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
        setContentView(R.layout.activity_all_sensors);

        // Initialize views
        ecgChart = findViewById(R.id.ecg_chart);
        ppgChart = findViewById(R.id.ppg_chart);

        textHeartRate = findViewById(R.id.text_heart_rate);
        textOxygen = findViewById(R.id.text_oxygen);
        textConfidence = findViewById(R.id.text_confidence);
        textTemperature = findViewById(R.id.text_temperature);
        textTimestamp = findViewById(R.id.text_timestamp);

        Button homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            // Go back to main menu
            Intent intent = new Intent(AllSensorsActivity.this, MainMenu.class);
            startActivity(intent);
        });

        setupEcgChart();
        setupPpgChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Connect socket and subscribe to events
        mSocket.connect();
        mSocket.on("ecg_data", onNewEcgData);
        mSocket.on("ppg_gravity_data", onNewGravityPpgData);
        mSocket.on("ppg_data", onNewDigitalPpgData);   // SparkFun digital PPG
        mSocket.on("tmp102_data", onNewTempData);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unsubscribe and disconnect
        mSocket.off("ecg_data", onNewEcgData);
        mSocket.off("ppg_gravity_data", onNewGravityPpgData);
        mSocket.off("ppg_data", onNewDigitalPpgData);
        mSocket.off("tmp102_data", onNewTempData);
        mSocket.disconnect();
    }

    private void setupEcgChart() {
        ecgDataSet = new LineDataSet(new ArrayList<>(), "ECG");
        ecgDataSet.setColor(Color.RED);
        ecgDataSet.setDrawCircles(false);
        ecgDataSet.setLineWidth(1.5f);
        ecgDataSet.setDrawValues(false);

        ecgLineData = new LineData(ecgDataSet);
        ecgChart.setData(ecgLineData);

        // Configure chart axes
        XAxis xAxis = ecgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = ecgChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(5f); // ECG in volts (0-5V if using a typical Arduino ADC)

        ecgChart.getAxisRight().setEnabled(false);
        ecgChart.setTouchEnabled(false);
        Description desc = new Description();
        desc.setText("ECG Waveform");
        ecgChart.setDescription(desc);
        ecgChart.invalidate();
    }

    private void setupPpgChart() {
        ppgDataSet = new LineDataSet(new ArrayList<>(), "PPG");
        ppgDataSet.setColor(Color.BLUE);
        ppgDataSet.setDrawCircles(false);
        ppgDataSet.setLineWidth(1.5f);
        ppgDataSet.setDrawValues(false);

        ppgLineData = new LineData(ppgDataSet);
        ppgChart.setData(ppgLineData);

        // Configure chart axes
        XAxis xAxis = ppgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = ppgChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1023f); // If your Gravity PPG is 10-bit analog

        ppgChart.getAxisRight().setEnabled(false);
        ppgChart.setTouchEnabled(false);
        Description desc = new Description();
        desc.setText("PPG Waveform");
        ppgChart.setDescription(desc);
        ppgChart.invalidate();
    }

    // ---------------- Socket.io Listeners ---------------- //

    private final Emitter.Listener onNewEcgData = args -> runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        try {
            String timestamp = data.getString("timestamp");
            double ecgValue = data.getDouble("value");
            textTimestamp.setText("Last Update: " + timestamp);

            // Add new point to ECG chart
            float yVal = (float) ecgValue;
            int xVal = ecgDataSet.getEntryCount();
            ecgDataSet.addEntry(new Entry(xVal, yVal));
            ecgLineData.notifyDataChanged();
            ecgChart.notifyDataSetChanged();
            ecgChart.setVisibleXRangeMaximum(200);
            ecgChart.moveViewToX(ecgDataSet.getEntryCount() - 1);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ECG data", e);
        }
    });

    private final Emitter.Listener onNewGravityPpgData = args -> runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        try {
            String timestamp = data.getString("timestamp");
            double ppgValue = data.getDouble("value");
            textTimestamp.setText("Last Update: " + timestamp);

            // Add new point to PPG chart
            float yVal = (float) ppgValue;
            int xVal = ppgDataSet.getEntryCount();
            ppgDataSet.addEntry(new Entry(xVal, yVal));
            ppgLineData.notifyDataChanged();
            ppgChart.notifyDataSetChanged();
            ppgChart.setVisibleXRangeMaximum(200);
            ppgChart.moveViewToX(ppgDataSet.getEntryCount() - 1);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gravity PPG data", e);
        }
    });

    // Digital PPG data (SparkFun)
    private final Emitter.Listener onNewDigitalPpgData = args -> runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        try {
            int heartRate = data.getInt("heart_rate");
            int confidence = data.getInt("confidence");
            int oxygen = data.getInt("oxygen");

            textHeartRate.setText("HR: " + heartRate + " bpm");
            textConfidence.setText("Confidence: " + confidence + "%");
            textOxygen.setText("SpO₂: " + oxygen + "%");

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing SparkFun PPG data", e);
        }
    });

    // Temperature data
    private final Emitter.Listener onNewTempData = args -> runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        try {
            double tempC = data.getDouble("temperature_c");
            textTemperature.setText("Temp: " + String.format("%.2f", tempC) + "°C");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing temperature data", e);
        }
    });
}

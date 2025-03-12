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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class AllSensorsActivity extends AppCompatActivity {

    private static final String TAG = "AllSensorsActivity";

    // Update the IP/URL if needed.
    private String socketUrl = "http://172.20.10.2:5000";
    private Socket mSocket;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private LineChart ecgChart, ppgChart;
    private LineDataSet ecgDataSet, ppgDataSet;
    private LineData ecgLineData, ppgLineData;

    private TextView textHeartRate, textOxygen, textConfidence, textTemperature, textTimestamp;
    private TextView userDetails;
    private Button signInButton, logoutButton, homeButton;

    // TextView to display locally computed ECG BPM
    private TextView text_ecg;

    // TextView to display locally computed PPG BPM
    private TextView text_ppg_bpm;

    // Lists to store recent samples for local BPM calculations
    private final ArrayList<ECGSample> ecgSamples = new ArrayList<>();
    private final ArrayList<PPGSample> ppgSamples = new ArrayList<>();

    // Inner classes to hold samples (timestamp in ms, and a float value).
    public static class ECGSample {
        public long timestamp;
        public float value;
        public ECGSample(long timestamp, float value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
    public static class PPGSample {
        public long timestamp;
        public float value;
        public PPGSample(long timestamp, float value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_sensors);  // Make sure this layout has @+id/signin!

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Find chart views
        ecgChart = findViewById(R.id.ecg_chart);
        ppgChart = findViewById(R.id.ppg_chart);

        // Find TextViews for data
        textHeartRate   = findViewById(R.id.text_heart_rate);
        textOxygen      = findViewById(R.id.text_oxygen);
        textConfidence  = findViewById(R.id.text_confidence);
        textTemperature = findViewById(R.id.text_temperature);
        textTimestamp   = findViewById(R.id.text_timestamp);
        userDetails     = findViewById(R.id.main_title);

        // Buttons
        signInButton    = findViewById(R.id.signin);
        logoutButton    = findViewById(R.id.logout);
        homeButton      = findViewById(R.id.home_button);

        // TextViews for local BPM calculations
        text_ecg        = findViewById(R.id.text_ecg);
        text_ppg_bpm    = findViewById(R.id.text_ppg_bpm);

        if (currentUser == null) {
            redirectToLogin();
        } else {
            userDetails.setText("Logged in as: " + currentUser.getEmail());
        }

        homeButton.setOnClickListener(v -> {
            // Example: go to MainMenu or maybe DatabaseViewActivity
            Intent intent = new Intent(AllSensorsActivity.this, MainMenu.class);
            startActivity(intent);
        });
        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(AllSensorsActivity.this, Login.class);
            startActivity(intent);
        });
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            redirectToLogin();
        });

        setupEcgChart();
        setupPpgChart();
        setChartLabelsWhite();

        IO.Options options = new IO.Options();
        options.reconnection = true;
        options.reconnectionDelay = 2000;
        options.reconnectionAttempts = Integer.MAX_VALUE;

        try {
            mSocket = IO.socket(socketUrl, options);
            Log.d(TAG, "Socket created with URL: " + socketUrl);
            addSocketListeners();
            mSocket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error creating socket", e);
        }
    }

    private void addSocketListeners() {
        mSocket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Socket connected."));
        mSocket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "Socket disconnected."));
        mSocket.on("reconnect", args -> Log.d(TAG, "Socket reconnected."));

        // Data event listeners
        mSocket.on("ecg_data", onNewEcgData);
        mSocket.on("ppg_gravity_data", onNewGravityPpgData);
        mSocket.on("ppg_data", onNewDigitalPpgData);
        mSocket.on("tmp102_data", onNewTempData);

        // If your server also emits ppg_bpm, we can optionally listen:
        mSocket.on("ppg_bpm", onNewPpgBpm);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSocket != null && !mSocket.connected()) {
            Log.d(TAG, "Socket not connected in onResume, reconnecting...");
            mSocket.connect();
        } else {
            Log.d(TAG, "Socket is connected in onResume.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSocket != null) {
            mSocket.off("ecg_data", onNewEcgData);
            mSocket.off("ppg_gravity_data", onNewGravityPpgData);
            mSocket.off("ppg_data", onNewDigitalPpgData);
            mSocket.off("tmp102_data", onNewTempData);
            mSocket.off("ppg_bpm", onNewPpgBpm);
            mSocket.disconnect();
        }
    }

    private void setupEcgChart() {
        ecgDataSet = new LineDataSet(new ArrayList<>(), "ECG");
        ecgDataSet.setColor(Color.RED);
        ecgDataSet.setDrawCircles(false);
        ecgDataSet.setLineWidth(1.5f);
        ecgDataSet.setDrawValues(false);

        ecgLineData = new LineData(ecgDataSet);
        ecgChart.setData(ecgLineData);

        XAxis xAxis = ecgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = ecgChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(5f);

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

        XAxis xAxis = ppgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = ppgChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1023f);

        ppgChart.getAxisRight().setEnabled(false);
        ppgChart.setTouchEnabled(false);

        Description desc = new Description();
        desc.setText("PPG Waveform");
        ppgChart.setDescription(desc);
        ppgChart.invalidate();
    }

    private void setChartLabelsWhite() {
        ecgChart.getXAxis().setTextColor(Color.WHITE);
        ecgChart.getAxisLeft().setTextColor(Color.WHITE);
        ecgChart.getAxisRight().setTextColor(Color.WHITE);
        ppgChart.getXAxis().setTextColor(Color.WHITE);
        ppgChart.getAxisLeft().setTextColor(Color.WHITE);
        ppgChart.getAxisRight().setTextColor(Color.WHITE);

        ecgChart.getLegend().setTextColor(Color.WHITE);
        ppgChart.getLegend().setTextColor(Color.WHITE);
    }

    // ---------------- Socket Event Listeners ---------------- //

    // ECG data event (for local ECG BPM)
    private final Emitter.Listener onNewEcgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String timestamp = data.getString("timestamp");
            double ecgValue = data.getDouble("value");
            textTimestamp.setText("Last Update: " + timestamp);

            // Plot on ECG chart
            addEcgEntryToGraph((float) ecgValue);

            // Local ECG BPM calculation
            long now = System.currentTimeMillis();
            ecgSamples.add(new ECGSample(now, (float)ecgValue));
            // Remove samples older than 10 seconds
            long windowMs = 10000;
            while (!ecgSamples.isEmpty() && now - ecgSamples.get(0).timestamp > windowMs) {
                ecgSamples.remove(0);
            }
            // Lower threshold from 2.0 to 1.8 to avoid overcount
            int ecgBpm = computeLocalBpm(ecgSamples, windowMs, 1.8f);
            text_ecg.setText("ECG BPM: " + ecgBpm);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ECG data", e);
        }
    });

    // Gravity PPG data event (for local PPG BPM)
    private final Emitter.Listener onNewGravityPpgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String timestamp = data.getString("timestamp");
            double ppgValue = data.getDouble("value");
            textTimestamp.setText("Last Update: " + timestamp);

            // Plot on PPG chart
            addPpgEntryToGraph((float) ppgValue);

            // Local PPG BPM calculation
            long now = System.currentTimeMillis();
            ppgSamples.add(new PPGSample(now, (float)ppgValue));
            // Remove samples older than 10 seconds
            long windowMs = 10000;
            while (!ppgSamples.isEmpty() && now - ppgSamples.get(0).timestamp > windowMs) {
                ppgSamples.remove(0);
            }
            // Use threshold ~600 for your PPG peaks around 800
            int ppgBpm = computeLocalBpm(ppgSamples, windowMs, 600f);
            text_ppg_bpm.setText("PPG BPM: " + ppgBpm);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gravity PPG data", e);
        }
    });

    // Digital PPG data from the SparkFun sensor
    private final Emitter.Listener onNewDigitalPpgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            int heartRate  = data.getInt("heart_rate");
            int confidence = data.getInt("confidence");
            int oxygen     = data.getInt("oxygen");
            textHeartRate.setText("HR: " + heartRate + " bpm");
            textConfidence.setText("Confidence: " + confidence + " %");
            textOxygen.setText("SpO₂: " + oxygen + "%");

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Digital PPG data", e);
        }
    });

    // Temperature data
    private final Emitter.Listener onNewTempData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            double tempC = data.getDouble("temperature_c");
            textTemperature.setText("Temp: " + String.format("%.2f", tempC) + "°C");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Temperature data", e);
        }
    });

    // If the server also sends ppg_bpm events
    private final Emitter.Listener onNewPpgBpm = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            int bpm = data.getInt("bpm");
            // If you want to override local BPM with server BPM, you can do:
            // text_ppg_bpm.setText("PPG BPM: " + bpm);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing PPG BPM data", e);
        }
    });

    // ---------------- Chart & BPM Helpers ---------------- //

    // Reusable local BPM calculation for both ECG & PPG.
    // Uses a simple peak detection: if current > prev && current > next && current > threshold, it's a peak.
    private int computeLocalBpm(ArrayList<?> samples, long windowMs, float threshold) {
        // We define ? because we use it for both ECGSample & PPGSample
        if (samples.size() < 3) return 0;

        int peakCount = 0;
        for (int i = 1; i < samples.size() - 1; i++) {
            float prevVal = getValue(samples.get(i - 1));
            float currVal = getValue(samples.get(i));
            float nextVal = getValue(samples.get(i + 1));
            if (currVal > prevVal && currVal > nextVal && currVal > threshold) {
                peakCount++;
            }
        }
        double windowSec = windowMs / 1000.0;
        return (int)((peakCount / windowSec) * 60);
    }

    // Helper to retrieve the float value from either ECGSample or PPGSample
    private float getValue(Object sample) {
        if (sample instanceof ECGSample) {
            return ((ECGSample)sample).value;
        } else if (sample instanceof PPGSample) {
            return ((PPGSample)sample).value;
        }
        return 0f;
    }

    private void addEcgEntryToGraph(float value) {
        int xValue = ecgDataSet.getEntryCount();
        ecgDataSet.addEntry(new Entry(xValue, value));
        ecgLineData.notifyDataChanged();
        ecgChart.notifyDataSetChanged();
        ecgChart.setVisibleXRangeMaximum(200);
        ecgChart.moveViewToX(ecgDataSet.getEntryCount() - 1);
        Log.d(TAG, "ECG data set entry count: " + ecgDataSet.getEntryCount());
        ecgChart.invalidate();
    }

    private void addPpgEntryToGraph(float value) {
        int xValue = ppgDataSet.getEntryCount();
        ppgDataSet.addEntry(new Entry(xValue, value));
        ppgLineData.notifyDataChanged();
        ppgChart.notifyDataSetChanged();
        ppgChart.setVisibleXRangeMaximum(200);
        ppgChart.moveViewToX(ppgDataSet.getEntryCount() - 1);
        Log.d(TAG, "PPG data set entry count: " + ppgDataSet.getEntryCount());
        ppgChart.invalidate();
    }

    private void redirectToLogin() {
        Log.d(TAG, "User not logged in, redirecting to Login screen.");
        Intent intent = new Intent(AllSensorsActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

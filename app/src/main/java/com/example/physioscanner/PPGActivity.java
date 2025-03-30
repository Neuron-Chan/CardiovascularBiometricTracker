package com.example.physioscanner;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class PPGActivity extends AppCompatActivity {

    private static final String TAG = "PPGActivity";
    private Socket mSocket;
    private DocumentReference piRef;

    // For Gravity PPG Chart
    private LineChart ppgChart;
    private LineDataSet ppgDataSet;
    private LineData ppgLineData;

    // Sparkfun digital PPG fields
    private TextView sparkHeartRateText, sparkConfidenceText, sparkOxygenText;
    private TextView timestampTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppg);

        ppgChart            = findViewById(R.id.ppg_chart);
        timestampTextView   = findViewById(R.id.timestamp_text);
        sparkHeartRateText  = findViewById(R.id.spark_heart_rate_text);
        sparkOxygenText     = findViewById(R.id.spark_oxygen_text);
        sparkConfidenceText = findViewById(R.id.spark_confidence_text);

        setupPpgChart();

        // Firestore doc for Pi IP
        piRef = FirebaseFirestore.getInstance().collection("pi_status").document("status");
        piRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (snapshot != null && snapshot.exists()) {
                    String ip = snapshot.getString("ip");
                    initSocket(ip);
                }
            }
        });
    }

    private void setupPpgChart() {
        ppgDataSet = new LineDataSet(new ArrayList<>(), "Gravity PPG Data");
        ppgDataSet.setColor(android.graphics.Color.BLUE);
        ppgDataSet.setDrawCircles(false);
        ppgDataSet.setLineWidth(2f);
        ppgDataSet.setDrawValues(false);

        ppgLineData = new LineData(ppgDataSet);
        ppgChart.setData(ppgLineData);

        XAxis xAxis = ppgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0);

        YAxis leftAxis = ppgChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1023f);

        ppgChart.getAxisRight().setEnabled(false);
        Description desc = new Description();
        desc.setText("Live Gravity PPG Data");
        ppgChart.setDescription(desc);
        ppgChart.setTouchEnabled(false);
        ppgChart.invalidate();
    }

    private void initSocket(String ip) {
        if (ip == null || ip.isEmpty()) {
            Log.w(TAG, "No IP from Firestore, can't connect socket");
            return;
        }
        try {
            if (mSocket != null) {
                mSocket.disconnect();
                mSocket.off("ppg_gravity_data", onNewGravityPpgData);
                mSocket.off("ppg_data", onNewSparkfunPpgData);
            }
            String socketUrl = "http://" + ip + ":5000";
            mSocket = IO.socket(socketUrl);
            mSocket.connect();
            mSocket.on("ppg_gravity_data", onNewGravityPpgData);
            mSocket.on("ppg_data", onNewSparkfunPpgData);
            Log.d(TAG, "Socket connected to " + socketUrl);
        } catch (Exception e) {
            Log.e(TAG, "Socket error", e);
        }
    }

    // Gravity PPG
    private final Emitter.Listener onNewGravityPpgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String timestamp = data.getString("timestamp");
            double ppgValue = data.getDouble("value");
            timestampTextView.setText("Timestamp: " + timestamp);

            addPpgEntryToGraph((float) ppgValue);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Gravity PPG data", e);
        }
    });

    // SparkFun digital PPG
    private final Emitter.Listener onNewSparkfunPpgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            int heartRate   = data.getInt("heart_rate");
            int confidence  = data.getInt("confidence");
            int oxygen      = data.getInt("oxygen");
            sparkHeartRateText.setText("Heart Rate: " + heartRate + " bpm");
            sparkConfidenceText.setText("Confidence: " + confidence + " %");
            sparkOxygenText.setText("Oxygen: " + oxygen + " %");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing SparkFun PPG data", e);
        }
    });

    private void addPpgEntryToGraph(float value) {
        int xValue = ppgDataSet.getEntryCount();
        ppgDataSet.addEntry(new Entry(xValue, value));
        ppgLineData.notifyDataChanged();
        ppgChart.notifyDataSetChanged();
        ppgChart.setVisibleXRangeMaximum(100);
        ppgChart.moveViewToX(ppgDataSet.getEntryCount() - 1);
        ppgChart.invalidate();
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
            mSocket.off("ppg_gravity_data", onNewGravityPpgData);
            mSocket.off("ppg_data", onNewSparkfunPpgData);
            mSocket.disconnect();
        }
    }
}

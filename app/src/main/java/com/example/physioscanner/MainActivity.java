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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Socket mSocket;
    private DocumentReference piRef;

    private LineChart ecgChart;
    private LineDataSet ecgDataSet;
    private LineData ecgLineData;

    private TextView timestampText;
    private TextView ecgValueText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Suppose your layout includes ecg_chart, etc.

        ecgChart      = findViewById(R.id.ecg_chart);
        timestampText = findViewById(R.id.timestamp_text);
        ecgValueText  = findViewById(R.id.voltage_text);

        setupEcgChart();

        // Firestore doc: pi_status/status
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

    private void setupEcgChart() {
        ecgDataSet = new LineDataSet(new ArrayList<>(), "ECG");
        ecgDataSet.setColor(android.graphics.Color.BLUE);
        ecgDataSet.setDrawCircles(false);
        ecgDataSet.setDrawValues(false);
        ecgDataSet.setLineWidth(1.5f);

        ecgLineData = new LineData(ecgDataSet);
        ecgChart.setData(ecgLineData);

        XAxis xAxis = ecgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0);

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

    private void initSocket(String ip) {
        if (ip == null || ip.isEmpty()) {
            Log.w(TAG, "initSocket: IP is null/empty, skipping socket creation");
            return;
        }
        try {
            if (mSocket != null) {
                mSocket.disconnect();
                mSocket.off("ecg_data", onNewEcgData);
            }
            String socketUrl = "http://" + ip + ":5000";
            mSocket = IO.socket(socketUrl);
            mSocket.connect();
            mSocket.on("ecg_data", onNewEcgData);
            Log.d(TAG, "Socket connecting to " + socketUrl);
        } catch (Exception e) {
            Log.e(TAG, "Socket init error", e);
        }
    }

    private final Emitter.Listener onNewEcgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String timestamp = data.getString("timestamp");
            double ecgValue = data.getDouble("value");

            timestampText.setText("Timestamp: " + timestamp);
            ecgValueText.setText("ECG Value: " + ecgValue);

            // Plot on the chart
            addEcgEntry((float) ecgValue);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ecg_data", e);
        }
    });

    private void addEcgEntry(float value) {
        int xValue = ecgDataSet.getEntryCount();
        ecgDataSet.addEntry(new Entry(xValue, value));
        ecgLineData.notifyDataChanged();
        ecgChart.notifyDataSetChanged();
        ecgChart.setVisibleXRangeMaximum(200);
        ecgChart.moveViewToX(ecgDataSet.getEntryCount() - 1);
        ecgChart.invalidate();
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
            mSocket.off("ecg_data", onNewEcgData);
            mSocket.disconnect();
        }
    }
}

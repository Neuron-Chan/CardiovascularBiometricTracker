package com.example.physioscanner;

import android.content.Intent;
import android.graphics.Color;
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
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    // Socket URL is now fixed.
    private String socketUrl = "http://172.20.10.2:5000";
    private Socket mSocket;
    private static final String TAG = "MainActivity";

    private LineChart ecgChart;
    private LineDataSet ecgDataSet;
    private LineData ecgLineData;
    private TextView timestampText, ecgValueText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Only ECG

        ecgChart = findViewById(R.id.ecg_chart);
        timestampText = findViewById(R.id.timestamp_text);
        ecgValueText = findViewById(R.id.voltage_text);

        Button viewDatabaseButton = findViewById(R.id.view_database_button);
        viewDatabaseButton.setOnClickListener(v -> {
            mSocket.disconnect();
            Intent dbIntent = new Intent(MainActivity.this, DatabaseViewActivity.class);
            dbIntent.putExtra("dataType", "ecg");
            startActivity(dbIntent);
        });

        Button homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(MainActivity.this, MainMenu.class);
            startActivity(homeIntent);
        });

        setupEcgChart();

        try {
            mSocket = IO.socket(socketUrl);
            Log.d(TAG, "MainActivity socket created with URL: " + socketUrl);
            mSocket.connect();
            mSocket.on("ecg_data", onNewEcgData);
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
            mSocket.off("ecg_data", onNewEcgData);
        }
    }

    private void setupEcgChart() {
        ecgDataSet = new LineDataSet(new ArrayList<Entry>(), "Gravity ECG Data");
        ecgDataSet.setColor(Color.BLUE);
        ecgDataSet.setDrawCircles(false);
        ecgDataSet.setDrawValues(false);
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
        ecgChart.getDescription().setEnabled(false);
        ecgChart.setTouchEnabled(false);
        ecgChart.invalidate();
    }

    private final Emitter.Listener onNewEcgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
            String timestamp = data.getString("timestamp");
            double ecgValue = data.getDouble("value");
            timestampText.setText("Timestamp: " + timestamp);
            ecgValueText.setText("ECG Voltage: " + ecgValue + " V");
            addEcgEntryToGraph((float) ecgValue);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ECG data", e);
        }
    });

    private void addEcgEntryToGraph(float value) {
        int xValue = ecgDataSet.getEntryCount();
        ecgDataSet.addEntry(new Entry(xValue, value));
        ecgLineData.notifyDataChanged();
        ecgChart.notifyDataSetChanged();
        ecgChart.setVisibleXRangeMaximum(200);
        ecgChart.moveViewToX(ecgDataSet.getEntryCount() - 1);
        ecgChart.invalidate();
    }
}

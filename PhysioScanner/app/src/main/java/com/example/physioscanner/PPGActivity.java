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

public class PPGActivity extends AppCompatActivity {

    private static final String SOCKET_URL = "http://10.100.242.3:5000";
    private static final String TAG = "PPGActivity";
    private LineChart ppgChart;
    private LineDataSet ppgDataSet;
    private LineData ppgLineData;
    private TextView timestampTextView; // For gravity PPG graph timestamp
    // TextViews for SparkFun PPG sensor extra metrics
    private TextView sparkHeartRateText, sparkOxygenText, sparkConfidenceText;
    private Button homeButton, viewDatabaseButton;
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
        setContentView(R.layout.activity_ppg); // Ensure layout has the required views

        ppgChart = findViewById(R.id.ppg_chart);
        timestampTextView = findViewById(R.id.timestamp_text);
        sparkHeartRateText = findViewById(R.id.spark_heart_rate_text);
        sparkOxygenText = findViewById(R.id.spark_oxygen_text);
        sparkConfidenceText = findViewById(R.id.spark_confidence_text);

        homeButton = findViewById(R.id.home_button);
        viewDatabaseButton = findViewById(R.id.view_database_button);
        viewDatabaseButton.setText("View PPG Database");

        setupPpgChart();

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, MainMenu.class);
            startActivity(intent);
        });

        viewDatabaseButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, DatabaseViewActivity.class);
            intent.putExtra("dataType", "ppg");
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSocket.connect();
        // Subscribe to gravity PPG sensor data for graphing
        mSocket.on("ppg_gravity_data", onNewGravityPpgData);
        // Subscribe to SparkFun PPG sensor extra metrics
        mSocket.on("ppg_data", onNewSparkfunPpgData);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSocket.disconnect();
        mSocket.off("ppg_gravity_data", onNewGravityPpgData);
        mSocket.off("ppg_data", onNewSparkfunPpgData);
    }

    private void setupPpgChart() {
        ppgDataSet = new LineDataSet(new ArrayList<Entry>(), "Gravity PPG Data");
        ppgDataSet.setColor(Color.BLUE);
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
        leftAxis.setAxisMaximum(1023f); // Assuming analog range for gravity PPG

        ppgChart.getAxisRight().setEnabled(false);
        Description desc = new Description();
        desc.setText("Live Gravity PPG Data");
        ppgChart.setDescription(desc);
        ppgChart.setTouchEnabled(false);
        ppgChart.invalidate();
    }

    // Listener for Gravity PPG sensor data (for graphing)
    private Emitter.Listener onNewGravityPpgData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            runOnUiThread(() -> {
                try {
                    String timestamp = data.getString("timestamp");
                    double ppgValue = data.getDouble("value"); // expecting "value"
                    timestampTextView.setText("Timestamp: " + timestamp);
                    addPpgEntryToGraph((float) ppgValue);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing gravity PPG data", e);
                }
            });
        }
    };

    // Listener for SparkFun PPG sensor extra metrics
    private Emitter.Listener onNewSparkfunPpgData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            runOnUiThread(() -> {
                try {
                    int heartRate = data.getInt("heart_rate");
                    int confidence = data.getInt("confidence");
                    int oxygen = data.getInt("oxygen");
                    sparkHeartRateText.setText("Heart Rate: " + heartRate + " bpm");
                    sparkConfidenceText.setText("Confidence: " + confidence + " %");
                    sparkOxygenText.setText("Oxygen: " + oxygen + " %");
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing SparkFun PPG data", e);
                }
            });
        }
    };

    private void addPpgEntryToGraph(float value) {
        int xValue = ppgDataSet.getEntryCount();
        ppgDataSet.addEntry(new Entry(xValue, value));
        ppgLineData.notifyDataChanged();
        ppgChart.notifyDataSetChanged();
        ppgChart.setVisibleXRangeMaximum(100); // Display last 100 data points
        ppgChart.moveViewToX(ppgDataSet.getEntryCount() - 1);
        ppgChart.invalidate();
    }
}

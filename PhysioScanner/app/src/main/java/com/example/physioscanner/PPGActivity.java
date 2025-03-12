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

    private String socketUrl = "http://172.20.10.2:5000";
    private Socket mSocket;
    private static final String TAG = "PPGActivity";

    private LineChart ppgChart;
    private LineDataSet ppgDataSet;
    private LineData ppgLineData;
    private TextView timestampTextView;
    private TextView sparkHeartRateText, sparkOxygenText, sparkConfidenceText;
    private Button homeButton, viewDatabaseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppg);

        ppgChart             = findViewById(R.id.ppg_chart);
        timestampTextView    = findViewById(R.id.timestamp_text);
        sparkHeartRateText   = findViewById(R.id.spark_heart_rate_text);
        sparkOxygenText      = findViewById(R.id.spark_oxygen_text);
        sparkConfidenceText  = findViewById(R.id.spark_confidence_text);
        homeButton           = findViewById(R.id.home_button);
        viewDatabaseButton   = findViewById(R.id.view_database_button);
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

        try {
            mSocket = IO.socket(socketUrl);
            Log.d(TAG, "PPGActivity socket created with URL: " + socketUrl);
            mSocket.connect();
            mSocket.on("ppg_gravity_data", onNewGravityPpgData);
            mSocket.on("ppg_data", onNewSparkfunPpgData);
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
            mSocket.off("ppg_gravity_data", onNewGravityPpgData);
            mSocket.off("ppg_data", onNewSparkfunPpgData);
        }
    }

    private void setupPpgChart() {
        ppgDataSet = new LineDataSet(new ArrayList<>(), "Gravity PPG Data");
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
        leftAxis.setAxisMaximum(1023f);

        ppgChart.getAxisRight().setEnabled(false);
        Description desc = new Description();
        desc.setText("Live Gravity PPG Data");
        ppgChart.setDescription(desc);
        ppgChart.setTouchEnabled(false);
        ppgChart.invalidate();
    }

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

    private final Emitter.Listener onNewSparkfunPpgData = args -> runOnUiThread(() -> {
        try {
            JSONObject data = (JSONObject) args[0];
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

    private void addPpgEntryToGraph(float value) {
        int xValue = ppgDataSet.getEntryCount();
        ppgDataSet.addEntry(new Entry(xValue, value));
        ppgLineData.notifyDataChanged();
        ppgChart.notifyDataSetChanged();
        ppgChart.setVisibleXRangeMaximum(100);
        ppgChart.moveViewToX(ppgDataSet.getEntryCount() - 1);
        ppgChart.invalidate();
    }
}

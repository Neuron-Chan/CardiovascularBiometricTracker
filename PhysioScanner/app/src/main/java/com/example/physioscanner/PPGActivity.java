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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class PPGActivity extends AppCompatActivity {

    private static final String SOCKET_URL = "http://10.100.242.14:5000"; // Use http:// for Socket.IO
    private static final String TAG = "PPGActivity";

    private LineChart ppgChart;
    private LineDataSet ppgDataSet;
    private LineData ppgLineData;
    private TextView timestampTextView, ppgValueTextView;
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
        setContentView(R.layout.activity_ppg);

        ppgChart = findViewById(R.id.ppg_chart);
        timestampTextView = findViewById(R.id.timestamp_text);
        ppgValueTextView = findViewById(R.id.ppg_value_text);
        homeButton = findViewById(R.id.home_button);
        viewDatabaseButton = findViewById(R.id.view_database_button);

        setupPPGChart();

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, MainMenu.class);
            startActivity(intent);
        });

        viewDatabaseButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, DatabaseViewActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSocket.connect();
        mSocket.on("ppg_data", onNewPpgData);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSocket.disconnect();
        mSocket.off("ppg_data", onNewPpgData);
    }

    private void setupPPGChart() {
        ppgDataSet = new LineDataSet(new ArrayList<Entry>(), "PPG Signal");
        ppgDataSet.setColor(Color.BLUE);
        ppgDataSet.setDrawCircles(false);
        ppgDataSet.setLineWidth(2f);

        ppgLineData = new LineData(ppgDataSet);
        ppgChart.setData(ppgLineData);

        Description desc = new Description();
        desc.setText("Live PPG Data");
        ppgChart.setDescription(desc);
    }

    private Emitter.Listener onNewPpgData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            runOnUiThread(() -> {
                try {
                    double ppgValue = data.getDouble("ppg");
                    String timestamp = data.getString("timestamp");
                    timestampTextView.setText("Timestamp: " + timestamp);
                    ppgValueTextView.setText("PPG Value: " + ppgValue);
                    addPPGEntryToGraph((float) ppgValue);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing PPG data", e);
                }
            });
        }
    };

    private void addPPGEntryToGraph(float ppgValue) {
        int xValue = ppgDataSet.getEntryCount();
        ppgDataSet.addEntry(new Entry(xValue, ppgValue));
        ppgLineData.notifyDataChanged();
        ppgChart.notifyDataSetChanged();
        // For PPG at 25 Hz, show a visible window of ~100 points (4 seconds)
        ppgChart.setVisibleXRangeMaximum(100);
        ppgChart.moveViewToX(ppgDataSet.getEntryCount());
        ppgChart.invalidate();
    }
}

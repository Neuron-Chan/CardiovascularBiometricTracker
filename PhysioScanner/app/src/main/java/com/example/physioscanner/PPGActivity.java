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

    // Use the new IP address
    private static final String SOCKET_URL = "http://192.168.2.94:5000";
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
        // Ensure that the PPG activity uses its own layout
        setContentView(R.layout.activity_ppg);

        // Bind UI elements from activity_ppg.xml
        ppgChart = findViewById(R.id.ppg_chart);
        timestampTextView = findViewById(R.id.timestamp_text);
        ppgValueTextView = findViewById(R.id.ppg_value_text);
        homeButton = findViewById(R.id.home_button);
        viewDatabaseButton = findViewById(R.id.view_database_button);

        // Set the button text explicitly to "View PPG Database"
        viewDatabaseButton.setText("View PPG Database");

        setupPPGChart();

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(PPGActivity.this, MainMenu.class);
            startActivity(intent);
        });

        // When the user taps the database button, pass the extra "dataType" = "ppg"
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
        ppgDataSet.setDrawValues(false);  // Remove point labels

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
                    // Extract "ppg" data (the server emits heart_rate in the "ppg" key)
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
        // For 25 Hz, a window of 100 points is about 4 seconds.
        ppgChart.setVisibleXRangeMaximum(100);
        // Immediately jump to the latest entry:
        ppgChart.moveViewToX(ppgDataSet.getEntryCount() - 1);
        ppgChart.invalidate();
    }
}

package com.example.physioscanner;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DatabaseViewActivity extends AppCompatActivity {

    private static final String TAG = "DatabaseViewActivity";
    private RecyclerView recyclerView;
    private DatabaseAdapter adapter;
    private ArrayList<DatabaseRecord> records;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button homeButton, refreshButton;
    private String dataType; // "ecg", "ppg", or "temp"
    private String apiUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_view);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        recyclerView = findViewById(R.id.recycler_view);
        refreshButton = findViewById(R.id.refresh_button);
        homeButton = findViewById(R.id.home_button);

        records = new ArrayList<>();
        adapter = new DatabaseAdapter(records);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Determine the data type
        dataType = getIntent().getStringExtra("dataType");
        if (dataType == null) {
            dataType = "ecg";
        }
        Log.d(TAG, "Data type requested: " + dataType);

        // Set the API URL based on data type
        if (dataType.equalsIgnoreCase("ppg")) {
            apiUrl = "http://10.100.242.3:5000/api/ppg_data";
        } else if (dataType.equalsIgnoreCase("temp")) {
            apiUrl = "http://10.100.242.3:5000/api/temperature_data";
        } else {
            apiUrl = "http://10.100.242.3:5000/api/ecg_data";
        }

        refreshButton.setOnClickListener(v -> new FetchDatabaseTask().execute(apiUrl));
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(DatabaseViewActivity.this, MainMenu.class);
            startActivity(intent);
        });

        // Initial data load
        new FetchDatabaseTask().execute(apiUrl);
    }

    private class FetchDatabaseTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching data: " + e.getMessage(), e);
                return null;
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            swipeRefreshLayout.setRefreshing(false);
            if (result != null) {
                parseAndDisplayData(result);
            } else {
                Log.e(TAG, "Fetch failed");
            }
        }
    }

    private void parseAndDisplayData(String json) {
        try {
            JSONArray jsonArray = new JSONArray(json);
            records.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject record = jsonArray.getJSONObject(i);
                String timestamp = record.getString("timestamp");
                double value = 0;
                String label = "";
                if (dataType.equalsIgnoreCase("ppg")) {
                    value = record.getDouble("ppg");
                    label = "Heart Rate (bpm)";
                } else if (dataType.equalsIgnoreCase("temp")) {
                    value = record.getDouble("temperature_c");
                    label = "Temperature (°C)";
                } else {
                    value = record.getDouble("voltage");
                    label = "Voltage (V)";
                }
                records.add(new DatabaseRecord(timestamp, value, label));
            }
            adapter.notifyDataSetChanged();
            if (!records.isEmpty()) {
                recyclerView.scrollToPosition(records.size() - 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }
}

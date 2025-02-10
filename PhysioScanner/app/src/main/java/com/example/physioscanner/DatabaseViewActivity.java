package com.example.physioscanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
import java.util.List;

public class DatabaseViewActivity extends AppCompatActivity {

    private static final String TAG = "DatabaseViewActivity";
    private RecyclerView recyclerView;
    private DatabaseAdapter adapter;
    private List<DatabaseRecord> records;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button homeButton;
    private Button scrollToBottomButton;  // Button to instantly jump to latest record
    private String dataType; // "ecg" or "ppg"
    private String apiUrl;
    private String cacheKey;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private static final int POLL_INTERVAL = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_view);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        recyclerView = findViewById(R.id.recycler_view);
        homeButton = findViewById(R.id.home_button);
        scrollToBottomButton = findViewById(R.id.scroll_to_bottom_button); // Ensure this exists in your layout

        // Get data type from the Intent extra; default to "ecg"
        dataType = getIntent().getStringExtra("dataType");
        if (dataType == null) {
            dataType = "ecg";
        }
        Log.d(TAG, "Data type requested: " + dataType);

        // Set API endpoint and cache key based on the data type.
        if (dataType.equalsIgnoreCase("ppg")) {
            apiUrl = "http://10.100.242.14:5000/api/ppg_data";
            cacheKey = "ppg_data_cache";
        } else {
            apiUrl = "http://10.100.242.14:5000/api/ecg_data";
            cacheKey = "ecg_data_cache";
        }

        records = new ArrayList<>();
        adapter = new DatabaseAdapter(records);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Show the "Scroll to Latest" button when user scrolls up
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisibleItem < adapter.getItemCount() - 1) {
                    scrollToBottomButton.setVisibility(View.VISIBLE);
                } else {
                    scrollToBottomButton.setVisibility(View.GONE);
                }
            }
        });

        // When the "Scroll to Latest" button is pressed, jump instantly to the latest record.
        scrollToBottomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                scrollToBottomButton.setVisibility(View.GONE);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchDatabaseTask().execute(apiUrl);
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DatabaseViewActivity.this, MainMenu.class);
                startActivity(intent);
            }
        });

        // Load cached data first
        loadCachedData();

        // Start periodic polling for real-time updates
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                new FetchDatabaseTask().execute(apiUrl);
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };
        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }

    private void loadCachedData() {
        SharedPreferences prefs = getSharedPreferences("DatabaseCache", Context.MODE_PRIVATE);
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            parseAndDisplayData(cachedJson, true);
        }
    }

    private void cacheData(String json) {
        SharedPreferences prefs = getSharedPreferences("DatabaseCache", Context.MODE_PRIVATE);
        prefs.edit().putString(cacheKey, json).apply();
    }

    /**
     * Parses JSON data and updates the RecyclerView.
     * If autoScroll is true, it instantly jumps to the latest record.
     */
    private void parseAndDisplayData(String json, boolean autoScroll) {
        try {
            JSONArray jsonArray = new JSONArray(json);
            Log.d(TAG, "Parsed " + jsonArray.length() + " records");
            records.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject record = jsonArray.getJSONObject(i);
                String timestamp = record.getString("timestamp");
                String valueKey = dataType.equalsIgnoreCase("ppg") ? "ppg" : "voltage";
                String valueStr = record.optString(valueKey, "N/A");
                try {
                    double value = Double.parseDouble(valueStr);
                    String label = dataType.equalsIgnoreCase("ppg") ? "Heart Rate" : "Voltage";
                    records.add(new DatabaseRecord(timestamp, value, label));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Skipping record with invalid value: " + valueStr);
                }
            }
            adapter.notifyDataSetChanged();
            if (autoScroll && !records.isEmpty()) {
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }

    // Overloaded method: default autoScroll = true.
    private void parseAndDisplayData(String json) {
        parseAndDisplayData(json, true);
    }

    private class FetchDatabaseTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error code: " + responseCode);
                    return null;
                }
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
                cacheData(result);
                parseAndDisplayData(result);
            } else {
                Log.e(TAG, "Fetch failed, using cached data if available");
            }
        }
    }
}

package com.example.physioscanner;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

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
    private DocumentReference piRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_view);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        recyclerView       = findViewById(R.id.recycler_view);
        refreshButton      = findViewById(R.id.refresh_button);
        homeButton         = findViewById(R.id.home_button);

        records = new ArrayList<>();
        adapter = new DatabaseAdapter(records);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        dataType = getIntent().getStringExtra("dataType");
        if (dataType == null) {
            dataType = "ecg";
        }
        Log.d(TAG, "Data type requested: " + dataType);

        // Firestore doc for Pi IP
        piRef = FirebaseFirestore.getInstance().collection("pi_status").document("status");
        piRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (snapshot != null && snapshot.exists()) {
                    String ip = snapshot.getString("ip");
                    updateApiUrl(ip);
                    // auto-fetch new data whenever IP changes
                    new FetchDatabaseTask().execute(apiUrl);
                }
            }
        });

        refreshButton.setOnClickListener(v -> new FetchDatabaseTask().execute(apiUrl));
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(DatabaseViewActivity.this, MainMenu.class);
            startActivity(intent);
        });
    }

    private void updateApiUrl(String ip) {
        if (ip == null || ip.isEmpty()) {
            Log.w(TAG, "No IP from Firestore, can't build API URL");
            return;
        }
        String base = "http://" + ip + ":5000/api/";
        if (dataType.equalsIgnoreCase("ppg")) {
            apiUrl = base + "ppg_data";
        } else if (dataType.equalsIgnoreCase("temp")) {
            apiUrl = base + "temperature_data";
        } else {
            apiUrl = base + "ecg_data";
        }
        Log.d(TAG, "API URL set to: " + apiUrl);
    }

    private class FetchDatabaseTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
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
            if (result == null) {
                Log.e(TAG, "Fetch failed");
            } else {
                parseAndDisplayData(result);
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
                double value;
                String label;

                // Adjust this logic for your real JSON field names
                if (dataType.equalsIgnoreCase("ppg")) {
                    // e.g. "value" or "ppg_value" in server JSON
                    value = record.has("value") ? record.getDouble("value") : 0.0;
                    label = "PPG Value";
                } else if (dataType.equalsIgnoreCase("temp")) {
                    // e.g. "temperature_c" from server
                    value = record.has("temperature_c") ? record.getDouble("temperature_c") : 0.0;
                    label = "Temperature (°C)";
                } else {
                    // e.g. "ecg_value" from server
                    value = record.has("value") ? record.getDouble("value") : 0.0;
                    label = "ECG Voltage (V)";
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

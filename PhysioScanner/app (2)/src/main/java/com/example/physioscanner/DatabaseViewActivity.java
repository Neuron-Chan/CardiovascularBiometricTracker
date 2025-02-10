package com.example.physioscanner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DatabaseViewActivity extends AppCompatActivity {

    private static final String API_URL = "http://192.168.2.96:8080/api/ecg_data"; // Flask API URL
    private static final String TAG = "DatabaseViewActivity";
    private RecyclerView recyclerView;
    private DatabaseAdapter adapter;
    private List<ECGEntry> ecgEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_view);

        recyclerView = findViewById(R.id.recycler_view);
        ecgEntries = new ArrayList<>();
        adapter = new DatabaseAdapter(ecgEntries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Fetch data from the Raspberry Pi
        new FetchDatabaseTask().execute(API_URL);
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
            if (result == null) {
                Log.e(TAG, "Failed to fetch data.");
                return;
            }
            try {
                JSONArray jsonArray = new JSONArray(result);
                ecgEntries.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject record = jsonArray.getJSONObject(i);
                    String timestamp = record.getString("timestamp");
                    double voltage = record.getDouble("voltage");

                    ecgEntries.add(new ECGEntry(timestamp, voltage));
                }

                adapter.notifyDataSetChanged();

                // Automatically scroll to the latest entry
                if (!ecgEntries.isEmpty()) {
                    recyclerView.scrollToPosition(ecgEntries.size() - 1);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error parsing JSON", e);
            }
        }
    }
}

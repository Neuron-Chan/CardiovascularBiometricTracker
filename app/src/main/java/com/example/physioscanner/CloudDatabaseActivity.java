package com.example.physioscanner;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CloudDatabaseActivity extends AppCompatActivity {

    private static final String TAG = "CloudDatabaseActivity";

    // Firebase components
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // UI components
    private Spinner sensorTypeSpinner;
    private Button queryButton, startDateButton, endDateButton, homeButton;
    private TextView dataCountText, statusText;
    private LineChart ecgChart, ppgChart;
    private RecyclerView dataRecyclerView;
    private LinearLayout chartContainer;
    private ScrollView scrollContainer;
    private RadioGroup sortOrderGroup;
    private RadioButton ascendingRadio, descendingRadio;

    // Vital signs TextViews
    private TextView ecgHrValue, ecgAvgValue, ppgPulseValue, ppgAvgValue;

    // Adapters
    private SensorDataAdapter dataAdapter;

    // Query parameters
    private String selectedSensorType = "ecg_data";
    private Date startDate = null;
    private Date endDate = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS", Locale.US);
    private boolean ascendingOrder = true; // Default to ascending (oldest first)

    // Data storage
    private List<Map<String, Object>> queryResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_database);

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to access the database", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize UI components
        sensorTypeSpinner = findViewById(R.id.sensor_type_spinner);
        queryButton = findViewById(R.id.query_button);
        startDateButton = findViewById(R.id.start_date_button);
        endDateButton = findViewById(R.id.end_date_button);
        homeButton = findViewById(R.id.home_button);
        dataCountText = findViewById(R.id.data_count_text);
        statusText = findViewById(R.id.status_text);
        sortOrderGroup = findViewById(R.id.sort_order_group);
        ascendingRadio = findViewById(R.id.radio_ascending);
        descendingRadio = findViewById(R.id.radio_descending);

        // Charts are optional based on sensor type
        ecgChart = findViewById(R.id.ecg_chart);
        ppgChart = findViewById(R.id.ppg_chart);
        chartContainer = findViewById(R.id.chart_container);

        // Initialize vital sign TextViews
        ecgHrValue = findViewById(R.id.ecg_hr_value);
        ecgAvgValue = findViewById(R.id.ecg_avg_value);
        ppgPulseValue = findViewById(R.id.ppg_pulse_value);
        ppgAvgValue = findViewById(R.id.ppg_avg_value);

        // RecyclerView for displaying query results
        dataRecyclerView = findViewById(R.id.data_recycler_view);
        dataRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataAdapter = new SensorDataAdapter(queryResults);
        dataRecyclerView.setAdapter(dataAdapter);

        scrollContainer = findViewById(R.id.scroll_container);

        // Setup spinner with sensor types
        setupSensorTypeSpinner();

        // Setup date buttons
        setupDateButtons();

        // Setup sort order radio buttons
        sortOrderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_ascending) {
                ascendingOrder = true;
            } else {
                ascendingOrder = false;
            }

            // Re-sort and display data if we have results
            if (!queryResults.isEmpty()) {
                sortQueryResults();
                updateDataDisplay();
            }
        });

        // Setup query button
        queryButton.setOnClickListener(v -> performQuery());

        // Setup home button
        homeButton.setOnClickListener(v -> finish());

        // Setup charts (initially hidden)
        setupCharts();
        chartContainer.setVisibility(View.GONE);

        // For debugging, log the user ID
        Log.d(TAG, "Current user UID: " + (currentUser != null ? currentUser.getUid() : "null"));
    }

    private void setupSensorTypeSpinner() {
        List<String> sensorTypes = new ArrayList<>();
        sensorTypes.add("ECG Data");
        sensorTypes.add("PPG Data");
        sensorTypes.add("Temperature Data");
        sensorTypes.add("Heart Rate Data");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, sensorTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sensorTypeSpinner.setAdapter(adapter);
        sensorTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        selectedSensorType = "ecg_data";
                        break;
                    case 1:
                        selectedSensorType = "ppg_gravity_data";
                        break;
                    case 2:
                        selectedSensorType = "tmp102_data";
                        break;
                    case 3:
                        selectedSensorType = "ppg_max_data";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupDateButtons() {
        // Set default dates to today and 7 days ago
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTime();
        endDateButton.setText(dateFormat.format(endDate));

        calendar.add(Calendar.DAY_OF_MONTH, -7);
        startDate = calendar.getTime();
        startDateButton.setText(dateFormat.format(startDate));

        // Setup click listeners for date buttons
        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(final boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && startDate != null) {
            calendar.setTime(startDate);
        } else if (!isStartDate && endDate != null) {
            calendar.setTime(endDate);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);

                    if (isStartDate) {
                        startDate = selectedCalendar.getTime();
                        startDateButton.setText(dateFormat.format(startDate));
                    } else {
                        endDate = selectedCalendar.getTime();
                        endDateButton.setText(dateFormat.format(endDate));
                    }
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void setupCharts() {
        // ECG Chart setup
        setupEcgChart();

        // PPG Chart setup
        setupPpgChart();
    }

    private void setupEcgChart() {
        // Set chart properties for medical monitor style
        ecgChart.setBackgroundColor(Color.BLACK);
        ecgChart.setDrawGridBackground(false);
        ecgChart.setDrawBorders(false);
        ecgChart.setAutoScaleMinMaxEnabled(true);
        ecgChart.setDragEnabled(true);
        ecgChart.setScaleEnabled(true);
        ecgChart.setPinchZoom(false);
        ecgChart.setDoubleTapToZoomEnabled(false);
        ecgChart.getLegend().setEnabled(false);

        // Configure axes
        XAxis xAxis = ecgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#244D24")); // Dark green grid
        xAxis.setTextColor(Color.GREEN);
        xAxis.setDrawAxisLine(false);
        xAxis.setEnabled(true);

        YAxis leftAxis = ecgChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#244D24")); // Dark green grid
        leftAxis.setTextColor(Color.GREEN);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(5f);

        YAxis rightAxis = ecgChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Set description
        Description description = new Description();
        description.setText("");
        ecgChart.setDescription(description);
    }

    private void setupPpgChart() {
        // Set chart properties for medical monitor style
        ppgChart.setBackgroundColor(Color.BLACK);
        ppgChart.setDrawGridBackground(false);
        ppgChart.setDrawBorders(false);
        ppgChart.setAutoScaleMinMaxEnabled(true);
        ppgChart.setDragEnabled(true);
        ppgChart.setScaleEnabled(true);
        ppgChart.setPinchZoom(false);
        ppgChart.setDoubleTapToZoomEnabled(false);
        ppgChart.getLegend().setEnabled(false);

        // Configure axes
        XAxis xAxis = ppgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#244D24")); // Dark green grid
        xAxis.setTextColor(Color.GREEN);
        xAxis.setDrawAxisLine(false);
        xAxis.setEnabled(true);

        YAxis leftAxis = ppgChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#244D24")); // Dark green grid
        leftAxis.setTextColor(Color.GREEN);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1023f);

        YAxis rightAxis = ppgChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Set description
        Description description = new Description();
        description.setText("");
        ppgChart.setDescription(description);
    }

    private void performQuery() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to access the database", Toast.LENGTH_LONG).show();
            return;
        }

        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.after(endDate)) {
            Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update UI to show loading
        dataCountText.setText("Loading...");
        statusText.setText("Querying cloud database...");
        chartContainer.setVisibility(View.GONE);

        // Clear previous results
        queryResults.clear();
        dataAdapter.notifyDataSetChanged();

        // Only query from the user's collection (avoid security issues with root collections)
        queryUserCollection();
    }

    private void queryUserCollection() {
        Log.d(TAG, "Querying user collection: " + currentUser.getUid());

        // Get reference to user's collection directly
        db.collection("users")
                .document(currentUser.getUid())
                .collection(selectedSensorType)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result == null || result.isEmpty()) {
                                dataCountText.setText("No data found");
                                statusText.setText("No data available for the selected sensor type");
                                Log.d(TAG, "No data found in user's collection");
                                return;
                            }

                            Log.d(TAG, "Found " + result.size() + " records in user's collection");
                            processQueryResults(result);
                        } else {
                            Log.w(TAG, "Error getting documents from user's collection.", task.getException());
                            dataCountText.setText("Query failed");
                            statusText.setText("Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        }
                    }
                });
    }

    private void processQueryResults(QuerySnapshot querySnapshot) {
        // Process all documents
        for (QueryDocumentSnapshot document : querySnapshot) {
            Map<String, Object> data = document.getData();
            queryResults.add(data);
        }

        // Sort data based on user's preference
        sortQueryResults();

        // Update UI
        updateDataDisplay();
    }

    private void sortQueryResults() {
        if (queryResults.isEmpty()) return;

        Collections.sort(queryResults, (data1, data2) -> {
            int result = compareTimestamps(data1, data2);
            return ascendingOrder ? result : -result; // Reverse for descending
        });
    }

    private int compareTimestamps(Map<String, Object> data1, Map<String, Object> data2) {
        // Try Raspberry Pi timestamp first
        if (data1.containsKey("timestamp") && data2.containsKey("timestamp")) {
            String ts1 = data1.get("timestamp").toString();
            String ts2 = data2.get("timestamp").toString();
            return ts1.compareTo(ts2);
        }

        // Fall back to arduino_timestamp if available
        if (data1.containsKey("arduino_timestamp") && data2.containsKey("arduino_timestamp")) {
            long ts1 = 0, ts2 = 0;

            Object obj1 = data1.get("arduino_timestamp");
            Object obj2 = data2.get("arduino_timestamp");

            if (obj1 instanceof Long) ts1 = (Long) obj1;
            else if (obj1 instanceof Integer) ts1 = ((Integer) obj1).longValue();

            if (obj2 instanceof Long) ts2 = (Long) obj2;
            else if (obj2 instanceof Integer) ts2 = ((Integer) obj2).longValue();

            return Long.compare(ts1, ts2);
        }

        // No reliable timestamp, just keep as-is
        return 0;
    }

    private void updateDataDisplay() {
        // Update the recycler view
        dataAdapter.notifyDataSetChanged();

        // Update status
        dataCountText.setText("Results: " + queryResults.size() + " records");
        statusText.setText("Query completed successfully");

        // Process chart data if needed
        if (selectedSensorType.equals("ecg_data") || selectedSensorType.equals("ppg_gravity_data")) {
            prepareChartData();
            updateVitalSigns(); // Update vital signs display
            chartContainer.setVisibility(View.VISIBLE);
        } else {
            chartContainer.setVisibility(View.GONE);
        }

        // Scroll to top
        scrollContainer.smoothScrollTo(0, 0);
    }

    private void prepareChartData() {
        if (selectedSensorType.equals("ecg_data")) {
            // Process ECG data for charting
            ArrayList<Entry> ecgEntries = new ArrayList<>();

            for (int i = 0; i < queryResults.size(); i++) {
                Map<String, Object> data = queryResults.get(i);
                float ecgValue = 0;

                if (data.containsKey("ecg_value")) {
                    Object ecgObj = data.get("ecg_value");
                    ecgValue = extractFloatValue(ecgObj);
                } else if (data.containsKey("value")) {
                    Object valueObj = data.get("value");
                    ecgValue = extractFloatValue(valueObj);
                }

                if (ecgValue > 0) {
                    // Use index position as X value to create scrolling effect
                    ecgEntries.add(new Entry(i, ecgValue));
                }
            }

            // Update ECG chart if we have data
            if (!ecgEntries.isEmpty()) {
                LineDataSet ecgDataSet;

                if (ecgChart.getData() != null && ecgChart.getData().getDataSetCount() > 0) {
                    // Reuse existing dataset to update
                    ecgDataSet = (LineDataSet) ecgChart.getData().getDataSetByIndex(0);
                    ecgDataSet.clear();
                    for (Entry entry : ecgEntries) {
                        ecgDataSet.addEntry(entry);
                    }
                    ecgDataSet.notifyDataSetChanged();
                    ecgChart.getData().notifyDataChanged();
                } else {
                    // Create new dataset
                    ecgDataSet = new LineDataSet(ecgEntries, "");
                    ecgDataSet.setColor(Color.GREEN); // Medical monitor green
                    ecgDataSet.setDrawCircles(false);
                    ecgDataSet.setDrawValues(false);
                    ecgDataSet.setLineWidth(1.5f);
                    ecgDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
                    ecgDataSet.setCubicIntensity(0.1f); // Subtle smoothing
                    ecgDataSet.setDrawFilled(false);
                    ecgDataSet.setHighlightEnabled(false);

                    LineData lineData = new LineData(ecgDataSet);
                    ecgChart.setData(lineData);
                }

                // Set to show approximately 5 seconds of data at a time (50 samples/second)
                ecgChart.setVisibleXRangeMaximum(250);

                // Move to the beginning for historical data view
                ecgChart.moveViewToX(0);
                ecgChart.notifyDataSetChanged();
                ecgChart.invalidate();

                // Show ECG chart, hide PPG chart
                ppgChart.setVisibility(View.GONE);
                ecgChart.setVisibility(View.VISIBLE);
            } else {
                ecgChart.setVisibility(View.GONE);
            }
        } else if (selectedSensorType.equals("ppg_gravity_data")) {
            // Process PPG data for charting
            ArrayList<Entry> ppgEntries = new ArrayList<>();

            for (int i = 0; i < queryResults.size(); i++) {
                Map<String, Object> data = queryResults.get(i);
                float ppgValue = 0;

                if (data.containsKey("ppg_value")) {
                    Object ppgObj = data.get("ppg_value");
                    ppgValue = extractFloatValue(ppgObj);
                } else if (data.containsKey("value")) {
                    Object valueObj = data.get("value");
                    ppgValue = extractFloatValue(valueObj);
                }

                if (ppgValue > 0) {
                    // Use index position as X value to create scrolling effect
                    ppgEntries.add(new Entry(i, ppgValue));
                }
            }

            // Update PPG chart if we have data
            if (!ppgEntries.isEmpty()) {
                LineDataSet ppgDataSet;

                if (ppgChart.getData() != null && ppgChart.getData().getDataSetCount() > 0) {
                    // Reuse existing dataset to update
                    ppgDataSet = (LineDataSet) ppgChart.getData().getDataSetByIndex(0);
                    ppgDataSet.clear();
                    for (Entry entry : ppgEntries) {
                        ppgDataSet.addEntry(entry);
                    }
                    ppgDataSet.notifyDataSetChanged();
                    ppgChart.getData().notifyDataChanged();
                } else {
                    // Create new dataset
                    ppgDataSet = new LineDataSet(ppgEntries, "");
                    ppgDataSet.setColor(Color.GREEN); // Medical monitor green
                    ppgDataSet.setDrawCircles(false);
                    ppgDataSet.setDrawValues(false);
                    ppgDataSet.setLineWidth(1.5f);
                    ppgDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
                    ppgDataSet.setCubicIntensity(0.1f); // Subtle smoothing
                    ppgDataSet.setDrawFilled(false);
                    ppgDataSet.setHighlightEnabled(false);

                    LineData lineData = new LineData(ppgDataSet);
                    ppgChart.setData(lineData);
                }

                // Set to show approximately 5 seconds of data at a time (50 samples/second)
                ppgChart.setVisibleXRangeMaximum(250);

                // Move to the beginning for historical data view
                ppgChart.moveViewToX(0);
                ppgChart.notifyDataSetChanged();
                ppgChart.invalidate();

                // Show PPG chart, hide ECG chart
                ecgChart.setVisibility(View.GONE);
                ppgChart.setVisibility(View.VISIBLE);
            } else {
                ppgChart.setVisibility(View.GONE);
            }
        } else {
            // Hide both charts for other sensor types
            ecgChart.setVisibility(View.GONE);
            ppgChart.setVisibility(View.GONE);
        }
    }

    private void updateVitalSigns() {
        // Find the vital sign TextView elements if not already initialized
        if (ecgHrValue == null) ecgHrValue = findViewById(R.id.ecg_hr_value);
        if (ecgAvgValue == null) ecgAvgValue = findViewById(R.id.ecg_avg_value);
        if (ppgPulseValue == null) ppgPulseValue = findViewById(R.id.ppg_pulse_value);
        if (ppgAvgValue == null) ppgAvgValue = findViewById(R.id.ppg_avg_value);

        if (selectedSensorType.equals("ecg_data")) {
            // Calculate average voltage and heart rate from ECG data
            float avgVoltage = 0;
            int validDataPoints = 0;

            // Sample some points to estimate heart rate (simplified approach)
            ArrayList<Long> peakTimes = new ArrayList<>();
            float lastValue = 0;
            float threshold = 2.0f; // Threshold for peak detection - adjust as needed

            for (Map<String, Object> data : queryResults) {
                float ecgValue = 0;

                if (data.containsKey("ecg_value")) {
                    Object ecgObj = data.get("ecg_value");
                    ecgValue = extractFloatValue(ecgObj);
                } else if (data.containsKey("value")) {
                    Object valueObj = data.get("value");
                    ecgValue = extractFloatValue(valueObj);
                }

                // Accumulate valid values for average calculation
                if (ecgValue > 0) {
                    avgVoltage += ecgValue;
                    validDataPoints++;

                    // Simple peak detection for heart rate estimation
                    if (ecgValue > threshold && lastValue <= threshold && data.containsKey("arduino_timestamp")) {
                        // Found a rising edge peak
                        Object tsObj = data.get("arduino_timestamp");
                        if (tsObj instanceof Long) {
                            peakTimes.add((Long) tsObj);
                        }
                    }

                    lastValue = ecgValue;
                }
            }

            // Calculate and display average voltage
            if (validDataPoints > 0) {
                avgVoltage /= validDataPoints;
                ecgAvgValue.setText(String.format(Locale.US, "%.2f V", avgVoltage));
            } else {
                ecgAvgValue.setText("-- V");
            }

            // Calculate heart rate from peaks if we have enough data
            if (peakTimes.size() >= 2) {
                // Calculate average time between peaks
                long totalTimeBetweenPeaks = 0;
                for (int i = 1; i < peakTimes.size(); i++) {
                    totalTimeBetweenPeaks += (peakTimes.get(i) - peakTimes.get(i-1));
                }

                double avgTimeBetweenPeaks = totalTimeBetweenPeaks / (double)(peakTimes.size() - 1);
                // Convert to seconds and calculate BPM
                double secondsBetweenPeaks = avgTimeBetweenPeaks / 1000.0;
                int heartRate = (int) Math.round(60.0 / secondsBetweenPeaks);

                // Show heart rate if it seems reasonable (30-200 BPM)
                if (heartRate >= 30 && heartRate <= 200) {
                    ecgHrValue.setText(heartRate + " BPM");
                } else {
                    ecgHrValue.setText("-- BPM");
                }
            } else {
                ecgHrValue.setText("-- BPM");
            }
        } else if (selectedSensorType.equals("ppg_gravity_data")) {
            // Calculate average PPG value and pulse rate
            float avgPpgValue = 0;
            int validDataPoints = 0;

            // Sample some points to estimate pulse rate (simplified approach)
            ArrayList<Long> peakTimes = new ArrayList<>();
            float lastValue = 0;
            float threshold = 600f; // Threshold for peak detection - adjust as needed

            for (Map<String, Object> data : queryResults) {
                float ppgValue = 0;

                if (data.containsKey("ppg_value")) {
                    Object ppgObj = data.get("ppg_value");
                    ppgValue = extractFloatValue(ppgObj);
                } else if (data.containsKey("value")) {
                    Object valueObj = data.get("value");
                    ppgValue = extractFloatValue(valueObj);
                }

                // Accumulate valid values for average calculation
                if (ppgValue > 0) {
                    avgPpgValue += ppgValue;
                    validDataPoints++;

                    // Simple peak detection for pulse rate estimation
                    if (ppgValue > threshold && lastValue <= threshold && data.containsKey("arduino_timestamp")) {
                        // Found a rising edge peak
                        Object tsObj = data.get("arduino_timestamp");
                        if (tsObj instanceof Long) {
                            peakTimes.add((Long) tsObj);
                        }
                    }

                    lastValue = ppgValue;
                }
            }

            // Calculate and display average PPG value
            if (validDataPoints > 0) {
                avgPpgValue /= validDataPoints;
                ppgAvgValue.setText(String.format(Locale.US, "%.0f", avgPpgValue));
            } else {
                ppgAvgValue.setText("--");
            }

            // Calculate pulse rate from peaks if we have enough data
            if (peakTimes.size() >= 2) {
                // Calculate average time between peaks
                long totalTimeBetweenPeaks = 0;
                for (int i = 1; i < peakTimes.size(); i++) {
                    totalTimeBetweenPeaks += (peakTimes.get(i) - peakTimes.get(i-1));
                }

                double avgTimeBetweenPeaks = totalTimeBetweenPeaks / (double)(peakTimes.size() - 1);
                // Convert to seconds and calculate BPM
                double secondsBetweenPeaks = avgTimeBetweenPeaks / 1000.0;
                int pulseRate = (int) Math.round(60.0 / secondsBetweenPeaks);

                // Show pulse rate if it seems reasonable (30-200 BPM)
                if (pulseRate >= 30 && pulseRate <= 200) {
                    ppgPulseValue.setText(pulseRate + " BPM");
                } else {
                    ppgPulseValue.setText("-- BPM");
                }
            } else {
                ppgPulseValue.setText("-- BPM");
            }
        }
    }

    // Helper method to extract float values from different object types
    private float extractFloatValue(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        } else if (obj instanceof Long) {
            return ((Long) obj).floatValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).floatValue();
        } else if (obj instanceof String) {
            try {
                return Float.parseFloat((String) obj);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Could not parse string value: " + obj);
            }
        }
        return 0f;
    }

    /**
     * RecyclerView adapter for sensor data
     */
    private class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.ViewHolder> {

        private List<Map<String, Object>> dataList;

        public SensorDataAdapter(List<Map<String, Object>> dataList) {
            this.dataList = dataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sensor_data, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> data = dataList.get(position);
            StringBuilder dataText = new StringBuilder();

            // Add all fields to the text
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                dataText.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n");
            }

            holder.dataTextView.setText(dataText.toString());

            // Set record index
            holder.recordNumberTextView.setText("Record #" + (position + 1));

            // Add timestamp at the top if available
            if (data.containsKey("timestamp")) {
                holder.timestampTextView.setText("Timestamp: " + data.get("timestamp"));
                holder.timestampTextView.setVisibility(View.VISIBLE);
            } else {
                holder.timestampTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView timestampTextView;
            public TextView recordNumberTextView;
            public TextView dataTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                timestampTextView = itemView.findViewById(R.id.text_timestamp);
                recordNumberTextView = itemView.findViewById(R.id.text_record_number);
                dataTextView = itemView.findViewById(R.id.text_data);
            }
        }
    }
}
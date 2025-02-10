package com.example.physioscanner;

public class DatabaseRecord {
    private final String timestamp;
    private final double value;
    private final String label; // "Voltage" for ECG, "Heart Rate" for PPG

    public DatabaseRecord(String timestamp, double value, String label) {
        this.timestamp = timestamp;
        this.value = value;
        this.label = label;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}

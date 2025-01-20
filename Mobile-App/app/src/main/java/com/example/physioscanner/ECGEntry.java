package com.example.physioscanner;

public class ECGEntry {
    private final String timestamp;
    private final double voltage;

    public ECGEntry(String timestamp, double voltage) {
        this.timestamp = timestamp;
        this.voltage = voltage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getVoltage() {
        return voltage;
    }
}

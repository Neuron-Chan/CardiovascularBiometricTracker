package com.example.cbt;

public class BiometricData {

    private String heartRate;
    private String ecg;
    private String ppg;
    private String temperature;

    public BiometricData(String heartRate, String ecg, String ppg, String temperature) {
        this.heartRate = heartRate;
        this.ecg = ecg;
        this.ppg = ppg;
        this.temperature = temperature;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public String getEcg() {
        return ecg;
    }

    public String getPpg() {
        return ppg;
    }

    public String getTemperature() {
        return temperature;
    }
}

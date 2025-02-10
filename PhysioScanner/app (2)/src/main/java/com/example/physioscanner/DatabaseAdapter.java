package com.example.physioscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DatabaseAdapter extends RecyclerView.Adapter<DatabaseAdapter.ViewHolder> {

    private final List<ECGEntry> ecgEntries;

    public DatabaseAdapter(List<ECGEntry> ecgEntries) {
        this.ecgEntries = ecgEntries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ecg_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ECGEntry entry = ecgEntries.get(position);
        holder.timestampTextView.setText("Timestamp: " + entry.getTimestamp());
        holder.voltageTextView.setText("Voltage: " + entry.getVoltage() + " V");
    }

    @Override
    public int getItemCount() {
        return ecgEntries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView;
        TextView voltageTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.timestamp_text);
            voltageTextView = itemView.findViewById(R.id.voltage_text);
        }
    }
}

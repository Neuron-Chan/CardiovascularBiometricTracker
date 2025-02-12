package com.example.physioscanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DatabaseAdapter extends RecyclerView.Adapter<DatabaseAdapter.ViewHolder> {
    private final List<DatabaseRecord> records;

    public DatabaseAdapter(List<DatabaseRecord> records) {
        this.records = records;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ecg_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DatabaseRecord record = records.get(position);
        holder.timestampTextView.setText("Timestamp: " + record.getTimestamp());
        holder.voltageTextView.setText(record.getLabel() + ": " + record.getValue());
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView;
        TextView voltageTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.timestamp_text);
            voltageTextView = itemView.findViewById(R.id.voltage_text);
        }
    }
}

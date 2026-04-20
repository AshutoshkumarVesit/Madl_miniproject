package com.movemate;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RunAdapter extends RecyclerView.Adapter<RunAdapter.RunViewHolder> {
    private final List<RunModel> runs;
    private final OnRunActionListener actionListener;

    public interface OnRunActionListener {
        void onDelete(RunModel run, int position);
        void onShare(RunModel run);
    }

    public RunAdapter(List<RunModel> runs, OnRunActionListener actionListener) {
        this.runs = runs;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public RunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_run, parent, false);
        return new RunViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RunViewHolder holder, int position) {
        RunModel run = runs.get(position);
        holder.name.setText(run.getName());
        holder.distance.setText(String.format("%.2f km", run.getDistanceKm()));
        holder.duration.setText(formatDuration(run.getDurationMs()));
        holder.date.setText(run.getDate());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RunDetailActivity.class);
            intent.putExtra("name", run.getName());
            intent.putExtra("distance", run.getDistanceKm());
            intent.putExtra("duration", run.getDurationMs());
            intent.putExtra("calories", run.getCalories());
            intent.putExtra("date", run.getDate());
            intent.putExtra("route", run.getRouteJson());
            v.getContext().startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            Toast.makeText(v.getContext(), "Long press detected", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Run Options")
                    .setItems(new CharSequence[]{"Delete Run", "Share Run"}, (dialog, which) -> {
                        int pos = holder.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;
                        if (which == 0 && actionListener != null) {
                            actionListener.onDelete(run, pos);
                        } else if (which == 1 && actionListener != null) {
                            actionListener.onShare(run);
                        }
                    })
                    .show();
            return true;
        });
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return runs.size();
    }

    public void removeAt(int position) {
        runs.remove(position);
        notifyItemRemoved(position);
    }

    static class RunViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView distance;
        final TextView duration;
        final TextView date;

        RunViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.runName);
            distance = itemView.findViewById(R.id.runDistance);
            duration = itemView.findViewById(R.id.runDuration);
            date = itemView.findViewById(R.id.runDate);
        }
    }
}

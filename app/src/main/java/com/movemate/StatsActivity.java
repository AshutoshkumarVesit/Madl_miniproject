package com.movemate;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.movemate.firestore.FirestoreRepository;
import com.movemate.firestore.RunLogModel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {
    private SwipeRefreshLayout swipeRefreshLayout;
    private BarChart weeklyChart;
    private RecyclerView bestRecycler;
    private DBHelper dbHelper;
    private FirestoreRepository firestoreRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        dbHelper = new DBHelper(this);
        firestoreRepo = new FirestoreRepository();

        swipeRefreshLayout = findViewById(R.id.statsSwipeRefresh);
        weeklyChart = findViewById(R.id.weeklyBarChart);
        bestRecycler = findViewById(R.id.personalBestRecycler);
        if (bestRecycler != null) {
            bestRecycler.setLayoutManager(new LinearLayoutManager(this));
        }

        refreshStats();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshStats();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    private void refreshStats() {
        // Load local runs first for instant display
        List<RunModel> runs = dbHelper.getRuns();
        displayStats(runs);

        // Also fetch from Firestore for cloud sync
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firestoreRepo.getRunLogs(user.getUid(), new FirestoreRepository.RunLogsCallback() {
                @Override
                public void onSuccess(List<RunLogModel> firestoreRuns) {
                    if (firestoreRuns == null || firestoreRuns.isEmpty()) {
                        Log.d("StatsActivity", "No Firestore runs found, showing local data only");
                        return;
                    }
                    Log.d("StatsActivity", "Fetched " + firestoreRuns.size() + " runs from Firestore");

                    // Convert Firestore RunLogModel to RunModel
                    List<RunModel> cloudRuns = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    for (RunLogModel log : firestoreRuns) {
                        String date = "";
                        if (log.getTimestamp() != null) {
                            date = sdf.format(log.getTimestamp().toDate());
                        }
                        RunModel rm = new RunModel(
                                -1,
                                "Run " + date,
                                log.getDistance(),
                                log.getDuration() * 1000, // seconds to ms
                                log.getCalories(),
                                date,
                                null
                        );
                        cloudRuns.add(rm);
                    }

                    // Merge: start with local runs, add cloud-only runs
                    List<RunModel> localRuns = dbHelper.getRuns();
                    List<RunModel> merged = new ArrayList<>(localRuns);
                    for (RunModel cloudRun : cloudRuns) {
                        boolean alreadyLocal = false;
                        for (RunModel localRun : localRuns) {
                            if (Math.abs(cloudRun.getDistanceKm() - localRun.getDistanceKm()) < 0.01
                                    && cloudRun.getCalories() == localRun.getCalories()
                                    && cloudRun.getDate() != null
                                    && cloudRun.getDate().equals(localRun.getDate())) {
                                alreadyLocal = true;
                                break;
                            }
                        }
                        if (!alreadyLocal) {
                            merged.add(cloudRun);
                        }
                    }

                    displayStats(merged);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("StatsActivity", "Failed to fetch Firestore runs", e);
                    // Local data is already displayed, so no action needed
                }
            });
        }
    }

    /**
     * Displays stats UI from the given run list.
     */
    private void displayStats(List<RunModel> runs) {
        float[] weeklyDistances = computeWeeklyDistances(runs);
        setupWeeklyBarChart(weeklyChart, weeklyDistances);
        if (bestRecycler != null) {
            bestRecycler.setAdapter(new BestAdapter(buildBestList(runs)));
        }
    }

    /**
     * Aggregates run distances by day for the last 7 days (Mon–Sun).
     */
    private float[] computeWeeklyDistances(List<RunModel> runs) {
        float[] distances = new float[7]; // Mon=0 ... Sun=6
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // Build a set of the last 7 day strings
        Map<String, Integer> dayIndexMap = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -(6 - i));
            String dateStr = sdf.format(c.getTime());
            dayIndexMap.put(dateStr, i);
        }

        for (RunModel run : runs) {
            String runDate = run.getDate();
            if (runDate != null && dayIndexMap.containsKey(runDate)) {
                int idx = dayIndexMap.get(runDate);
                distances[idx] += (float) run.getDistanceKm();
            }
        }
        return distances;
    }

    private void setupWeeklyBarChart(BarChart chart, float[] weeklyDistances) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < weeklyDistances.length; i++) {
            entries.add(new BarEntry(i, weeklyDistances[i]));
        }
        BarDataSet dataSet = new BarDataSet(entries, "Weekly Distance (km)");
        dataSet.setColor(0xFFC6FF00);
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(10f);
        chart.setData(new BarData(dataSet));
        Description d = new Description();
        d.setText("");
        chart.setDescription(d);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setTextColor(0xFFFFFFFF);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFFFFFFFF);
        Legend legend = chart.getLegend();
        legend.setTextColor(0xFFFFFFFF);
        chart.setTouchEnabled(false);
        chart.animateY(600);
        chart.invalidate();
    }

    /**
     * Computes personal bests from actual run data.
     */
    private List<BestItem> buildBestList(List<RunModel> runs) {
        List<BestItem> list = new ArrayList<>();

        if (runs.isEmpty()) {
            list.add(new BestItem("No runs yet", "Start running to see your stats!", "--"));
            return list;
        }

        // Find farthest single run
        double maxDistance = 0;
        String maxDistDate = "";
        // Find fastest pace run (min/km)
        double fastestPace = Double.MAX_VALUE;
        String fastestPaceDate = "";
        // Totals
        double totalDistance = 0;
        long totalDuration = 0;
        int totalCalories = 0;

        for (RunModel run : runs) {
            totalDistance += run.getDistanceKm();
            totalDuration += run.getDurationMs();
            totalCalories += run.getCalories();

            if (run.getDistanceKm() > maxDistance) {
                maxDistance = run.getDistanceKm();
                maxDistDate = run.getDate();
            }
            if (run.getDistanceKm() > 0) {
                double pace = (run.getDurationMs() / 1000.0 / 60.0) / run.getDistanceKm();
                if (pace < fastestPace) {
                    fastestPace = pace;
                    fastestPaceDate = run.getDate();
                }
            }
        }

        list.add(new BestItem("Total Runs", "All time", String.valueOf(runs.size())));
        list.add(new BestItem("Total Distance", "All time", String.format(Locale.getDefault(), "%.2f km", totalDistance)));
        list.add(new BestItem("Farthest Run", maxDistDate, String.format(Locale.getDefault(), "%.2f km", maxDistance)));
        if (fastestPace < Double.MAX_VALUE) {
            list.add(new BestItem("Fastest Pace", fastestPaceDate, String.format(Locale.getDefault(), "%.2f min/km", fastestPace)));
        }
        list.add(new BestItem("Total Calories", "All time", String.format(Locale.getDefault(), "%d kcal", totalCalories)));

        return list;
    }

    private static class BestItem {
        final String title;
        final String subtitle;
        final String value;

        BestItem(String t, String s, String v) {
            title = t;
            subtitle = s;
            value = v;
        }
    }

    private static class BestAdapter extends RecyclerView.Adapter<BestAdapter.BestVH> {
        private final List<BestItem> data;

        BestAdapter(List<BestItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public BestVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stat_best, parent, false);
            return new BestVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BestVH holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class BestVH extends RecyclerView.ViewHolder {
            TextView title, subtitle, value;
            ImageView icon;

            BestVH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.bestTitle);
                subtitle = itemView.findViewById(R.id.bestSubtitle);
                value = itemView.findViewById(R.id.bestValue);
                icon = itemView.findViewById(R.id.bestIcon);
            }

            void bind(BestItem item) {
                title.setText(item.title);
                subtitle.setText(item.subtitle);
                value.setText(item.value);
            }
        }
    }
}


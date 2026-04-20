package com.movemate;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.movemate.firestore.FirestoreRepository;
import com.movemate.firestore.RunLogModel;
import com.movemate.notification.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private static final String GOALS_PREFS = "goals_prefs";
    private static final String WEEKLY_KEY = "weekly_goal";

    private GestureDetector gestureDetector;
    private RunAdapter runAdapter;
    private List<RunModel> runs;
    private DBHelper dbHelper;
    private FirestoreRepository firestoreRepo;
    private SwipeRefreshLayout swipeRefreshLayout;

    // UI references
    private ImageView profileImage;
    private TextView greetingText, subtitleText, motivationText, motivationEmoji;
    private TextView weeklyTitle, weeklyDetail, weeklyPercentText;
    private LinearProgressIndicator weeklyProgress;
    private TextView currentDistance, currentCalories, currentDuration, currentRunCount;
    private BarChart dashboardBarChart;
    private PieChart dashboardPieChart;
    private LinearLayout emptyStateLayout;
    private RecyclerView recentRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize notification channels and schedule workers
        NotificationHelper.createChannels(this);
        if (NotificationHelper.areNotificationsEnabled(this)) {
            NotificationHelper.scheduleAllWorkers(this);
        }

        dbHelper = new DBHelper(this);
        firestoreRepo = new FirestoreRepository();
        gestureDetector = new GestureDetector(this, new DashboardGestureListener());

        // Bind views
        bindViews();

        // Setup swipe refresh
        swipeRefreshLayout = findViewById(R.id.dashboardSwipeRefresh);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.color_primary));
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.color_surface_container));
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshAllData();
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        // Greeting
        setupGreeting();

        // Goal edit button
        ImageView editGoalButton = findViewById(R.id.editGoalButton);
        if (editGoalButton != null) {
            editGoalButton.setOnClickListener(v -> showGoalEditDialog());
        }

        // RecyclerView
        recentRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentRecycler.setNestedScrollingEnabled(false);
        runs = dbHelper.getRuns();
        runAdapter = new RunAdapter(runs, new RunAdapter.OnRunActionListener() {
            @Override
            public void onDelete(RunModel run, int position) {
                if (dbHelper.deleteRun(run.getId())) {
                    runAdapter.removeAt(position);
                    Toast.makeText(DashboardActivity.this, "Run deleted", Toast.LENGTH_SHORT).show();
                    refreshAllData();
                }
            }

            @Override
            public void onShare(RunModel run) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, String.format("Run: %s\nDistance: %.2f km\nDuration: %s\nCalories: %d",
                        run.getName(), run.getDistanceKm(), formatDuration(run.getDurationMs()), run.getCalories()));
                startActivity(Intent.createChooser(share, "Share run"));
            }
        });
        recentRecycler.setAdapter(runAdapter);

        // Bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;
                if (id == R.id.nav_stats) {
                    startActivity(new Intent(this, StatsActivity.class));
                    return true;
                }
                if (id == R.id.nav_run) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                }
                if (id == R.id.nav_community) {
                    startActivity(new Intent(this, CommunityActivity.class));
                    return true;
                }
                if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        // Start Run FAB
        ExtendedFloatingActionButton startRunFab = findViewById(R.id.startRunFab);
        startRunFab.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        // Load all data
        refreshAllData();
    }

    private void bindViews() {
        profileImage = findViewById(R.id.profileImage);
        greetingText = findViewById(R.id.greetingText);
        subtitleText = findViewById(R.id.subtitleText);
        motivationText = findViewById(R.id.motivationText);
        motivationEmoji = findViewById(R.id.motivationEmoji);
        weeklyTitle = findViewById(R.id.weeklyTitle);
        weeklyDetail = findViewById(R.id.weeklyDetail);
        weeklyPercentText = findViewById(R.id.weeklyPercentText);
        weeklyProgress = findViewById(R.id.weeklyProgress);
        currentDistance = findViewById(R.id.currentDistance);
        currentCalories = findViewById(R.id.currentCalories);
        currentDuration = findViewById(R.id.currentDuration);
        currentRunCount = findViewById(R.id.currentRunCount);
        dashboardBarChart = findViewById(R.id.dashboardBarChart);
        dashboardPieChart = findViewById(R.id.dashboardPieChart);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        recentRecycler = findViewById(R.id.recentRecycler);
    }

    private void setupGreeting() {
        String name = SessionManager.getUserName(this);
        if (name == null || name.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                name = user.getDisplayName();
                if (name == null || name.isEmpty()) {
                    name = user.getEmail();
                }
            }
        }

        // Time-based greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        String displayName = (name != null && !name.isEmpty()) ? name : "Runner";
        if (greetingText != null) {
            greetingText.setText(greeting + ", " + displayName + " 👋");
        }
    }

    private void refreshAllData() {
        if (profileImage != null) {
            ImageUtils.loadProfileImage(this, profileImage);
        }

        // Load local runs first for instant display
        runs.clear();
        runs.addAll(dbHelper.getRuns());
        runAdapter.notifyDataSetChanged();
        updateDashboardUI();

        // Also fetch from Firestore for cloud sync
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firestoreRepo.getRunLogs(user.getUid(), new FirestoreRepository.RunLogsCallback() {
                @Override
                public void onSuccess(List<RunLogModel> firestoreRuns) {
                    if (firestoreRuns == null || firestoreRuns.isEmpty()) {
                        Log.d("DashboardActivity", "No Firestore runs found, showing local data only");
                        return;
                    }
                    Log.d("DashboardActivity", "Fetched " + firestoreRuns.size() + " runs from Firestore");

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

                    runs.clear();
                    runs.addAll(merged);
                    runAdapter.notifyDataSetChanged();
                    updateDashboardUI();
                }

                @Override
                public void onError(Exception e) {
                    Log.e("DashboardActivity", "Failed to fetch Firestore runs", e);
                    // Local data is already displayed, so no action needed
                }
            });
        }
    }

    /**
     * Updates all dashboard stats, charts, and UI elements from the current runs list.
     */
    private void updateDashboardUI() {
        // Toggle empty state
        if (emptyStateLayout != null) {
            if (runs.isEmpty()) {
                emptyStateLayout.setVisibility(View.VISIBLE);
                recentRecycler.setVisibility(View.GONE);
            } else {
                emptyStateLayout.setVisibility(View.GONE);
                recentRecycler.setVisibility(View.VISIBLE);
            }
        }

        // Compute total stats from real data
        double totalDistance = 0;
        int totalCalories = 0;
        long totalDurationMs = 0;
        for (RunModel run : runs) {
            totalDistance += run.getDistanceKm();
            totalCalories += run.getCalories();
            totalDurationMs += run.getDurationMs();
        }
        long totalMinutes = totalDurationMs / 1000 / 60;

        // Animate stats
        animateTextValue(currentDistance, 0, (float) totalDistance, true);
        animateTextValue(currentCalories, 0, totalCalories, false);
        animateTextValue(currentDuration, 0, totalMinutes, false);
        if (currentRunCount != null) {
            animateTextValue(currentRunCount, 0, runs.size(), false);
        }

        // Update weekly goal from SharedPreferences
        updateWeeklyGoal();

        // Update subtitle based on runs
        if (subtitleText != null) {
            if (runs.isEmpty()) {
                subtitleText.setText("Start your first run today!");
            } else {
                subtitleText.setText(String.format(Locale.getDefault(), "%d total runs • %.1f km covered", runs.size(), totalDistance));
            }
        }

        // Charts
        setupBarChart();
        setupPieChart();
    }

    private void updateWeeklyGoal() {
        SharedPreferences prefs = getSharedPreferences(GOALS_PREFS, MODE_PRIVATE);
        float weeklyGoal = prefs.getFloat(WEEKLY_KEY, 20f);

        // Compute distance for last 7 days
        double weekDistance = computeWeekDistance(runs);
        float progress = (float) Math.min(weekDistance, weeklyGoal);
        int percent = weeklyGoal > 0 ? (int) ((weekDistance / weeklyGoal) * 100) : 0;
        percent = Math.min(percent, 100);

        if (weeklyTitle != null) {
            weeklyTitle.setText(String.format(Locale.getDefault(), "Weekly Goal  •  %.0f km", weeklyGoal));
        }
        if (weeklyProgress != null) {
            weeklyProgress.setMax((int) (weeklyGoal * 10));
            weeklyProgress.setProgressCompat((int) (progress * 10), true);
        }
        if (weeklyDetail != null) {
            double left = Math.max(0, weeklyGoal - weekDistance);
            weeklyDetail.setText(String.format(Locale.getDefault(), "%.1f km done / %.1f km left", weekDistance, left));
        }
        if (weeklyPercentText != null) {
            weeklyPercentText.setText(percent + "%");
        }

        // Update motivational banner
        updateMotivation(percent, weekDistance);
    }

    private void updateMotivation(int percent, double weekDistance) {
        String message;
        String emoji;
        if (runs.isEmpty()) {
            emoji = "🚀";
            message = "Start your first run and begin your fitness journey!";
        } else if (percent >= 100) {
            emoji = "🎉";
            message = "Goal crushed! You're on fire this week!";
        } else if (percent >= 75) {
            emoji = "💪";
            message = "Almost there! Just a little push to reach your goal!";
        } else if (percent >= 50) {
            emoji = "🔥";
            message = "Halfway done! Keep the momentum going!";
        } else if (percent >= 25) {
            emoji = "⚡";
            message = "Great start! Stay consistent to hit your target!";
        } else {
            emoji = "👟";
            message = String.format(Locale.getDefault(), "%.1f km logged this week. Let's pick up the pace!", weekDistance);
        }
        if (motivationEmoji != null) motivationEmoji.setText(emoji);
        if (motivationText != null) motivationText.setText(message);
    }

    private double computeWeekDistance(List<RunModel> allRuns) {
        double total = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // Compute dates for last 7 days
        Map<String, Boolean> last7Days = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            last7Days.put(sdf.format(c.getTime()), true);
        }

        for (RunModel run : allRuns) {
            if (run.getDate() != null && last7Days.containsKey(run.getDate())) {
                total += run.getDistanceKm();
            }
        }
        return total;
    }

    private void showGoalEditDialog() {
        SharedPreferences prefs = getSharedPreferences(GOALS_PREFS, MODE_PRIVATE);
        float currentGoal = prefs.getFloat(WEEKLY_KEY, 20f);

        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
        // Build a simple dialog with EditText
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf((int) currentGoal));
        input.setTextColor(getResources().getColor(R.color.color_on_surface));
        input.setHintTextColor(getResources().getColor(R.color.color_on_surface_secondary));
        input.setHint("Enter weekly goal in km");
        input.setPadding(48, 32, 48, 32);
        input.selectAll();

        new MaterialAlertDialogBuilder(this)
                .setTitle("🎯 Set Weekly Goal")
                .setMessage("How many kilometers do you want to run this week?")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    float newGoal = currentGoal;
                    try {
                        newGoal = Float.parseFloat(text);
                        if (newGoal <= 0) newGoal = currentGoal;
                    } catch (Exception e) {
                        // keep current goal
                    }
                    prefs.edit().putFloat(WEEKLY_KEY, newGoal).apply();
                    Toast.makeText(this, "Weekly goal updated to " + (int) newGoal + " km! 🎯", Toast.LENGTH_SHORT).show();
                    updateWeeklyGoal();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ======================== CHARTS ========================

    private void setupBarChart() {
        if (dashboardBarChart == null) return;

        float[] weeklyDistances = computeWeeklyDistances(runs);
        String[] dayLabels = getLast7DayLabels();

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < weeklyDistances.length; i++) {
            entries.add(new BarEntry(i, weeklyDistances[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Distance (km)");
        dataSet.setColor(0xFF00E5FF); // neon cyan
        dataSet.setValueTextColor(0xFFBAC9CC);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        dashboardBarChart.setData(barData);

        Description desc = new Description();
        desc.setText("");
        dashboardBarChart.setDescription(desc);
        dashboardBarChart.getAxisRight().setEnabled(false);
        dashboardBarChart.getAxisLeft().setTextColor(0xFFBAC9CC);
        dashboardBarChart.getAxisLeft().setGridColor(0x33FFFFFF);
        dashboardBarChart.getAxisLeft().setAxisMinimum(0f);

        XAxis xAxis = dashboardBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFFBAC9CC);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setDrawGridLines(false);

        Legend legend = dashboardBarChart.getLegend();
        legend.setTextColor(0xFFBAC9CC);
        legend.setEnabled(false);

        dashboardBarChart.setTouchEnabled(true);
        dashboardBarChart.setDragEnabled(false);
        dashboardBarChart.setScaleEnabled(false);
        dashboardBarChart.setDrawGridBackground(false);
        dashboardBarChart.animateY(800);
        dashboardBarChart.invalidate();
    }

    private void setupPieChart() {
        if (dashboardPieChart == null) return;

        // Show calorie breakdown of last 5 runs
        List<PieEntry> entries = new ArrayList<>();
        int[] pieColors = new int[]{
                0xFF00E5FF, // cyan
                0xFFEDB1FF, // purple
                0xFFD9C8FF, // violet
                0xFFC6FF00, // lime
                0xFFFF6B6B  // coral
        };

        int count = Math.min(runs.size(), 5);
        if (count == 0) {
            entries.add(new PieEntry(1, "No Data"));
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(new int[]{0xFF313442});
            dataSet.setValueTextColor(0xFFBAC9CC);
            dataSet.setValueTextSize(12f);
            PieData pieData = new PieData(dataSet);
            dashboardPieChart.setData(pieData);
        } else {
            List<Integer> colors = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                RunModel run = runs.get(i);
                String label = run.getName() != null && !run.getName().isEmpty() ? run.getName() : "Run " + (i + 1);
                entries.add(new PieEntry(run.getCalories(), label));
                colors.add(pieColors[i % pieColors.length]);
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setValueTextColor(0xFFFFFFFF);
            dataSet.setValueTextSize(11f);
            dataSet.setSliceSpace(2f);

            PieData pieData = new PieData(dataSet);
            dashboardPieChart.setData(pieData);
        }

        Description desc = new Description();
        desc.setText("");
        dashboardPieChart.setDescription(desc);
        dashboardPieChart.setDrawHoleEnabled(true);
        dashboardPieChart.setHoleColor(0xFF171B28);
        dashboardPieChart.setHoleRadius(45f);
        dashboardPieChart.setTransparentCircleRadius(50f);
        dashboardPieChart.setTransparentCircleColor(0xFF171B28);
        dashboardPieChart.setCenterText("Calories");
        dashboardPieChart.setCenterTextColor(0xFFBAC9CC);
        dashboardPieChart.setCenterTextSize(14f);

        Legend legend = dashboardPieChart.getLegend();
        legend.setTextColor(0xFFBAC9CC);
        legend.setTextSize(11f);
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);

        dashboardPieChart.setTouchEnabled(true);
        dashboardPieChart.animateY(800);
        dashboardPieChart.invalidate();
    }

    private float[] computeWeeklyDistances(List<RunModel> allRuns) {
        float[] distances = new float[7];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        Map<String, Integer> dayIndexMap = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -(6 - i));
            dayIndexMap.put(sdf.format(c.getTime()), i);
        }

        for (RunModel run : allRuns) {
            String runDate = run.getDate();
            if (runDate != null && dayIndexMap.containsKey(runDate)) {
                int idx = dayIndexMap.get(runDate);
                distances[idx] += (float) run.getDistanceKm();
            }
        }
        return distances;
    }

    private String[] getLast7DayLabels() {
        String[] labels = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -(6 - i));
            labels[i] = sdf.format(c.getTime());
        }
        return labels;
    }

    // ======================== ANIMATIONS ========================

    private void animateTextValue(TextView textView, float from, float to, boolean isDecimal) {
        if (textView == null) return;
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            if (isDecimal) {
                textView.setText(String.format(Locale.getDefault(), "%.1f", val));
            } else {
                textView.setText(String.valueOf((int) val));
            }
        });
        animator.start();
    }

    // ======================== UTILITIES ========================

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private class DashboardGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 120;
        private static final int SWIPE_VELOCITY_THRESHOLD = 120;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    onBackPressed();
                } else {
                    startActivity(new Intent(DashboardActivity.this, StatsActivity.class));
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Toast.makeText(DashboardActivity.this, "Double tap – starting run 🏃", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllData();
    }
}

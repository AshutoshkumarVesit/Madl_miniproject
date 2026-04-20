package com.movemate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        RecyclerView grid = findViewById(R.id.achievementGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 2));

        List<Achievement> achievements = buildAchievements();
        grid.setAdapter(new AchievementAdapter(achievements));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_stats);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) { startActivity(new android.content.Intent(this, DashboardActivity.class)); return true; }
                if (id == R.id.nav_stats) return true;
                if (id == R.id.nav_run) { startActivity(new android.content.Intent(this, MainActivity.class)); return true; }
                if (id == R.id.nav_community) { startActivity(new android.content.Intent(this, CommunityActivity.class)); return true; }
                if (id == R.id.nav_profile) { startActivity(new android.content.Intent(this, ProfileActivity.class)); return true; }
                return false;
            });
        }
    }

    private List<Achievement> buildAchievements() {
        DBHelper dbHelper = new DBHelper(this);
        List<RunModel> runs = dbHelper.getRuns();
        double totalDistance = 0; for (RunModel r : runs) totalDistance += r.getDistance();
        int totalRuns = runs.size();

        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("First Run", totalRuns >= 1));
        list.add(new Achievement("10 km total", totalDistance >= 10));
        list.add(new Achievement("100 km milestone", totalDistance >= 100));
        list.add(new Achievement("7-day streak", totalRuns >= 7));
        return list;
    }

    private static class Achievement {
        final String title; final boolean unlocked;
        Achievement(String title, boolean unlocked) { this.title = title; this.unlocked = unlocked; }
    }

    private static class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AViewHolder> {
        private final List<Achievement> data;
        AchievementAdapter(List<Achievement> data) { this.data = data; }
        @NonNull @Override public AViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
            return new AViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull AViewHolder holder, int position) { holder.bind(data.get(position)); }
        @Override public int getItemCount() { return data.size(); }
        static class AViewHolder extends RecyclerView.ViewHolder {
            ImageView icon; TextView title; TextView status;
            AViewHolder(View itemView) { super(itemView); icon = itemView.findViewById(R.id.achievementIcon); title = itemView.findViewById(R.id.achievementTitle); status = itemView.findViewById(R.id.achievementStatus); }
            void bind(Achievement a) {
                title.setText(a.title);
                status.setText(a.unlocked ? "Unlocked" : "Locked");
                icon.setImageResource(a.unlocked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
                itemView.setAlpha(a.unlocked ? 1f : 0.5f);
            }
        }
    }
}


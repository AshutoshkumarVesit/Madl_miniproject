package com.movemate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class TrainingPlansActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_plans);

        RecyclerView recyclerView = findViewById(R.id.planRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<TrainingPlan> plans = getPlans();
        recyclerView.setAdapter(new PlanAdapter(plans, plan -> {
            Intent intent = new Intent(this, PlanDetailActivity.class);
            intent.putExtra("name", plan.name);
            intent.putExtra("weekly", plan.weeklyDistance);
            intent.putExtra("pace", plan.pace);
            intent.putExtra("schedule", plan.schedule);
            startActivity(intent);
        }));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_stats);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) { startActivity(new Intent(this, DashboardActivity.class)); return true; }
                if (id == R.id.nav_stats) return true;
                if (id == R.id.nav_run) { startActivity(new Intent(this, MainActivity.class)); return true; }
                if (id == R.id.nav_community) { startActivity(new Intent(this, CommunityActivity.class)); return true; }
                if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); return true; }
                return false;
            });
        }
    }

    private List<TrainingPlan> getPlans() {
        List<TrainingPlan> list = new ArrayList<>();
        list.add(new TrainingPlan("Beginner 5K Plan", "20 km/week", "7:00 min/km", "Mon: Rest\nTue: 3km easy\nWed: 3km easy\nThu: Rest\nFri: 4km easy\nSat: 5km long\nSun: Rest"));
        list.add(new TrainingPlan("10K Endurance Plan", "30 km/week", "6:15 min/km", "Mon: Rest\nTue: 5km tempo\nWed: 4km easy\nThu: 5km intervals\nFri: Rest\nSat: 8km long\nSun: 4km recovery"));
        list.add(new TrainingPlan("Weight Loss Running Plan", "25 km/week", "6:45 min/km", "Mon: 4km easy\nTue: 4km easy\nWed: 4km easy\nThu: Rest\nFri: 5km tempo\nSat: 6km long\nSun: Rest"));
        return list;
    }

    private static class TrainingPlan {
        String name, weeklyDistance, pace, schedule;
        TrainingPlan(String name, String weeklyDistance, String pace, String schedule) {
            this.name = name; this.weeklyDistance = weeklyDistance; this.pace = pace; this.schedule = schedule;
        }
    }

    private interface PlanClickListener { void onClick(TrainingPlan plan); }

    private static class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanVH> {
        private final List<TrainingPlan> data; private final PlanClickListener listener;
        PlanAdapter(List<TrainingPlan> data, PlanClickListener listener) { this.data = data; this.listener = listener; }
        @NonNull @Override public PlanVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_plan, parent, false);
            return new PlanVH(v);
        }
        @Override public void onBindViewHolder(@NonNull PlanVH holder, int position) { holder.bind(data.get(position), listener); }
        @Override public int getItemCount() { return data.size(); }
        static class PlanVH extends RecyclerView.ViewHolder {
            TextView name, weekly, pace;
            PlanVH(View itemView) { super(itemView); name = itemView.findViewById(R.id.planName); weekly = itemView.findViewById(R.id.planWeekly); pace = itemView.findViewById(R.id.planPace); }
            void bind(TrainingPlan plan, PlanClickListener listener) {
                name.setText(plan.name); weekly.setText(plan.weeklyDistance); pace.setText(plan.pace);
                itemView.setOnClickListener(v -> listener.onClick(plan));
            }
        }
    }
}


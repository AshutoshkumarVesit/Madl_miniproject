package com.movemate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PlanDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail);

        TextView name = findViewById(R.id.planDetailName);
        TextView weekly = findViewById(R.id.planDetailWeekly);
        TextView pace = findViewById(R.id.planDetailPace);
        TextView schedule = findViewById(R.id.planDetailSchedule);

        name.setText(getIntent().getStringExtra("name"));
        weekly.setText(getIntent().getStringExtra("weekly"));
        pace.setText(getIntent().getStringExtra("pace"));
        schedule.setText(getIntent().getStringExtra("schedule"));
    }
}


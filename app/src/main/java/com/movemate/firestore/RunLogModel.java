package com.movemate.firestore;

import com.google.firebase.Timestamp;

public class RunLogModel {
    private double distance;    // e.g., kilometers
    private int steps;
    private int calories;
    private long duration;      // duration in seconds
    private Timestamp timestamp;

    public RunLogModel() {
        // Needed for Firestore deserialization
    }

    public RunLogModel(double distance, int steps, int calories, long duration) {
        this.distance = distance;
        this.steps = steps;
        this.calories = calories;
        this.duration = duration;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}


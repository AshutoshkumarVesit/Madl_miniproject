package com.movemate;

public class RunModel {
    private final int id;
    private final String name;
    private final double distanceKm;
    private final long durationMs;
    private final int calories;
    private final String date;
    private final String routeJson;

    public RunModel(int id, String name, double distanceKm, long durationMs, int calories, String date, String routeJson) {
        this.id = id;
        this.name = name;
        this.distanceKm = distanceKm;
        this.durationMs = durationMs;
        this.calories = calories;
        this.date = date;
        this.routeJson = routeJson;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getDistanceKm() { return distanceKm; }
    public long getDurationMs() { return durationMs; }
    public int getCalories() { return calories; }
    public String getDate() { return date; }
    public String getRouteJson() { return routeJson; }
    public double getDistance() { return distanceKm; }
    public long getDuration() { return durationMs; }
}

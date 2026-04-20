package com.movemate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunDetailActivity extends AppCompatActivity {
    private MapView map;
    private double distance;
    private long duration;
    private int calories;
    private String date;
    private String routeJson;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureMapCache();
        setContentView(R.layout.activity_run_detail);

        name = getIntent().getStringExtra("name");
        distance = getIntent().getDoubleExtra("distance", 0);
        duration = getIntent().getLongExtra("duration", 0);
        calories = getIntent().getIntExtra("calories", 0);
        date = getIntent().getStringExtra("date");
        routeJson = getIntent().getStringExtra("route");

        ((TextView) findViewById(R.id.detailTitle)).setText(name != null ? name : "Run Details");
        ((TextView) findViewById(R.id.detailDate)).setText(date != null ? date : "");
        ((TextView) findViewById(R.id.detailDistance)).setText(String.format(Locale.getDefault(), "Distance: %.2f km", distance));
        ((TextView) findViewById(R.id.detailTime)).setText("Time: " + formatDuration(duration));
        double pace = distance > 0 ? (duration / 1000.0 / 60.0) / distance : 0;
        ((TextView) findViewById(R.id.detailPace)).setText(distance > 0 ? String.format(Locale.getDefault(), "Pace: %.2f min/km", pace) : "Pace: 0:00 /km");
        ((TextView) findViewById(R.id.detailCalories)).setText(String.format(Locale.getDefault(), "Calories: %d kcal", calories));

        map = findViewById(R.id.detailMap);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);

            List<GeoPoint> points = decodeRoute(routeJson);
            if (!points.isEmpty()) {
                Polyline routeLine = new Polyline();
                routeLine.setColor(0xFF76FF03);
                routeLine.setWidth(8f);
                routeLine.setPoints(points);
                map.getOverlays().add(routeLine);

                map.getController().setZoom(15.0);
                map.getController().setCenter(points.get(0));
            }
        }
    }

    private void configureMapCache() {
        IConfigurationProvider config = Configuration.getInstance();
        File basePath = new File(getCacheDir(), "osmdroid");
        File tileCache = new File(basePath, "tiles");
        if (!tileCache.exists()) {
            tileCache.mkdirs();
        }
        config.setUserAgentValue(getPackageName());
        config.setOsmdroidBasePath(basePath);
        config.setOsmdroidTileCache(tileCache);
        config.setTileFileSystemCacheMaxBytes(80L * 1024L * 1024L);
        config.setTileFileSystemCacheTrimBytes(60L * 1024L * 1024L);
    }

    private List<GeoPoint> decodeRoute(String route) {
        List<GeoPoint> pts = new ArrayList<>();
        if (route == null || route.isEmpty()) return pts;
        try {
            JSONArray arr = new JSONArray(route);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                pts.add(new GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pts;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}

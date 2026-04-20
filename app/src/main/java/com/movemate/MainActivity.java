package com.movemate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.movemate.firestore.FirestoreRepository;
import com.movemate.firestore.RunLogModel;
import com.movemate.notification.NotificationHelper;
import com.movemate.notification.RunTrackingService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline routeLine;
    private final List<GeoPoint> routePoints = new ArrayList<>();

    private TextView timerText;
    private TextView statDistance;
    private TextView statPace;
    private TextView statCalories;
    private Button controlButton;
    private Button stopButton;

    private long startTimeMs = 0L;
    private long pausedAccumulatedMs = 0L;
    private double distanceMeters = 0.0;
    private boolean isTracking = false;
    private boolean isPaused = false;

    private DBHelper dbHelper;
    private FirestoreRepository firestoreRepo;
    private FirebaseAuth auth;

    private String currentRunName = "";

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    enableMyLocationIfPermitted();
                    startLocationUpdates();
                } else {
                    Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // No-op: we just need to ask, the helper checks before sending
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureMapCache();
        setContentView(R.layout.activity_main);

        // Initialize notification channels and request permission on Android 13+
        NotificationHelper.createChannels(this);
        requestNotificationPermission();

        dbHelper = new DBHelper(this);
        firestoreRepo = new FirestoreRepository();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        timerText = findViewById(R.id.timerText);
        statDistance = findViewById(R.id.statDistance);
        statPace = findViewById(R.id.statPace);
        statCalories = findViewById(R.id.statCalories);
        controlButton = findViewById(R.id.runControlButton);
        stopButton = findViewById(R.id.stopRunButton);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_run);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, DashboardActivity.class));
                    return true;
                }
                if (id == R.id.nav_stats) {
                    startActivity(new Intent(this, StatsActivity.class));
                    return true;
                }
                if (id == R.id.nav_run) return true;
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

        map = findViewById(R.id.mapFragment);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.getController().setZoom(17.0);

            routeLine = new Polyline();
            routeLine.setColor(0xFF76FF03);
            routeLine.setWidth(10f);
            map.getOverlays().add(routeLine);
        }

        setupLocationCallback();
        ensureUserDocument();

        controlButton.setOnClickListener(v -> {
            if (!isTracking) {
                promptForRunNameAndStart();
            } else if (isPaused) {
                resumeRun();
            } else {
                pauseRun();
            }
        });

        stopButton.setOnClickListener(v -> {
            if (isTracking) {
                stopRun();
            }
        });

        enableMyLocationIfPermitted();
        centerOnLastLocation();
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

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                List<Location> locations = locationResult.getLocations();
                if (locations == null || locations.isEmpty()) return;
                Location latest = locations.get(locations.size() - 1);
                onLocationUpdate(latest);
            }
        };
    }

    private void promptForRunNameAndStart() {
        final EditText input = new EditText(this);
        input.setHint("Morning Run");
        String suggested = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date());
        input.setText("Run " + suggested);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Name this run")
                .setView(input)
                .setPositiveButton("Start", (d, which) -> {
                    currentRunName = input.getText().toString().trim();
                    if (currentRunName.isEmpty()) {
                        currentRunName = "Run " + suggested;
                    }
                    startRun();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startRun() {
        isTracking = true;
        isPaused = false;
        startTimeMs = SystemClock.elapsedRealtime();
        pausedAccumulatedMs = 0L;
        distanceMeters = 0.0;
        routePoints.clear();
        updatePolyline();
        updateStatsUi();
        controlButton.setText("Pause Run");
        startLocationUpdates();
        centerOnLastLocation();

        // Start foreground tracking service
        Intent serviceIntent = new Intent(this, RunTrackingService.class);
        serviceIntent.setAction(RunTrackingService.ACTION_START);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void resumeRun() {
        isPaused = false;
        startTimeMs = SystemClock.elapsedRealtime() - pausedAccumulatedMs;
        controlButton.setText("Pause Run");
        startLocationUpdates();
    }

    private void pauseRun() {
        isPaused = true;
        pausedAccumulatedMs = SystemClock.elapsedRealtime() - startTimeMs;
        controlButton.setText("Start Run");
        stopLocationUpdates();
    }

    private void stopRun() {
        stopLocationUpdates();
        isTracking = false;
        isPaused = false;
        controlButton.setText("Start Run");
        long durationMs = SystemClock.elapsedRealtime() - startTimeMs;
        double distanceKm = distanceMeters / 1000.0;
        int calories = (int) (distanceKm * 60); // simple formula
        saveRun(currentRunName, distanceKm, durationMs, calories, new ArrayList<>(routePoints));
        pushRunToFirestore(distanceKm, durationMs, calories);
        Toast.makeText(this, "Run saved", Toast.LENGTH_SHORT).show();

        // Stop foreground tracking service
        Intent serviceIntent = new Intent(this, RunTrackingService.class);
        serviceIntent.setAction(RunTrackingService.ACTION_STOP);
        startService(serviceIntent);

        // Send run-completed notification
        String distStr = String.format(Locale.getDefault(), "%.2f km", distanceKm);
        String durStr = formatDuration(durationMs);
        NotificationHelper.notify(this, NotificationHelper.ID_RUN_COMPLETED,
                NotificationHelper.buildRunCompleted(this, distStr, durStr, calories));

        currentRunName = "";
    }

    private void onLocationUpdate(Location location) {
        if (!isTracking || isPaused) return;

        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (!routePoints.isEmpty()) {
            GeoPoint last = routePoints.get(routePoints.size() - 1);
            float[] results = new float[1];
            Location.distanceBetween(last.getLatitude(), last.getLongitude(), point.getLatitude(), point.getLongitude(), results);
            distanceMeters += results[0];
        }
        routePoints.add(point);

        updatePolyline();
        updateStatsUi();

        if (map != null) {
            map.getController().animateTo(point);
        }

        // Update foreground tracking notification
        updateTrackingNotification();
    }

    private void updatePolyline() {
        if (map == null || routeLine == null) return;
        routeLine.setPoints(routePoints);
        map.invalidate();
    }

    private void updateStatsUi() {
        long elapsedMs = isTracking ? (isPaused ? pausedAccumulatedMs : SystemClock.elapsedRealtime() - startTimeMs) : 0L;
        statDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceMeters / 1000.0));
        timerText.setText(formatDuration(elapsedMs));
        double pace = distanceMeters > 0 ? (elapsedMs / 1000.0 / 60.0) / (distanceMeters / 1000.0) : 0;
        statPace.setText(distanceMeters > 0 ? String.format(Locale.getDefault(), "%.2f min/km", pace) : "0:00 /km");
        int calories = (int) ((distanceMeters / 1000.0) * 60);
        statCalories.setText(String.format(Locale.getDefault(), "%d kcal", calories));
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateTrackingNotification() {
        String distance = String.format(Locale.getDefault(), "%.2f km", distanceMeters / 1000.0);
        long elapsedMs = SystemClock.elapsedRealtime() - startTimeMs;
        String duration = formatDuration(elapsedMs);
        Intent updateIntent = new Intent(this, RunTrackingService.class);
        updateIntent.setAction(RunTrackingService.ACTION_UPDATE);
        updateIntent.putExtra(RunTrackingService.EXTRA_DISTANCE, distance);
        updateIntent.putExtra(RunTrackingService.EXTRA_DURATION, duration);
        startService(updateIntent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        LocationRequest request = LocationRequest.create()
                .setInterval(4000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
        enableMyLocationIfPermitted();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void saveRun(String name, double distanceKm, long durationMs, int calories, List<GeoPoint> coords) {
        try {
            JSONArray array = new JSONArray();
            for (GeoPoint latLng : coords) {
                JSONObject obj = new JSONObject();
                obj.put("lat", latLng.getLatitude());
                obj.put("lng", latLng.getLongitude());
                array.put(obj);
            }
            String routeJson = array.toString();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String safeName = name == null || name.isEmpty() ? "Run " + date : name;
            dbHelper.insertRun(safeName, distanceKm, durationMs, calories, date, routeJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void centerOnLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && map != null) {
                GeoPoint p = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                map.getController().setZoom(17.0);
                map.getController().setCenter(p);
            }
        });
    }

    private void enableMyLocationIfPermitted() {
        if (map == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (myLocationOverlay == null) {
                myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
                map.getOverlays().add(myLocationOverlay);
            }
            myLocationOverlay.enableMyLocation();
            centerOnLastLocation();
        }
    }

    private void ensureUserDocument() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w("MainActivity", "No Firebase user; skipping Firestore user doc creation");
            return;
        }
        String displayName = SessionManager.getUserName(this);
        if (displayName == null || displayName.isEmpty()) {
            displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
        }
        firestoreRepo.createUserIfNotExists(user.getUid(), displayName != null ? displayName : "Runner")
                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to ensure user doc", e));
    }

    private void pushRunToFirestore(double distanceKm, long durationMs, int calories) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w("MainActivity", "pushRunToFirestore: user not signed in");
            return;
        }
        Log.d("MainActivity", "pushRunToFirestore: uid=" + user.getUid()
                + " dist=" + distanceKm + " dur=" + (durationMs / 1000) + "s cal=" + calories);
        RunLogModel run = new RunLogModel(distanceKm, 0, calories, durationMs / 1000);
        firestoreRepo.addRun(user.getUid(), run)
                .addOnSuccessListener(ref -> {
                    Log.d("MainActivity", "Run added to Firestore successfully: " + ref.getId());
                    firestoreRepo.updateTotalDistance(user.getUid(), distanceKm)
                            .addOnSuccessListener(v -> Log.d("MainActivity", "totalDistance updated on Firestore"))
                            .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update totalDistance", e));
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to add run to Firestore: " + e.getMessage(), e));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (isTracking && !isPaused) {
            pauseRun();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onBackPressed() {
        if (isTracking) {
            stopRun();
        }
        super.onBackPressed();
    }
}

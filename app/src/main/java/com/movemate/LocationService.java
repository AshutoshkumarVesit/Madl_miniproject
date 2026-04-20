package com.movemate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Lightweight helper for starting/stopping high-accuracy location updates.
 */
public class LocationService {
    private final FusedLocationProviderClient client;

    public LocationService(Context context) {
        client = LocationServices.getFusedLocationProviderClient(context);
    }

    public void start(Context context, @NonNull LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest request = LocationRequest.create()
                .setInterval(4000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        client.requestLocationUpdates(request, callback, context.getMainLooper());
    }

    public void stop(@NonNull LocationCallback callback) {
        client.removeLocationUpdates(callback);
    }
}


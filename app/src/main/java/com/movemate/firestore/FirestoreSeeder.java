package com.movemate.firestore;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Seeds Firestore with dummy users and run data for testing.
 * Call FirestoreSeeder.seed() once from any Activity to populate.
 */
public class FirestoreSeeder {

    private static final String TAG = "FirestoreSeeder";

    private static final String[][] DUMMY_USERS = {
            {"dummy_rahul",   "Rahul Sharma"},
            {"dummy_priya",   "Priya Patel"},
            {"dummy_arjun",   "Arjun Reddy"},
            {"dummy_sneha",   "Sneha Gupta"},
            {"dummy_vikram",  "Vikram Singh"},
            {"dummy_ananya",  "Ananya Iyer"},
            {"dummy_rohan",   "Rohan Desai"},
            {"dummy_kavya",   "Kavya Nair"},
            {"dummy_aditya",  "Aditya Joshi"},
            {"dummy_meera",   "Meera Kapoor"},
    };

    public static void seed() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Random random = new Random(42); // Fixed seed for reproducibility

        for (String[] user : DUMMY_USERS) {
            String uid = user[0];
            String name = user[1];

            // Generate 5-15 runs per user
            int runCount = 5 + random.nextInt(11);
            double totalDistance = 0;

            for (int i = 0; i < runCount; i++) {
                // Random distance: 1.5 – 15.0 km
                double distance = 1.5 + (random.nextDouble() * 13.5);
                distance = Math.round(distance * 100.0) / 100.0;

                // Pace: 4.5 – 7.5 min/km → duration in seconds
                double paceMinPerKm = 4.5 + (random.nextDouble() * 3.0);
                long durationSeconds = (long) (distance * paceMinPerKm * 60);

                // Steps: ~1300 steps per km
                int steps = (int) (distance * (1200 + random.nextInt(200)));

                // Calories: ~60 kcal per km
                int calories = (int) (distance * (55 + random.nextInt(15)));

                // Timestamp: random day in the last 30 days
                long now = System.currentTimeMillis();
                long daysAgo = random.nextInt(30);
                long ts = now - (daysAgo * 24 * 60 * 60 * 1000L);
                ts -= random.nextInt(12) * 60 * 60 * 1000L; // Random hour

                Map<String, Object> runData = new HashMap<>();
                runData.put("distance", distance);
                runData.put("steps", steps);
                runData.put("calories", calories);
                runData.put("duration", durationSeconds);
                runData.put("timestamp", new Timestamp(new Date(ts)));

                totalDistance += distance;

                db.collection("users").document(uid)
                        .collection("runs")
                        .add(runData)
                        .addOnSuccessListener(ref -> Log.d(TAG, "Run added: " + ref.getId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Run add failed", e));
            }

            // Create user doc with name and totalDistance
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("totalDistance", Math.round(totalDistance * 100.0) / 100.0);

            db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener(v -> Log.d(TAG, "User created: " + name))
                    .addOnFailureListener(e -> Log.e(TAG, "User create failed: " + name, e));
        }

        Log.d(TAG, "🚀 Firestore seeding started for " + DUMMY_USERS.length + " users!");
    }
}

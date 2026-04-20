package com.movemate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileAvatar;
    private Uri cameraImageUri;

    // Launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileAvatar = findViewById(R.id.profileAvatar);
        
        // Load initial profile image
        ImageUtils.loadProfileImage(this, profileAvatar);

        profileAvatar.setOnClickListener(v -> showImageSourceDialog());

        setupLaunchers();

        // Display user info from Firebase Auth / SessionManager
        TextView usernameText = findViewById(R.id.username);
        TextView userLevelText = findViewById(R.id.userLevel);
        TextView totalRunsText = findViewById(R.id.totalRuns);
        TextView totalDistanceText = findViewById(R.id.totalDistance);
        TextView averagePaceText = findViewById(R.id.averagePace);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = SessionManager.getUserName(this);
        String email = SessionManager.getUserEmail(this);

        if ((displayName == null || displayName.isEmpty()) && firebaseUser != null) {
            displayName = firebaseUser.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = firebaseUser.getEmail();
            }
        }
        if ((email == null || email.isEmpty()) && firebaseUser != null) {
            email = firebaseUser.getEmail();
        }

        if (usernameText != null) {
            usernameText.setText(displayName != null ? displayName : "User");
        }
        if (userLevelText != null) {
            userLevelText.setText(email != null ? email : "");
        }

        // Load stats from local SQLite database
        DBHelper dbHelper = new DBHelper(this);
        List<RunModel> runs = dbHelper.getRuns();
        int totalRuns = runs.size();
        double totalDistance = 0;
        long totalDuration = 0;
        for (RunModel run : runs) {
            totalDistance += run.getDistanceKm();
            totalDuration += run.getDurationMs();
        }

        if (totalRunsText != null) {
            totalRunsText.setText(String.valueOf(totalRuns));
        }
        if (totalDistanceText != null) {
            totalDistanceText.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
        }
        if (averagePaceText != null) {
            if (totalDistance > 0) {
                double avgPace = (totalDuration / 1000.0 / 60.0) / totalDistance; // min/km
                averagePaceText.setText(String.format(Locale.getDefault(), "%.1f min/km", avgPace));
            } else {
                averagePaceText.setText("0:00 /km");
            }
        }

        // Navigate to sub-screens
        TextView settingAchievements = findViewById(R.id.settingAchievements);
        if (settingAchievements != null) {
            settingAchievements.setOnClickListener(v ->
                    startActivity(new Intent(this, AchievementsActivity.class)));
        }
        TextView settingGeneral = findViewById(R.id.settingGeneral);
        if (settingGeneral != null) {
            settingGeneral.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));
        }
        TextView settingTraining = findViewById(R.id.settingTraining);
        if (settingTraining != null) {
            settingTraining.setOnClickListener(v ->
                    startActivity(new Intent(this, TrainingPlansActivity.class)));
        }

        // Logout button
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            SessionManager.logoutUser(this);
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        // Also handle the text "Logout" setting item
        TextView settingLogout = findViewById(R.id.settingLogout);
        if (settingLogout != null) {
            settingLogout.setOnClickListener(v -> logoutButton.performClick());
        }
    }

    private void setupLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show();
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri selectedImage = result.getData().getData();
                if (selectedImage != null) {
                    saveAndLoadImage(selectedImage);
                }
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                saveAndLoadImage(cameraImageUri);
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo 📷", "Choose from Gallery 🖼️"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else {
                        launchGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        File photoFile = new File(getCacheDir(), "temp_camera.jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
        cameraLauncher.launch(cameraImageUri);
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void saveAndLoadImage(Uri uri) {
        if (ImageUtils.saveImageFromUri(this, uri)) {
            ImageUtils.loadProfileImage(this, profileAvatar);
            Toast.makeText(this, "Profile picture updated! 🎉", Toast.LENGTH_SHORT).show();
            
            // Upload to Firestore to sync across devices
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                String base64 = encodeImageToBase64(uri);
                if (base64 != null) {
                    com.movemate.firestore.FirestoreRepository repo = new com.movemate.firestore.FirestoreRepository();
                    repo.updateProfilePhoto(firebaseUser.getUid(), base64);
                }
            }
        } else {
            Toast.makeText(this, "Failed to update profile picture.", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(imageUri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (bitmap == null) return null;
            // Scale down aggressively so we don't exceed Firestore limits
            int maxDim = 120;
            float ratio = Math.min((float) maxDim / bitmap.getWidth(), (float) maxDim / bitmap.getHeight());
            int width = Math.round(ratio * bitmap.getWidth());
            int height = Math.round(ratio * bitmap.getHeight());
            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] b = baos.toByteArray();
            return android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}


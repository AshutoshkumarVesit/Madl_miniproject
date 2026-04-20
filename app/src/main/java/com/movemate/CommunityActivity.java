package com.movemate;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;

import com.movemate.firestore.FirestoreRepository;
import com.movemate.firestore.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommunityActivity extends AppCompatActivity {
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration leaderboardListener;
    private LeaderboardAdapter adapter;
    private final List<UserModel> leaderboardData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        firestoreRepo = new FirestoreRepository();

        RecyclerView leaderboard = findViewById(R.id.leaderboardRecycler);
        leaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(leaderboardData);
        leaderboard.setAdapter(adapter);

        // Start listening to live leaderboard data from Firestore
        leaderboardListener = firestoreRepo.listenToLeaderboard(new FirestoreRepository.LeaderboardListener() {
            @Override
            public void onUpdate(List<UserModel> users) {
                leaderboardData.clear();
                leaderboardData.addAll(users);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.e("CommunityActivity", "Leaderboard listener error", e);
                Toast.makeText(CommunityActivity.this, "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
            }
        });

        Button share = findViewById(R.id.shareButton);
        share.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "I just logged a great run with MoveMate!");
            startActivity(Intent.createChooser(intent, "Share run"));
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_community);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) { startActivity(new Intent(this, DashboardActivity.class)); return true; }
                if (id == R.id.nav_stats) { startActivity(new Intent(this, StatsActivity.class)); return true; }
                if (id == R.id.nav_run) { startActivity(new Intent(this, MainActivity.class)); return true; }
                if (id == R.id.nav_community) return true;
                if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); return true; }
                return false;
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leaderboardListener != null) {
            leaderboardListener.remove();
        }
    }

    private static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LVH> {
        private final List<UserModel> data;
        LeaderboardAdapter(List<UserModel> data) { this.data = data; }
        @NonNull @Override public LVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
            return new LVH(v);
        }
        @Override public void onBindViewHolder(@NonNull LVH holder, int position) { holder.bind(data.get(position), position); }
        @Override public int getItemCount() { return data.size(); }
        static class LVH extends RecyclerView.ViewHolder {
            TextView rank, avatar, name, distance;
            ImageView avatarImage;
            LVH(View itemView) {
                super(itemView);
                rank = itemView.findViewById(R.id.rankText);
                avatar = itemView.findViewById(R.id.avatarText);
                name = itemView.findViewById(R.id.nameText);
                distance = itemView.findViewById(R.id.distanceText);
                avatarImage = itemView.findViewById(R.id.avatarImage);
            }
            void bind(UserModel user, int position) {
                String displayName = user.getName() == null || user.getName().trim().isEmpty() ? "User" : user.getName().trim();
                rank.setText(String.valueOf(position + 1));
                name.setText(displayName);
                distance.setText(String.format("%.1f km", user.getTotalDistance()));
                
                String currentUser = SessionManager.getUserName(itemView.getContext());
                if (currentUser == null || currentUser.isEmpty()) {
                    com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null) {
                        currentUser = firebaseUser.getDisplayName();
                        if (currentUser == null || currentUser.isEmpty()) {
                            currentUser = firebaseUser.getEmail();
                        }
                    }
                }
                
                String base64 = user.getPhotoBase64();
                if (base64 != null && !base64.trim().isEmpty()) {
                    try {
                        byte[] decodedString = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        if (decodedByte != null) {
                            avatarImage.setImageBitmap(decodedByte);
                            avatarImage.setVisibility(View.VISIBLE);
                            avatar.setVisibility(View.GONE);
                        } else {
                            fallbackToLocalOrText(displayName, currentUser);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fallbackToLocalOrText(displayName, currentUser);
                    }
                } else {
                    fallbackToLocalOrText(displayName, currentUser);
                }
            }

            private void fallbackToLocalOrText(String displayName, String currentUser) {
                if (displayName.equals(currentUser)) {
                    avatarImage.setVisibility(View.VISIBLE);
                    avatar.setVisibility(View.GONE);
                    ImageUtils.loadProfileImage(itemView.getContext(), avatarImage);
                } else {
                    avatarImage.setVisibility(View.GONE);
                    avatar.setVisibility(View.VISIBLE);
                    avatar.setText(displayName.substring(0, 1).toUpperCase(Locale.getDefault()));
                    GradientDrawable bg = (GradientDrawable) avatar.getBackground().mutate();
                    bg.setColor(resolveAvatarColor(displayName));
                }
            }

            private int resolveAvatarColor(String name) {
                int[] palette = new int[]{
                        0xFF26A69A,
                        0xFF5C6BC0,
                        0xFFEC407A,
                        0xFF42A5F5,
                        0xFFFF7043,
                        0xFF8D6E63
                };
                int index = Math.abs(name.hashCode()) % palette.length;
                return palette[index];
            }
        }
    }
}


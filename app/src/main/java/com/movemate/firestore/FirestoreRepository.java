package com.movemate.firestore;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreRepository {

    private final FirebaseFirestore db;
    private final CollectionReference users;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        users = db.collection("users");
    }

    public Task<Void> createUserIfNotExists(String uid, String name) {
        DocumentReference userRef = users.document(uid);
        return userRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                return Tasks.forResult(null);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("totalDistance", 0.0);
            return userRef.set(data);
        });
    }

    public Task<DocumentReference> addRun(String uid, RunLogModel run) {
        Map<String, Object> data = new HashMap<>();
        data.put("distance", run.getDistance());
        data.put("steps", run.getSteps());
        data.put("calories", run.getCalories());
        data.put("duration", run.getDuration());
        data.put("timestamp", FieldValue.serverTimestamp());
        return users.document(uid)
                .collection("runs")
                .add(data);
    }

    public Task<Void> updateTotalDistance(String uid, double distanceToAdd) {
        return users.document(uid)
                .update("totalDistance", FieldValue.increment(distanceToAdd));
    }

    public Task<Void> updateProfilePhoto(String uid, String photoBase64) {
        return users.document(uid)
                .update("photoBase64", photoBase64);
    }

    public void getRunLogs(String uid, RunLogsCallback callback) {
        users.document(uid)
                .collection("runs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RunLogModel> runs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        RunLogModel run = doc.toObject(RunLogModel.class);
                        runs.add(run);
                    }
                    callback.onSuccess(runs);
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenToRunLogs(String uid, RunLogsListener listener) {
        return users.document(uid)
                .collection("runs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (value == null) {
                        listener.onUpdate(new ArrayList<>());
                        return;
                    }
                    List<RunLogModel> runs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        runs.add(doc.toObject(RunLogModel.class));
                    }
                    listener.onUpdate(runs);
                });
    }

    public void getLeaderboard(LeaderboardCallback callback) {
        users.orderBy("totalDistance", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onSuccess(mapUsers(querySnapshot)))
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenToLeaderboard(LeaderboardListener listener) {
        return users.orderBy("totalDistance", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (value != null) {
                        listener.onUpdate(mapUsers(value));
                    }
                });
    }

    private List<UserModel> mapUsers(QuerySnapshot snapshot) {
        List<UserModel> usersList = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            String uid = doc.getId();
            String name = doc.getString("name");
            Double distance = doc.getDouble("totalDistance");
            String photoBase64 = doc.getString("photoBase64");
            usersList.add(new UserModel(
                    uid,
                    name == null ? "" : name, 
                    distance == null ? 0.0 : distance,
                    photoBase64
            ));
        }
        return usersList;
    }

    public interface RunLogsCallback {
        void onSuccess(List<RunLogModel> runs);
        void onError(Exception e);
    }

    public interface RunLogsListener {
        void onUpdate(List<RunLogModel> runs);
        void onError(Exception e);
    }

    public interface LeaderboardCallback {
        void onSuccess(List<UserModel> users);
        void onError(Exception e);
    }

    public interface LeaderboardListener {
        void onUpdate(List<UserModel> users);
        void onError(Exception e);
    }
}


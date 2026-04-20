package com.movemate.firestore;

public class UserModel {
    private String uid;
    private String name;
    private double totalDistance;
    private String photoBase64;

    public UserModel() {
        // Needed for Firestore deserialization
    }

    public UserModel(String uid, String name, double totalDistance, String photoBase64) {
        this.uid = uid;
        this.name = name;
        this.totalDistance = totalDistance;
        this.photoBase64 = photoBase64;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
}


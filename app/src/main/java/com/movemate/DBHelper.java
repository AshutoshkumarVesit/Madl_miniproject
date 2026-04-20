package com.movemate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "movemate.db";
    private static final int DB_VERSION = 3;

    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    private static final String TABLE_RUNS = "runs";
    private static final String RUN_ID = "id";
    private static final String RUN_NAME = "name";
    private static final String RUN_DISTANCE = "distance";
    private static final String RUN_DURATION = "duration";
    private static final String RUN_CALORIES = "calories";
    private static final String RUN_DATE = "date";
    private static final String RUN_ROUTE = "route";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT" +
                ")";
        String createRuns = "CREATE TABLE " + TABLE_RUNS + " (" +
                RUN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RUN_NAME + " TEXT, " +
                RUN_DISTANCE + " REAL, " +
                RUN_DURATION + " INTEGER, " +
                RUN_CALORIES + " INTEGER, " +
                RUN_DATE + " TEXT, " +
                RUN_ROUTE + " TEXT" +
                ")";
        db.execSQL(createUsers);
        db.execSQL(createRuns);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RUNS);
        onCreate(db);
    }

    public boolean insertUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_EMAIL + "=? AND " + COL_PASSWORD + "=?";
        String[] args = new String[]{email, password};
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_ID}, selection, args, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean userExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_EMAIL + "=?";
        String[] args = new String[]{email};
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_ID}, selection, args, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public String getUserName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = new String[]{COL_NAME};
        String selection = COL_EMAIL + "=?";
        String[] args = new String[]{email};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, args, null, null, null);
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
        }
        cursor.close();
        return name;
    }

    public boolean insertRun(String name, double distanceKm, long durationMs, int calories, String date, String routeJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RUN_NAME, name);
        values.put(RUN_DISTANCE, distanceKm);
        values.put(RUN_DURATION, durationMs);
        values.put(RUN_CALORIES, calories);
        values.put(RUN_DATE, date);
        values.put(RUN_ROUTE, routeJson);
        long result = db.insert(TABLE_RUNS, null, values);
        return result != -1;
    }

    public List<RunModel> getRuns() {
        List<RunModel> runs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_RUNS, null, null, null, null, null, RUN_ID + " DESC");
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(RUN_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(RUN_NAME));
            double distance = cursor.getDouble(cursor.getColumnIndexOrThrow(RUN_DISTANCE));
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(RUN_DURATION));
            int calories = cursor.getInt(cursor.getColumnIndexOrThrow(RUN_CALORIES));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(RUN_DATE));
            String route = cursor.getString(cursor.getColumnIndexOrThrow(RUN_ROUTE));
            runs.add(new RunModel(id, name, distance, duration, calories, date, route));
        }
        cursor.close();
        return runs;
    }

    public boolean deleteRun(int runId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_RUNS, RUN_ID + "=?", new String[]{String.valueOf(runId)});
        return rows > 0;
    }
}

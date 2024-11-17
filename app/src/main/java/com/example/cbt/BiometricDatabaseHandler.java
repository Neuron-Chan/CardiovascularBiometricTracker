package com.example.cbt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BiometricDatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "biometrics.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_BIOMETRICS = "biometrics";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_HEART_RATE = "heart_rate";
    private static final String COLUMN_ECG = "ecg";
    private static final String COLUMN_PPG = "ppg";
    private static final String COLUMN_TEMPERATURE = "temperature";

    public BiometricDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BIOMETRICS_TABLE = "CREATE TABLE " + TABLE_BIOMETRICS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_HEART_RATE + " TEXT,"
                + COLUMN_ECG + " TEXT,"
                + COLUMN_PPG + " TEXT,"
                + COLUMN_TEMPERATURE + " TEXT" + ")";
        db.execSQL(CREATE_BIOMETRICS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIOMETRICS);
        onCreate(db);
    }

    // Add a new biometric data entry
    public void addBiometricData(BiometricData data) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_HEART_RATE, data.getHeartRate());
        values.put(COLUMN_ECG, data.getEcg());
        values.put(COLUMN_PPG, data.getPpg());
        values.put(COLUMN_TEMPERATURE, data.getTemperature());

        db.insert(TABLE_BIOMETRICS, null, values);
        db.close();
    }

    // Get the latest biometric data
    public BiometricData getLatestBiometricData() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_BIOMETRICS + " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            String heartRate = cursor.getString(cursor.getColumnIndex(COLUMN_HEART_RATE));
            String ecg = cursor.getString(cursor.getColumnIndex(COLUMN_ECG));
            String ppg = cursor.getString(cursor.getColumnIndex(COLUMN_PPG));
            String temperature = cursor.getString(cursor.getColumnIndex(COLUMN_TEMPERATURE));

            cursor.close();
            return new BiometricData(heartRate, ecg, ppg, temperature);
        } else {
            return null;
        }
    }
}

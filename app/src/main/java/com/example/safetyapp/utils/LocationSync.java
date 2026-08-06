package com.example.safetyapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.safetyapp.SettingsActivity;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class LocationSync {

    public static void pushLocation(Context context, double lat, double lon) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String myPhone = prefs.getString(SettingsActivity.KEY_MY_PHONE, "");
        if (myPhone == null || myPhone.trim().isEmpty()) return;

        // Clean phone number for Firebase key (e.g. +91 12345 67890 -> 911234567890)
        String phoneKey = myPhone.replaceAll("[^0-9]", "");
        if (phoneKey.isEmpty()) return;

        Map<String, Object> locData = new HashMap<>();
        locData.put("latitude", lat);
        locData.put("longitude", lon);
        locData.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance().getReference("locations").child(phoneKey).setValue(locData);
    }
}

package com.example.safetyapp.utils;

import android.content.Context;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationHelper {

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onLocationFailed();
    }

    public static void getLastLocation(Context context, LocationCallback callback) {
        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(context);

        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    callback.onLocationReceived(
                            location.getLatitude(), location.getLongitude());
                } else {
                    callback.onLocationFailed();
                }
            }).addOnFailureListener(e -> callback.onLocationFailed());
        } catch (SecurityException e) {
            callback.onLocationFailed();
        }
    }
}
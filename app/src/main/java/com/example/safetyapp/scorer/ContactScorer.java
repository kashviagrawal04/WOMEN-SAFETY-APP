package com.example.safetyapp.scorer;

import com.example.safetyapp.model.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContactScorer {

    public static List<String> getTopContacts(
            List<Contact> contacts, double userLat, double userLon, int topN) {

        if (contacts == null || contacts.isEmpty()) {
            List<String> helpline = new ArrayList<>();
            helpline.add("1091");
            return helpline;
        }

        // Step 1: calculate distance for all contacts
        double maxDistance = 0;
        for (Contact c : contacts) {
            double d = haversine(userLat, userLon, c.getLatitude(), c.getLongitude());
            c.setDistance(d);
            if (d > maxDistance) maxDistance = d;
        }

        // Step 2: compute score (lower score = better = gets picked)
        for (Contact c : contacts) {
            double normalizedDistance = (maxDistance > 0)
                    ? c.getDistance() / maxDistance : 0;
            c.setScore((c.getPriority() * 0.7) + (normalizedDistance * 0.3));
        }

        // Step 3: sort ascending by score
        Collections.sort(contacts, (a, b) -> Double.compare(a.getScore(), b.getScore()));

        // Step 4: pick top N phone numbers
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, contacts.size()); i++) {
            result.add(contacts.get(i).getPhone());
        }

        // Step 5: always append helpline last
        result.add("1091");

        return result;
    }

    // Also returns names (for the confirm dialog UI)
    public static List<Contact> getTopContactObjects(
            List<Contact> contacts, double userLat, double userLon, int topN) {

        if (contacts == null || contacts.isEmpty()) return new ArrayList<>();

        double maxDistance = 0;
        for (Contact c : contacts) {
            double d = haversine(userLat, userLon, c.getLatitude(), c.getLongitude());
            c.setDistance(d);
            if (d > maxDistance) maxDistance = d;
        }

        for (Contact c : contacts) {
            double normalizedDistance = (maxDistance > 0)
                    ? c.getDistance() / maxDistance : 0;
            c.setScore((c.getPriority() * 0.7) + (normalizedDistance * 0.3));
        }

        Collections.sort(contacts, (a, b) -> Double.compare(a.getScore(), b.getScore()));

        return contacts.subList(0, Math.min(topN, contacts.size()));
    }

    // Haversine formula — calculates distance in km between two coordinates
    private static double haversine(double lat1, double lon1,
                                    double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
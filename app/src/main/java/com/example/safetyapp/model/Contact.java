package com.example.safetyapp.model;

public class Contact {
    public String name;
    public String phone;
    public int priority;       // 1 = highest priority
    public double latitude;
    public double longitude;
    public double distance;    // calculated at runtime
    public double score;       // calculated at runtime

    public Contact(String name, String phone, int priority,
                   double latitude, double longitude) {
        this.name = name;
        this.phone = phone;
        this.priority = priority;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = 0;
        this.score = 0;
    }

    // Getters
    public String getName()   { return name; }
    public String getPhone()  { return phone; }
    public int getPriority()  { return priority; }
    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }
    public double getDistance()  { return distance; }
    public double getScore()     { return score; }

    // Setters
    public void setDistance(double distance) { this.distance = distance; }
    public void setScore(double score)       { this.score = score; }

    @Override
    public String toString() {
        return "Contact{name='" + name + "', phone='" + phone +
                "', priority=" + priority + ", score=" + score + "}";
    }
}
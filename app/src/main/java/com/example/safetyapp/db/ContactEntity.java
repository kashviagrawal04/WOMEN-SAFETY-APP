package com.example.safetyapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String phone;
    public int priority;
    public double latitude;
    public double longitude;

    public ContactEntity(String name, String phone, int priority,
                         double latitude, double longitude) {
        this.name = name;
        this.phone = phone;
        this.priority = priority;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
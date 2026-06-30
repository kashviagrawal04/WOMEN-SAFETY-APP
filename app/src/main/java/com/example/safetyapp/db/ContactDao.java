package com.example.safetyapp.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ContactDao {

    @Insert
    void insert(ContactEntity contact);

    @Update
    void update(ContactEntity contact);

    @Delete
    void delete(ContactEntity contact);

    @Query("SELECT * FROM contacts ORDER BY priority ASC")
    LiveData<List<ContactEntity>> getAllContacts();

    @Query("SELECT * FROM contacts ORDER BY priority ASC")
    List<ContactEntity> getAllContactsSync();

    @Query("DELETE FROM contacts")
    void deleteAll();
}
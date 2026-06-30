package com.example.safetyapp;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.safetyapp.db.AppDatabase;
import com.example.safetyapp.db.ContactEntity;
import com.example.safetyapp.model.Contact;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME     = "marvin_prefs";
    public static final String KEY_GUARDIAN   = "guardian_number";
    public static final String KEY_CALL_TRUSTED  = "call_trusted";
    public static final String KEY_CALL_HELPLINE = "call_helpline";

    private ContactAdapter adapter;
    private List<Contact> contactList = new ArrayList<>();
    private SharedPreferences prefs;

    private Switch switchCallTrusted;
    private Switch switchCallHelpline;
    private EditText etGuardianNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Contacts RecyclerView
        RecyclerView rv = findViewById(R.id.rvContacts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(contactList, pos -> deleteContact(pos));
        rv.setAdapter(adapter);

        // Switches
        switchCallTrusted  = findViewById(R.id.switchCallTrusted);
        switchCallHelpline = findViewById(R.id.switchCallHelpline);
        etGuardianNumber   = findViewById(R.id.etGuardianNumber);

        // Restore saved prefs
        switchCallTrusted.setChecked(prefs.getBoolean(KEY_CALL_TRUSTED, true));
        switchCallHelpline.setChecked(prefs.getBoolean(KEY_CALL_HELPLINE, false));
        etGuardianNumber.setText(prefs.getString(KEY_GUARDIAN, ""));

        loadContacts();

        findViewById(R.id.btnAddContact).setOnClickListener(v -> showAddDialog());

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String guardianNum = etGuardianNumber.getText().toString().trim();
        prefs.edit()
                .putBoolean(KEY_CALL_TRUSTED,  switchCallTrusted.isChecked())
                .putBoolean(KEY_CALL_HELPLINE, switchCallHelpline.isChecked())
                .putString(KEY_GUARDIAN, guardianNum)
                .apply();
        Toast.makeText(this, "✓ Guard protocols updated", Toast.LENGTH_SHORT).show();
    }

    private void loadContacts() {
        new Thread(() -> {
            List<ContactEntity> entities =
                    AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            contactList.clear();
            for (ContactEntity e : entities)
                contactList.add(new Contact(e.name, e.phone, e.priority, e.latitude, e.longitude));
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }).start();
    }

    private void deleteContact(int pos) {
        if (pos < 0 || pos >= contactList.size()) return;
        Contact c = contactList.get(pos);
        new Thread(() -> {
            // Find matching entity and delete
            List<ContactEntity> entities =
                    AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            for (ContactEntity e : entities) {
                if (e.name.equals(c.getName()) && e.phone.equals(c.getPhone())) {
                    AppDatabase.getInstance(this).contactDao().delete(e);
                    break;
                }
            }
        }).start();
        contactList.remove(pos);
        adapter.notifyItemRemoved(pos);
    }

    private void showAddDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_contact);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etName     = dialog.findViewById(R.id.etName);
        EditText etPhone    = dialog.findViewById(R.id.etPhone);
        EditText etPriority = dialog.findViewById(R.id.etPriority);

        dialog.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name  = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String prioS = etPriority.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Name and phone are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!phone.matches("[0-9+\\- ]{7,15}")) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            int priority = prioS.isEmpty() ? 5 : Integer.parseInt(prioS);
            priority = Math.max(1, Math.min(10, priority)); // clamp 1–10

            int finalPriority = priority;
            ContactEntity entity = new ContactEntity(name, phone, finalPriority, 0.0, 0.0);
            new Thread(() -> {
                AppDatabase.getInstance(this).contactDao().insert(entity);
                runOnUiThread(() -> {
                    contactList.add(new Contact(name, phone, finalPriority, 0.0, 0.0));
                    adapter.notifyItemInserted(contactList.size() - 1);
                    dialog.dismiss();
                    Toast.makeText(this, "✓ Contact added", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        dialog.show();
    }
}

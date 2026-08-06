package com.example.safetyapp;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
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
    public static final String KEY_MY_PHONE   = "my_phone_number";
    public static final String KEY_SHAKE_SOS  = "shake_sos";
    public static final String KEY_CALL_TRUSTED  = "call_trusted";
    public static final String KEY_CALL_HELPLINE = "call_helpline";

    private ContactAdapter adapter;
    private List<Contact> contactList = new ArrayList<>();
    private SharedPreferences prefs;

    private SwitchCompat switchCallTrusted;
    private SwitchCompat switchCallHelpline;
    private SwitchCompat switchShakeSos;
    private EditText etGuardianNumber;
    private EditText etMyPhoneNumber;

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
        switchShakeSos     = findViewById(R.id.switchShakeSos);
        etGuardianNumber   = findViewById(R.id.etGuardianNumber);
        etMyPhoneNumber    = findViewById(R.id.etMyPhoneNumber);

        // Restore saved prefs
        switchCallTrusted.setChecked(prefs.getBoolean(KEY_CALL_TRUSTED, true));
        switchCallHelpline.setChecked(prefs.getBoolean(KEY_CALL_HELPLINE, false));
        switchShakeSos.setChecked(prefs.getBoolean(KEY_SHAKE_SOS, false));
        etGuardianNumber.setText(prefs.getString(KEY_GUARDIAN, ""));
        if (etMyPhoneNumber != null) {
            etMyPhoneNumber.setText(prefs.getString(KEY_MY_PHONE, ""));
        }

        loadContacts();

        findViewById(R.id.btnAddContact).setOnClickListener(v -> showAddDialog());

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String guardianNum = etGuardianNumber.getText().toString().trim();
        String myPhoneNum = etMyPhoneNumber != null ? etMyPhoneNumber.getText().toString().trim() : "";
        boolean shakeEnabled = switchShakeSos.isChecked();
        
        prefs.edit()
                .putBoolean(KEY_CALL_TRUSTED,  switchCallTrusted.isChecked())
                .putBoolean(KEY_CALL_HELPLINE, switchCallHelpline.isChecked())
                .putBoolean(KEY_SHAKE_SOS, shakeEnabled)
                .putString(KEY_GUARDIAN, guardianNum)
                .putString(KEY_MY_PHONE, myPhoneNum)
                .apply();
                
        android.content.Intent shakeIntent = new android.content.Intent(this, com.example.safetyapp.service.ShakeDetectionService.class);
        if (shakeEnabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(shakeIntent);
            } else {
                startService(shakeIntent);
            }
        } else {
            stopService(shakeIntent);
        }
        
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName     = dialog.findViewById(R.id.etName);
        EditText etPhone    = dialog.findViewById(R.id.etPhone);
        EditText etPriority = dialog.findViewById(R.id.etPriority);
        EditText etLatitude = dialog.findViewById(R.id.etLatitude);
        EditText etLongitude = dialog.findViewById(R.id.etLongitude);

        dialog.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name  = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String prioS = etPriority.getText().toString().trim();
            String latS  = etLatitude != null && etLatitude.getText() != null ? etLatitude.getText().toString().trim() : "";
            String lonS  = etLongitude != null && etLongitude.getText() != null ? etLongitude.getText().toString().trim() : "";

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
            double lat = 0.0;
            double lon = 0.0;
            try {
                if (!latS.isEmpty()) lat = Double.parseDouble(latS);
                if (!lonS.isEmpty()) lon = Double.parseDouble(lonS);
            } catch (NumberFormatException e) {
                // Ignore parsing errors, keep default 0.0
            }

            ContactEntity entity = new ContactEntity(name, phone, finalPriority, lat, lon);
            new Thread(() -> {
                AppDatabase.getInstance(this).contactDao().insert(entity);
                runOnUiThread(() -> {
                    contactList.add(new Contact(name, phone, finalPriority, lat, lon));
                    adapter.notifyItemInserted(contactList.size() - 1);
                    dialog.dismiss();
                    Toast.makeText(this, "✓ Contact added", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        dialog.show();
    }
}

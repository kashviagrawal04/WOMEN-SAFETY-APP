package com.example.safetyapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.safetyapp.db.AppDatabase;
import com.example.safetyapp.db.ContactEntity;
import com.example.safetyapp.model.Contact;
import com.example.safetyapp.scorer.ContactScorer;
import com.example.safetyapp.service.SosService;
import com.example.safetyapp.utils.LocationHelper;
import com.example.safetyapp.utils.PermissionHelper;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnSos;
    TextView tvStatus, tvTopContacts;
    LinearLayout navContacts, navSettings, navGuardian;
    MarvinDetector marvinDetector;
    double currentLat = 0, currentLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSos        = findViewById(R.id.btnSos);
        tvStatus      = findViewById(R.id.tvStatus);
        tvTopContacts = findViewById(R.id.tvTopContacts);
        navContacts   = findViewById(R.id.navContacts);
        navSettings   = findViewById(R.id.navSettings);
        navGuardian   = findViewById(R.id.navGuardian);

        startPulseAnimation();

        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestPermissions(this);
        } else {
            fetchLocation();
            loadTopContactsPreview();
            startMarvinDetector();
        }

        btnSos.setOnClickListener(v -> showConfirmDialog());

        navSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // Contacts tab — now fully implemented
        navContacts.setOnClickListener(v ->
                startActivity(new Intent(this, ContactsActivity.class)));

        // Guardian tab stays on main screen
        if (navGuardian != null)
            navGuardian.setOnClickListener(v ->
                    Toast.makeText(this, "Guardian AI is active", Toast.LENGTH_SHORT).show());
    }

    private void startMarvinDetector() {
        marvinDetector = new MarvinDetector(this, () -> {
            runOnUiThread(() -> {
                tvStatus.setText("🎙 \"Marvin\" detected!");
                showConfirmDialog();
            });
        });
        marvinDetector.startListening();
        tvStatus.setText("🎙 Listening for \"Marvin\"...");
    }

    private void startPulseAnimation() {
        android.view.View ring1 = findViewById(R.id.pulseRing1);
        android.view.View ring2 = findViewById(R.id.pulseRing2);
        if (ring1 == null || ring2 == null) return;

        ObjectAnimator sx1 = ObjectAnimator.ofFloat(ring1, "scaleX", 1f, 1.6f, 1f);
        ObjectAnimator sy1 = ObjectAnimator.ofFloat(ring1, "scaleY", 1f, 1.6f, 1f);
        ObjectAnimator a1  = ObjectAnimator.ofFloat(ring1, "alpha", 0.22f, 0f, 0.22f);
        sx1.setDuration(2500); sx1.setRepeatCount(-1);
        sy1.setDuration(2500); sy1.setRepeatCount(-1);
        a1.setDuration(2500);  a1.setRepeatCount(-1);
        sx1.start(); sy1.start(); a1.start();

        ObjectAnimator sx2 = ObjectAnimator.ofFloat(ring2, "scaleX", 1f, 1.6f, 1f);
        ObjectAnimator sy2 = ObjectAnimator.ofFloat(ring2, "scaleY", 1f, 1.6f, 1f);
        ObjectAnimator a2  = ObjectAnimator.ofFloat(ring2, "alpha", 0.12f, 0f, 0.12f);
        sx2.setDuration(2500); sx2.setRepeatCount(-1); sx2.setStartDelay(1200);
        sy2.setDuration(2500); sy2.setRepeatCount(-1); sy2.setStartDelay(1200);
        a2.setDuration(2500);  a2.setRepeatCount(-1);  a2.setStartDelay(1200);
        sx2.start(); sy2.start(); a2.start();
    }

    private void fetchLocation() {
        tvStatus.setText("📍 Location: Fetching...");
        LocationHelper.getLastLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLon = longitude;
                runOnUiThread(() -> tvStatus.setText("🎙 Listening for \"Marvin\"..."));
                loadTopContactsPreview();
            }
            @Override
            public void onLocationFailed() {
                runOnUiThread(() -> tvStatus.setText("🎙 Listening for \"Marvin\"..."));
            }
        });
    }

    private void loadTopContactsPreview() {
        new Thread(() -> {
            List<ContactEntity> entities =
                    AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            List<Contact> contacts = new ArrayList<>();
            for (ContactEntity e : entities)
                contacts.add(new Contact(e.name, e.phone, e.priority,
                        e.latitude, e.longitude));

            List<Contact> top = ContactScorer.getTopContactObjects(
                    contacts, currentLat, currentLon, 4);

            StringBuilder sb = new StringBuilder();
            for (Contact c : top)
                sb.append("• ").append(c.getName()).append("\n");
            sb.append("• Helpline 1091");

            String preview = top.isEmpty()
                    ? "No contacts yet — add some in Settings"
                    : sb.toString().trim();

            runOnUiThread(() -> tvTopContacts.setText(preview));
        }).start();
    }

    private void showConfirmDialog() {
        new Thread(() -> {
            List<ContactEntity> entities =
                    AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            List<Contact> contacts = new ArrayList<>();
            for (ContactEntity e : entities)
                contacts.add(new Contact(e.name, e.phone, e.priority,
                        e.latitude, e.longitude));

            List<Contact> top = ContactScorer.getTopContactObjects(
                    contacts, currentLat, currentLon, 4);

            StringBuilder names = new StringBuilder();
            for (Contact c : top)
                names.append("• ").append(c.getName()).append("\n");
            names.append("• Helpline 1091");

            runOnUiThread(() ->
                    new AlertDialog.Builder(this)
                            .setTitle("🆘 Send SOS Alert?")
                            .setMessage("SMS will be sent to:\n\n" + names)
                            .setPositiveButton("SEND NOW", (d, w) -> triggerSos())
                            .setNegativeButton("Cancel", null)
                            .show());
        }).start();
    }

    private void triggerSos() {
        startForegroundService(new Intent(this, SosService.class));
        startActivity(new Intent(this, EmergencyActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            if (PermissionHelper.allGranted(grantResults)) {
                fetchLocation();
                loadTopContactsPreview();
                startMarvinDetector();
            } else {
                tvStatus.setText("⚠ Permissions required to activate guardian");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTopContactsPreview(); // Refresh after returning from settings/contacts
        if (marvinDetector != null && PermissionHelper.hasAllPermissions(this)) {
            marvinDetector.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (marvinDetector != null) marvinDetector.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (marvinDetector != null) marvinDetector.release();
    }
}

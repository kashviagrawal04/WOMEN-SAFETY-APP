package com.example.safetyapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.safetyapp.db.AppDatabase;
import com.example.safetyapp.db.ContactEntity;
import com.example.safetyapp.model.Contact;
import com.example.safetyapp.scorer.ContactScorer;
import com.example.safetyapp.service.SosService;
import com.example.safetyapp.utils.LocationHelper;
import java.util.ArrayList;
import java.util.List;

public class EmergencyActivity extends AppCompatActivity {

    private LinearLayout llAlertedContacts;
    private TextView tvAlertCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        llAlertedContacts = findViewById(R.id.llAlertedContacts);
        tvAlertCount      = findViewById(R.id.tvAlertCount);

        startPulseAnimation();
        loadAlertedContacts();

        findViewById(R.id.btnCancelEmergency).setOnClickListener(v -> {
            stopService(new Intent(this, SosService.class));
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadAlertedContacts() {
        LocationHelper.getLastLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double lat, double lon) {
                fetchAndDisplay(lat, lon);
            }
            @Override
            public void onLocationFailed() {
                fetchAndDisplay(0, 0);
            }
        });
    }

    private void fetchAndDisplay(double lat, double lon) {
        new Thread(() -> {
            List<ContactEntity> entities =
                    AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            List<Contact> contacts = new ArrayList<>();
            for (ContactEntity e : entities)
                contacts.add(new Contact(e.name, e.phone, e.priority, e.latitude, e.longitude));

            List<Contact> top = ContactScorer.getTopContactObjects(contacts, lat, lon, 4);

            runOnUiThread(() -> {
                int count = top.size() + 1; // +1 for helpline
                tvAlertCount.setText(count + " contacts");
                llAlertedContacts.removeAllViews();

                for (Contact c : top) {
                    View item = LayoutInflater.from(this)
                            .inflate(R.layout.item_alerted_contact, llAlertedContacts, false);
                    TextView tvAvatar = item.findViewById(R.id.tvAlertAvatar);
                    TextView tvName   = item.findViewById(R.id.tvAlertName);
                    TextView tvPhone  = item.findViewById(R.id.tvAlertPhone);

                    String initial = (c.getName() != null && !c.getName().isEmpty())
                            ? String.valueOf(c.getName().charAt(0)).toUpperCase() : "?";
                    tvAvatar.setText(initial);
                    tvName.setText(c.getName());
                    tvPhone.setText(c.getPhone());
                    llAlertedContacts.addView(item);
                }

                // Always add helpline row
                View helplineItem = LayoutInflater.from(this)
                        .inflate(R.layout.item_alerted_contact, llAlertedContacts, false);
                ((TextView) helplineItem.findViewById(R.id.tvAlertAvatar)).setText("🆘");
                ((TextView) helplineItem.findViewById(R.id.tvAlertName)).setText("Women Helpline");
                ((TextView) helplineItem.findViewById(R.id.tvAlertPhone)).setText("1091");
                llAlertedContacts.addView(helplineItem);
            });
        }).start();
    }

    private void startPulseAnimation() {
        View ring1 = findViewById(R.id.emergPulse1);
        View ring2 = findViewById(R.id.emergPulse2);
        if (ring1 == null || ring2 == null) return;

        ObjectAnimator sx1 = ObjectAnimator.ofFloat(ring1, "scaleX", 1f, 1.6f, 1f);
        ObjectAnimator sy1 = ObjectAnimator.ofFloat(ring1, "scaleY", 1f, 1.6f, 1f);
        ObjectAnimator a1  = ObjectAnimator.ofFloat(ring1, "alpha", 0.22f, 0f, 0.22f);
        sx1.setDuration(2000); sx1.setRepeatCount(-1);
        sy1.setDuration(2000); sy1.setRepeatCount(-1);
        a1.setDuration(2000);  a1.setRepeatCount(-1);
        sx1.start(); sy1.start(); a1.start();

        ObjectAnimator sx2 = ObjectAnimator.ofFloat(ring2, "scaleX", 1f, 1.6f, 1f);
        ObjectAnimator sy2 = ObjectAnimator.ofFloat(ring2, "scaleY", 1f, 1.6f, 1f);
        ObjectAnimator a2  = ObjectAnimator.ofFloat(ring2, "alpha", 0.12f, 0f, 0.12f);
        sx2.setDuration(2000); sx2.setRepeatCount(-1); sx2.setStartDelay(1000);
        sy2.setDuration(2000); sy2.setRepeatCount(-1); sy2.setStartDelay(1000);
        a2.setDuration(2000);  a2.setRepeatCount(-1);  a2.setStartDelay(1000);
        sx2.start(); sy2.start(); a2.start();
    }
}

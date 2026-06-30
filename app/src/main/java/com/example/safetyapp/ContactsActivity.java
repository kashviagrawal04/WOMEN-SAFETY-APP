package com.example.safetyapp;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.safetyapp.db.AppDatabase;
import com.example.safetyapp.db.ContactEntity;
import com.example.safetyapp.model.Contact;
import com.example.safetyapp.scorer.ContactScorer;
import com.example.safetyapp.utils.LocationHelper;
import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private ContactAdapter adapter;
    private List<Contact> contactList = new ArrayList<>();
    private double currentLat = 0, currentLon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        RecyclerView rv = findViewById(R.id.rvAllContacts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(contactList, pos -> deleteContact(pos));
        rv.setAdapter(adapter);

        // Fetch location to show ranked order
        LocationHelper.getLastLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double lat, double lon) {
                currentLat = lat;
                currentLon = lon;
                loadContacts();
            }
            @Override
            public void onLocationFailed() {
                loadContacts();
            }
        });

        findViewById(R.id.btnAddContactFab).setOnClickListener(v -> showAddDialog());

        // Back nav
        TextView tvBack = findViewById(R.id.tvBackContacts);
        if (tvBack != null) tvBack.setOnClickListener(v -> finish());
    }

    private void loadContacts() {
        new Thread(() -> {
            List<ContactEntity> entities =
                    AppDatabase.getInstance(this).contactDao().getAllContactsSync();
            List<Contact> all = new ArrayList<>();
            for (ContactEntity e : entities)
                all.add(new Contact(e.name, e.phone, e.priority, e.latitude, e.longitude));

            // Rank by score so user can see who gets alerted first
            if (currentLat != 0 || currentLon != 0) {
                all = ContactScorer.getTopContactObjects(all, currentLat, currentLon, all.size());
            }

            List<Contact> ranked = all;
            runOnUiThread(() -> {
                contactList.clear();
                contactList.addAll(ranked);
                adapter.notifyDataSetChanged();

                TextView tvCount = findViewById(R.id.tvContactCount);
                if (tvCount != null)
                    tvCount.setText(contactList.size() + " in your circle");
            });
        }).start();
    }

    private void deleteContact(int pos) {
        if (pos < 0 || pos >= contactList.size()) return;
        Contact c = contactList.get(pos);
        new Thread(() -> {
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
        Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Name and phone required", Toast.LENGTH_SHORT).show();
                return;
            }
            int priority = prioS.isEmpty() ? 5 : Integer.parseInt(prioS);
            priority = Math.max(1, Math.min(10, priority));
            int fp = priority;

            ContactEntity entity = new ContactEntity(name, phone, fp, 0.0, 0.0);
            new Thread(() -> {
                AppDatabase.getInstance(this).contactDao().insert(entity);
                runOnUiThread(() -> {
                    contactList.add(new Contact(name, phone, fp, 0.0, 0.0));
                    adapter.notifyItemInserted(contactList.size() - 1);
                    dialog.dismiss();
                    TextView tvCount = findViewById(R.id.tvContactCount);
                    if (tvCount != null) tvCount.setText(contactList.size() + " in your circle");
                    Toast.makeText(this, "✓ Contact added", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        dialog.show();
    }
}

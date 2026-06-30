package com.example.safetyapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.safetyapp.SettingsActivity;
import com.example.safetyapp.db.AppDatabase;
import com.example.safetyapp.db.ContactEntity;
import com.example.safetyapp.model.Contact;
import com.example.safetyapp.scorer.ContactScorer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SosService extends Service {

    private static final String TAG        = "SosService";
    private static final String CHANNEL_ID = "sos_channel";
    private static final int    NOTIF_ID   = 1;
    private static final long   RECORD_DURATION_MS = 60_000; // 60 seconds

    private MediaRecorder mediaRecorder;
    private File          audioFile;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification("Sending SOS...", "Alerting your emergency contacts"));

        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        try {
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                double lat = location != null ? location.getLatitude()  : 0;
                double lon = location != null ? location.getLongitude() : 0;

                new Thread(() -> {
                    List<ContactEntity> entities =
                            AppDatabase.getInstance(this).contactDao().getAllContactsSync();
                    List<Contact> contacts = new ArrayList<>();
                    for (ContactEntity e : entities)
                        contacts.add(new Contact(e.name, e.phone, e.priority,
                                e.latitude, e.longitude));

                    List<String> numbers = ContactScorer.getTopContacts(contacts, lat, lon, 4);

                    SharedPreferences prefs = getSharedPreferences(
                            SettingsActivity.PREFS_NAME, MODE_PRIVATE);
                    boolean callTrusted  = prefs.getBoolean(SettingsActivity.KEY_CALL_TRUSTED, true);
                    boolean callHelpline = prefs.getBoolean(SettingsActivity.KEY_CALL_HELPLINE, false);
                    String guardianNum   = prefs.getString(SettingsActivity.KEY_GUARDIAN, "");

                    // Build SMS message
                    String mapsLink = (lat != 0 || lon != 0)
                            ? "https://maps.google.com/?q=" + lat + "," + lon
                            : "Location unavailable";
                    String message = "🆘 SOS! I need help immediately.\n"
                            + "My location: " + mapsLink + "\n"
                            + "— Sent by Marvin Safety App";

                    sendSms(numbers, message);

                    // Call primary guardian if enabled
                    if (callTrusted && !guardianNum.isEmpty()) {
                        callNumber(guardianNum);
                    } else if (callHelpline) {
                        callNumber("1091");
                    }

                    // Start audio recording as evidence
                    startAudioRecording();

                    updateNotification("SOS Sent ✓", "Recording audio evidence for 60s");
                }).start();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Location failed", e);
                // Still send SOS without location
                new Thread(() -> {
                    List<ContactEntity> entities =
                            AppDatabase.getInstance(this).contactDao().getAllContactsSync();
                    List<Contact> contacts = new ArrayList<>();
                    for (ContactEntity en : entities)
                        contacts.add(new Contact(en.name, en.phone, en.priority, 0, 0));
                    List<String> numbers = ContactScorer.getTopContacts(contacts, 0, 0, 4);
                    sendSms(numbers, "🆘 SOS! I need help. — Marvin Safety App");
                    startAudioRecording();
                }).start();
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
        }

        return START_NOT_STICKY;
    }

    private void sendSms(List<String> numbers, String message) {
        SmsManager sms = SmsManager.getDefault();
        for (String number : numbers) {
            try {
                // Split long messages automatically
                ArrayList<String> parts = sms.divideMessage(message);
                sms.sendMultipartTextMessage(number, null, parts, null, null);
                Log.d(TAG, "SMS sent to " + number);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS to " + number, e);
            }
        }
    }

    private void callNumber(String number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call " + number, e);
        }
    }

    private void startAudioRecording() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            audioFile = new File(getExternalFilesDir(null),
                    "sos_audio_" + timestamp + ".3gp");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.setMaxDuration((int) RECORD_DURATION_MS);
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Audio recording started: " + audioFile.getAbsolutePath());

            // Auto-stop after duration
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(this::stopAudioRecording, RECORD_DURATION_MS);

        } catch (Exception e) {
            Log.e(TAG, "Audio recording failed", e);
        }
    }

    private void stopAudioRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                Log.d(TAG, "Audio saved: " + (audioFile != null ? audioFile.getAbsolutePath() : "?"));
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
            } finally {
                mediaRecorder = null;
            }
        }
        stopSelf();
    }

    private Notification buildNotification(String title, String text) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "SOS Alert", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Emergency SOS alerts");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void updateNotification(String title, String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_ID, buildNotification(title, text));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAudioRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

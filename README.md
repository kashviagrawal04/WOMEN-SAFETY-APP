# HERZION — Marvin Safety App

> **"Whisper. Safe."** — Your silent guardian for personal safety and peace of mind.

---

## What is Marvin?

Marvin is an AI-powered women's safety Android app that listens for a secret wake word **"Marvin"** and instantly triggers an SOS alert to your emergency contacts — without you needing to touch your phone.

---

## Features

### 🎙 Voice-Triggered SOS
Say **"Marvin"** and the app automatically detects it using on-device speech recognition and triggers an emergency alert — completely hands-free.

### 📍 Smart Contact Scoring (AI)
Marvin uses a custom **AI scoring algorithm** that ranks your emergency contacts in real time based on:
- **Priority** you assign to each contact (1 = highest)
- **Geographic proximity** — who is physically closest to you right now

The top 4 contacts + helpline **1091** get the SOS SMS.

### 📲 Instant SOS Button
A large one-tap SOS button with a confirm dialog sends alerts immediately.

### 💬 SMS with Live Location
Emergency SMS is sent with a **Google Maps link** of your exact GPS coordinates.

### 🔴 Audio Evidence Recording
When SOS is triggered, Marvin silently records **60 seconds of ambient audio** and saves it securely on the device as evidence.

### 📞 Auto Call
Automatically calls your **primary guardian** or **helpline 1091** based on your settings.

### 👥 Emergency Circle
Manage your personal emergency contacts with name, phone, and priority. View them ranked by who would be alerted first.

### ⚙️ Persistent Settings
- Toggle auto-call to trusted contact
- Toggle helpline auto-call
- Set your primary guardian number
- All settings saved across sessions

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java (Android) |
| AI / Voice | Android SpeechRecognizer + TFLite |
| Contact Scoring | Custom Haversine + Priority Algorithm |
| Database | Room (SQLite) |
| Location | Google Play Services (FusedLocationProvider) |
| SMS | Android SmsManager |
| Audio | MediaRecorder |
| Background | Foreground Service |

---

## How It Works

```
User says "Marvin"
        ↓
MarvinDetector (SpeechRecognizer) detects keyword
        ↓
Confirm dialog shown
        ↓
User confirms (or auto-triggers)
        ↓
SosService starts (Foreground)
        ↓
┌─────────────────────────────────┐
│  Get GPS location               │
│  Score & rank contacts (AI)     │
│  Send SMS to top 4 + 1091       │
│  Call primary guardian          │
│  Record 60s audio evidence      │
└─────────────────────────────────┘
        ↓
EmergencyActivity shown with alerted contacts
```

## Permissions Required

| Permission | Purpose |
|-----------|---------|
| `RECORD_AUDIO` | Voice keyword detection + audio evidence |
| `ACCESS_FINE_LOCATION` | GPS coordinates for SOS message |
| `SEND_SMS` | Send SOS alerts to contacts |
| `CALL_PHONE` | Auto-call guardian/helpline |
| `READ_CONTACTS` | Access device contacts |
| `FOREGROUND_SERVICE` | Background SOS processing |

---

## Project Structure

```
HERZION/
├── app/src/main/
│   ├── java/com/example/safetyapp/
│   │   ├── MainActivity.java          # Home screen + SOS button
│   │   ├── SplashActivity.java        # Launch screen
│   │   ├── EmergencyActivity.java     # Active emergency screen
│   │   ├── ContactsActivity.java      # Emergency circle manager
│   │   ├── SettingsActivity.java      # App configuration
│   │   ├── MarvinDetector.java        # Voice keyword detection
│   │   ├── ContactAdapter.java        # RecyclerView adapter
│   │   ├── scorer/
│   │   │   └── ContactScorer.java     # AI contact ranking algorithm
│   │   ├── service/
│   │   │   └── SosService.java        # Background SOS + audio recording
│   │   ├── db/
│   │   │   ├── AppDatabase.java       # Room database
│   │   │   ├── ContactDao.java        # DB queries
│   │   │   └── ContactEntity.java     # Contact table schema
│   │   ├── model/
│   │   │   └── Contact.java           # Contact model
│   │   └── utils/
│   │       ├── LocationHelper.java    # GPS utilities
│   │       └── PermissionHelper.java  # Runtime permissions
│   └── assets/
│       └── marvin.tflite              # On-device AI model
```

## Getting Started

### 1. Clone the repo
```bash
git clone https://github.com/kashviagrawal04/HERZION.git
cd HERZION
```

### 2. Open in Android Studio
File → Open → Select the `HERZION` folder

### 3. Add your Google Services
Make sure you have Google Play Services available (for location).

### 4. Build & Run
Run → Run 'app' on your Android device (API 24+)

### 5. Grant Permissions
Allow all permissions when prompted — they are all required for the app to function.

---

## Setup Guide

1. Open the app → go to **Settings**
2. Add your emergency contacts with a priority (1 = most important)
3. Enter your primary guardian's number
4. Toggle whether to auto-call on SOS
5. Tap **Update Guard Protocols** to save
6. Return to home — Marvin is now **listening**

---

## Helpline

India Women's Helpline: **1091** (always included in every SOS alert)

---

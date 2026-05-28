# Ringl 📞

**Missed Call → WhatsApp Auto-Reply** — Lightweight Android app that detects missed calls and triggers a WhatsApp message via your backend.

## How It Works

1. Phone rings → call not answered → Android detects **MISSED** call
2. App sends webhook `POST /api/missed-call` to your backend
3. Backend sends WhatsApp message through Evolution API
4. Customer gets auto-reply on WhatsApp

## Setup

### 1. Configure Webhook
Open the app → enter your backend webhook URL and secret → Save.

### 2. Permissions
The app needs:
- **Phone State** — to detect incoming calls
- **Call Log** — to confirm the call was actually missed
- **Notifications** — for the persistent monitoring notification

### 3. Battery Optimization
**Important:** Disable battery optimization for this app:
- Settings → Apps → Ringl → Battery → **Unrestricted**
- On Xiaomi/Realme: Also enable **Auto-start**

### 4. Backend Endpoint
Your backend should accept:
```
POST /api/missed-call
Header: X-Webhook-Secret: <your-secret>
Body: { "phone": "919876543210", "timestamp": 1711900000000, "event": "missed_call", "source": "ringl_android" }
```

## Build

Open in Android Studio → Build → Generate APK.

**Min SDK:** Android 8.0 (API 26)  
**Target SDK:** Android 14 (API 34)

## Architecture

```
ringl/
├── app/src/main/java/com/ringl/missedcallhook/
│   ├── MainActivity.kt          # UI: config + stats
│   ├── receiver/
│   │   ├── MissedCallReceiver.kt # Hybrid detection (PhoneState + CallLog)
│   │   └── BootReceiver.kt       # Auto-start on reboot
│   ├── service/
│   │   └── MissedCallService.kt   # Foreground keep-alive
│   ├── webhook/
│   │   ├── WebhookSender.kt      # Dedup + queue via WorkManager
│   │   └── WebhookWorker.kt      # Reliable HTTP POST with retries
│   └── util/
│       ├── PrefsManager.kt       # SharedPreferences wrapper
│       └── PhoneUtils.kt         # Phone number normalization
```

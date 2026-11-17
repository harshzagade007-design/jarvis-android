# JARVIS Android + Cloud starter

A beginner-friendly first build: Android voice input + cloud AI command router + Android app actions + spoken replies.

## What this starter does
- Tap **SPEAK TO JARVIS** and speak; Android speech recognition transcribes it.
- Send the transcript to your FastAPI cloud backend, which asks an OpenAI model to classify it.
- Open YouTube search results, open supported apps, open the phone dialer, or open a prefilled SMS composer.
- Speak replies using the phone's installed text-to-speech voice, with a lower pitch setting.

## Important scope and safety
This is a working starter, not a finished always-listening Iron Man system. The included app uses a microphone button. It does **not** continuously listen for “Jarvis” / “Hey Jarvis” yet. Add Picovoice Porcupine wake-word detection and a microphone foreground service as a separate next stage; train/download two custom `.ppn` models for “Jarvis” and “Hey Jarvis”. The Picovoice Android quick start explains the SDK and custom keyword files: https://picovoice.ai/quick-start/wake-word-android/ . A persistent microphone service must be started while the app is visible, show its ongoing notification, and follow Android's microphone foreground-service rules. Android does not permit unrestricted hidden background microphone access.

Calls open the dialer, not place calls automatically. SMS opens a prefilled message composer, not send it. This avoids accidental calls/messages and respects Android's user-confirmation flows. Contact-name resolution, WhatsApp sending, app-specific automation and voice identity verification are not included in this first build.

The TTS pitch setting only lowers pitch; the actual voice/gender depends on the speech engine and voice packs installed on the phone. Choose an English male voice in Android Text-to-Speech settings if available.

## Part A — Deploy the cloud brain

1. Create an OpenAI API key at https://platform.openai.com/api-keys . API billing is separate from a ChatGPT subscription. Never put this key in the Android app or publish it in GitHub.
2. Create a GitHub repository and upload the **backend** folder contents (`main.py`, `requirements.txt`, `Procfile`).
3. On https://render.com create **New + → Web Service**, connect the repository, set the Root Directory to `backend` (if the repo includes the whole project), Runtime **Python**, Build Command `pip install -r requirements.txt`, Start Command `uvicorn main:app --host 0.0.0.0 --port $PORT`.
4. In Render → Environment add `OPENAI_API_KEY` with your secret key. Optionally set `OPENAI_MODEL` to a model available to your API project (default is `gpt-4.1-mini`).
5. Deploy. Open `https://YOUR-SERVICE.onrender.com/` and confirm it returns `{"status":"online","assistant":"JARVIS"}`.

## Part B — Build the Android app

1. Install Android Studio: https://developer.android.com/studio .
2. In Android Studio choose **Open** and select the `android` folder from this project.
3. Wait for Gradle sync. If Android Studio asks to install SDK 35 / build tools, accept.
4. Open `app/src/main/java/com/example/jarvis/MainActivity.kt` and replace `https://YOUR-RENDER-SERVICE.onrender.com` with your real Render HTTPS URL.
5. Connect your Android phone with USB debugging enabled, or use an emulator. Press **Run**.
6. Allow microphone permission. Press **SPEAK TO JARVIS**, say a command, and test.
7. To make an APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Android Studio shows the APK location.

## Example commands
- “Play relaxing piano on YouTube”
- “Open WhatsApp”
- “Open Google Maps”
- “Call 9876543210” (opens dialer with the number; tap call yourself)
- “Text 9876543210 saying I will reach in ten minutes” (only works if a phone number is explicitly spoken; opens a draft SMS)
- “Explain how solar panels work”

## Troubleshooting
- **Cloud error:** check Render logs, `OPENAI_API_KEY`, model access and API billing.
- **Speech does not work:** ensure Android speech services are installed and microphone permission is granted.
- **YouTube opens search instead of instantly playing:** this starter intentionally opens results; selecting a video is reliable across more Android versions and app configurations.
- **Male voice not heard:** Android Settings → Text-to-speech output → choose an available male English voice. Device vendors vary.

## Before public release
Add user authentication, rate limiting, request-size limits, HTTPS-only deployment, privacy disclosures, secure key storage, abuse monitoring and tests. Do not expose an unrestricted paid AI endpoint publicly. The OpenAI key belongs only on the backend.

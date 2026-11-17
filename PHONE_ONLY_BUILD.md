# Build JARVIS using only your Android phone

This project can be compiled in GitHub's cloud. You do not need Android Studio or a computer.

## What you need
- Your Android phone and Chrome
- A GitHub account
- Internet access

## 1. Upload the source to GitHub
1. Download and extract `jarvis_android_cloud_starter.zip` on your phone (Files app > Downloads > tap ZIP > Extract).
2. In Chrome, open https://github.com/new and create a **Public** repository named `jarvis-android` (public repositories get standard GitHub-hosted Actions minutes; do not put secrets in this repository).
3. Upload the contents of the `jarvis_project` folder, preserving the folder structure. The easiest reliable phone-only method is Termux from F-Droid:
   - Install Termux from https://f-droid.org/packages/com.termux/ . Avoid the outdated Play Store build.
   - Open Termux and run:
     ```sh
     termux-setup-storage
     pkg update -y
     pkg install git unzip -y
     cd ~/storage/downloads
     unzip jarvis_android_cloud_starter.zip
     cd jarvis_project
     git init
     git branch -M main
     git add .
     git commit -m "Add JARVIS starter"
     git remote add origin https://github.com/YOUR-GITHUB-USERNAME/jarvis-android.git
     git push -u origin main
     ```
   - Replace `YOUR-GITHUB-USERNAME` with your GitHub username. GitHub may require a Personal Access Token instead of your account password. Create a fine-grained token limited to this repository with Contents: Read and write. Paste it only into Termux when Git prompts for a password; never put it in source code or share it.
4. If you prefer not to use Termux, upload files through GitHub's website using **Add file > Upload files**, keeping the same paths. Do not upload the ZIP itself as a substitute for the extracted source.

## 2. Get the APK from GitHub Actions
1. Open your repository in Chrome.
2. Tap **Actions**. The workflow named **Build JARVIS APK** should start after the push.
3. Open the latest run. Wait until it shows a green check mark.
4. Scroll to **Artifacts** and tap `JARVIS-debug-APK` to download it.
5. Extract the downloaded artifact ZIP. It contains `app-debug.apk`.
6. Tap the APK in Files and allow Chrome/Files to install unknown apps if Android asks. Only install APKs built from your own repository.

## 3. Cloud backend
The backend is a separate deployment. Follow README.md Part A to create an OpenAI API key and deploy the `backend` folder to Render. Keep the API key in Render's Environment settings only. Do not commit `.env` or API keys to GitHub.

## Current limitations
The included starter is push-to-talk, not always listening. It can speak replies, search YouTube, launch supported apps, open the dialer, and prepare an SMS draft. It does not yet include the always-on Jarvis/Hey Jarvis wake word, WhatsApp sending, voice identity verification, or unrestricted phone control. Add those as later features and retain user confirmation for calls and messages.

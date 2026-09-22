# Cara Build APK - 3 Opsi

## Kenapa tidak langsung build di Termux tadi gagal?
Termux kamu belum ada `JDK` + `Android SDK` + `Gradle`. Build butuh download ~2GB dan sering timeout di Termux. Itu kenapa `java` dan `gradle` command not found tadi. Bukan error project, tapi environment belum siap.

## OPSI 1: Paling Cepat (5 menit) - Build via GitHub Actions (REKOMENDASI)
Ini cara "langsung build" tanpa install apa-apa di HP:

1. Buat repo di GitHub (misal `copy-text-floating`)
2. Push project ini:
```bash
cd /data/data/com.termux/files/home/storage/app/copy-text
git init
git add .
git commit -m "initial"
git branch -M main
git remote add origin https://github.com/USERNAME/copy-text-floating.git
git push -u origin main
```
3. Buka GitHub -> tab **Actions** -> workflow `Build APK` akan jalan otomatis
4. Tunggu 3-5 menit -> klik job -> download artifact `CopyText-Floating-APK` -> dapat `app-debug.apk` siap install

Workflow sudah aku siapkan di `.github/workflows/build.yml:1`

## OPSI 2: Build di Termux (butuh 30 menit + 2GB kuota)
Jalankan bertahap (jangan sekaligus, biar tidak timeout):
```bash
pkg update -y
pkg install -y openjdk-17
pkg install -y gradle aapt aapt2 d8 apksigner

# Download Android SDK command-line tools
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
curl -LO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip
mv cmdline-tools latest
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Install platform & build-tools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Build
cd /data/data/com.termux/files/home/storage/app/copy-text
gradle wrapper --gradle-version 8.4
./gradlew assembleDebug
# APK di app/build/outputs/apk/debug/app-debug.apk
```

## OPSI 3: Build di Laptop (Android Studio)
1. Buka Android Studio -> Open -> pilih folder `copy-text`
2. Tunggu sync -> Build -> Build APK(s)
3. APK ada di `app/build/outputs/apk/debug/`

---
Mau aku bantu push ke GitHub sekarang? Kasih URL repo GitHub kamu, aku setup push-nya.

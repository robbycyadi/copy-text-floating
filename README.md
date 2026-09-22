# Copy Text Floating — Aplikasi Mengambang Copy Text dari Gambar

Aplikasi Android floating bubble yang bisa copy text dari **gambar / aplikasi apapun** yang tampil di layar. Pakai OCR ML Kit Google (offline, support Indonesia).

## Fitur
- **Bubble mengambang (T COPY)** — bisa drag ke mana saja, tampil di atas semua aplikasi
- **Scan Layar penuh** — 1 tap capture seluruh layar → OCR → auto copy ke clipboard
- **Pilih Area (✂️)** — drag rectangle untuk seleksi area spesifik (cocok untuk gambar, tabel, chat)
- **Hasil mengambang** — preview teks selectable + tombol Salin & Tutup
- Auto copy + Toast "✓ Teks tercopy!"

## Cara Kerja
```
Bubble -> MediaProjection (screenshot) -> Bitmap -> ML Kit TextRecognition -> Clipboard
```

## Struktur Project
```
app/src/main/java/com/copytext/floating/
 ├─ MainActivity.kt      # minta izin overlay + MediaProjection
 ├─ FloatingService.kt   # bubble, screenshot, OCR
 └─ CropActivity.kt      # overlay drag untuk pilih area

app/src/main/res/layout/
 ├─ activity_main.xml    # UI onboarding
 ├─ layout_bubble.xml    # bubble + menu
 └─ layout_result.xml    # popup hasil OCR
```

## Cara Build APK

### Opsi 1: Android Studio (paling mudah)
1. Buka Android Studio -> Open -> pilih folder `copy-text`
2. Tunggu Gradle Sync selesai
3. Run (▶️) atau Build -> Build APK(s) -> `app/build/outputs/apk/debug/app-debug.apk`

### Opsi 2: Command Line
```bash
# di folder copy-text
gradle wrapper --gradle-version 8.4  # jika belum ada gradlew
./gradlew assembleDebug
# APK ada di app/build/outputs/apk/debug/app-debug.apk
```

> Butuh: Android SDK 34, JDK 17

## Cara Pakai (di HP)
1. Install APK
2. Buka app **Copy Text Floating**
3. Tap `1. Beri Izin Overlay` -> Aktifkan "Tampil di atas aplikasi lain"
4. Tap `2. Beri Izin Screenshot` -> Allow
5. Tap `Aktifkan Bubble` -> minimalkan app
6. Buka gambar / Instagram / PDF / apapun
7. Tap bubble **T COPY**
   - `📸 Scan Layar` = scan seluruh layar
   - `✂️ Pilih Area` = drag kotak di area teks yang mau di-copy
8. Teks muncul di popup tengah layar, **sudah otomatis tercopy** — tinggal Paste!

## Permission yang dipakai
- `SYSTEM_ALERT_WINDOW` — untuk bubble mengambang
- `FOREGROUND_SERVICE + MEDIA_PROJECTION` — screenshot + OCR

## Catatan
- ML Kit `text-recognition:16.0.0` support Latin (Indonesia, Inggris, dll). Untuk Chinese/Jepang/Korea ganti dependency di `app/build.gradle.kts`:
  ```kotlin
  implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
  implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
  ```
- Bubble bisa di-drag. Tap sekali untuk buka/tutup menu. Tombol Tutup di menu untuk matikan service.
- Tested minSdk 26 (Android 8.0) s/d Android 14.

## Troubleshooting
- **Gagal capture**: buka app lagi dan beri izin screenshot ulang (izin kadang reset setelah reboot)
- **Tidak ada teks terdeteksi**: coba `Pilih Area` dan seleksi lebih rapat, pastikan gambar tidak blur
- **Bubble hilang**: cek notifikasi "Copy Text Floating aktif" masih ada, kalau tidak aktifkan lagi di app

Butuh versi `.apk` jadi tanpa build? Bilang aja, aku bisa bantu build-kan atau buatkan versi Termux/Flutter.

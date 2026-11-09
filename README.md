# 🛡️ FamilyGuard

[![Build & Test Android App](https://github.com/floydous/family-guard/actions/workflows/build.yml/badge.svg)](https://github.com/floydous/family-guard/actions/workflows/build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

**FamilyGuard** adalah sistem perlindungan ganda pada perangkat Android yang dirancang khusus untuk melindungi ponsel orang tua yang sering dipinjamkan kepada anak dari ancaman **adware terselubung (*masquerading adware*)**, iklan layar penuh yang melumpuhkan, serta tautan **phising dan scam** di peramban web.

---

## 📌 Masalah yang Diselesaikan

1. **Adware Penyamar (*Masquerading Adware*):** Anak-anak yang sedang bermain game sering kali mengeklik iklan jebakan yang otomatis mengunduh berkas APK berbahaya. Aplikasi ini menyamar dengan nama dan ikon aplikasi resmi (misal: "Google Photos", "System Update", "Pembersih RAM"), meminta izin overlay layar (`SYSTEM_ALERT_WINDOW`), lalu menyembunyikan ikon peluncurnya dari *app drawer* agar tidak bisa dicopot oleh orang tua yang awam.
2. **Jebakan Phising & Scam di Peramban Web:** Tautan penipuan berhadiah palsu atau pencurian kredensial di peramban yang menjebak anggota keluarga. Filter DNS standar umumnya hanya menampilkan eror koneksi mati (`ERR_NAME_NOT_RESOLVED`), membuat pengguna mengira internet rusak alih-alih menyadari bahaya keamanan.

---

## 🏛️ Arsitektur Tiga Pilar

```text
+-----------------------------------------------------------------------------------+
|                                  FAMILYGUARD                                      |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ PILAR 1: APP DEFENSE ]         [ PILAR 2: LOCAL DNS ]     [ PILAR 3: ADVISORY ]|
|  - Storage Scanner & Quarantine   - Android VpnService       - Heads-Up Alerts    |
|  - Disguise Heuristics Engine     - Blocklists (HaGeZi/OISD) - Native Compose UI  |
|  - Post-Install Watchdog          - Compact Reverse Trie     - Zero SSL Mismatch  |
|  - One-Tap Uninstaller Dialog     - Upstream Encrypted DoH   - Parent Override PIN|
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### 1. Pertahanan Aplikasi Berlapis (Smart App Watchdog & Quarantine)
- **Pre-Install Storage Quarantine:** Memantau folder `/Download/` via `FileObserver`. Setiap berkas `.apk` baru dibedah secara instan. Jika terindikasi adware kloningan, ekstensinya seketika diubah menjadi `.quarantine` agar tidak bisa dieksekusi saat diklik.
- **Deteksi Typosquatting (Levenshtein Distance):** Mendeteksi kemiripan nama penipu (misal: "Gooogle Photos", "WhatApp").
- **Post-Install Watchdog:** Memantau instalasi aplikasi baru (`ACTION_PACKAGE_ADDED`). Jika aplikasi tidak memiliki ikon peluncur (`CATEGORY_LAUNCHER`), meniru brand resmi, atau menyalahgunakan overlay, FamilyGuard langsung memunculkan dialog pencopotan 1-sentuhan (`Intent.ACTION_DELETE`).

### 2. Filter DNS Lokal Terkurasi (Local DNS Guard)
- **Pencegatan Port 53 Tanpa Root:** Menggunakan Android `VpnService` yang hanya menangkap paket DNS UDP Port 53. Seluruh trafik data internet berat (streaming, game, video) dilewatkan secara langsung tanpa beban latensi atau pemborosan baterai (< 1-2% per hari).
- **Reverse Domain Filter Trie:** Struktur data *Radix Trie* terbalik yang mencocokkan domain dari kanan ke kiri (TLD ke subdomain) dengan waktu pencarian $O(L) < 0.05$ ms dan 100% deterministik.
- **Upstream Terenkripsi (DoH RFC 8484):** Kueri sah diteruskan melalui DNS-over-HTTPS ke Cloudflare (`1.1.1.1`) atau Quad9 (`9.9.9.9`).

### 3. Layanan Peringatan Native (Native In-App Threat Advisory)
- **Zero-SSL-Mismatch Architecture:** Domain phising di-sinkhole instan ke `0.0.0.0`, menghindari fatal error sertifikat HTTPS peramban (`NET::ERR_CERT_COMMON_NAME_INVALID`).
- **Heads-Up Alert & Throttling:** Memicu notifikasi sistem berprioritas tinggi dengan algoritma *debouncing* (maksimal 1 notifikasi per domain per 60 detik) untuk mencegah banjir notifikasi.
- **Layar Edukasi Ramah Anak:** Membuka antarmuka Material 3 yang menyajikan konteks ancaman, literasi siber ramah anak, dan otorisasi PIN Orang Tua (*Parent Override*) yang diamankan dengan hashing SHA-256 + salt.

---

## 🚀 Cara Mengunduh & Memasang APK

Setiap *push* dan *pull request* ke repositori ini secara otomatis dibangun dan diuji oleh GitHub Actions runner.

1. Buka tab **[Actions](https://github.com/floydous/family-guard/actions)** di repositori ini.
2. Klik alur kerja terbaru dari cabang `main`.
3. Gulir ke bawah ke bagian **Artifacts** dan unduh **`familyguard-debug-apk`**.
4. Ekstrak berkas zip dan pasang `app-debug.apk` di ponsel Android Anda.
5. Saat pertama kali dibuka:
   - Aktifkan sakelar **Filter DNS Lokal** (izinkan permintaan sambungan VPN sistem Android).
   - Aktifkan sakelar **Pengawas Aplikasi** untuk memantau unduhan APK.
   - Atur PIN Orang Tua melalui ikon gembok di pojok kanan atas.

---

## 🧪 Skenario Pengujian

### Uji Coba Filter DNS & Layanan Peringatan
1. Pastikan sakelar **Filter DNS Lokal** aktif di dashboard FamilyGuard.
2. Buka peramban (Chrome / Edge / Firefox) dan coba akses domain uji ancaman berikut:
   - `http://scam-bank-login.com` (Simulasi Phising Perbankan)
   - `http://hadiah-gratis.id` (Simulasi Scam Penipuan)
   - `http://doubleclick.net` (Simulasi Pelacak/Iklan Berbahaya)
3. Hasil:
   - Akses jaringan langsung diputus (`0.0.0.0`).
   - Notifikasi heads-up **"🛡️ FamilyGuard: Ancaman Diblokir!"** muncul di atas layar.
   - Menyentuh notifikasi akan membuka layar **ThreatAdvisoryActivity** yang menyajikan konteks bahaya, tips aman, dan formulir PIN Orang Tua.

### Uji Coba Karantina Unduhan APK
1. Unduh atau salin berkas APK tiruan dengan label nama kloningan (misal berlabel `"Google Photos"` dengan *package name* selain `com.google.android.apps.photos`) ke folder `/storage/emulated/0/Download/`.
2. Pengawas `DownloadStorageObserver` akan mendeteksi berkas secara instan dan mengubah namanya menjadi `nama_berkas.apk.quarantine`.

---

## 🛠️ Tumpukan Teknologi (Tech Stack)

| Komponen | Pustaka / Teknologi |
| :--- | :--- |
| **Bahasa Utama** | Kotlin 2.0.0 (Modern Android Development) |
| **Antarmuka (UI)** | Jetpack Compose BOM, Material Design 3 |
| **Jaringan & Layanan** | Android `VpnService`, Coroutines, OkHttp 4.12 (DoH RFC 8484) |
| **Struktur Data Filter** | Custom Reverse Domain Trie ($O(L)$ Lookup) |
| **Keamanan Kredensial** | SHA-256 Hashing dengan Salted Digest |
| **Penyimpanan Lokal** | SharedPreferences / DataStore Preferences |
| **CI/CD** | GitHub Actions (`ubuntu-latest`, JDK 17 Temurin, Gradle 8.7 Wrapper) |
| **Pengujian** | JUnit 4 (27 Unit Tests) |

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah lisensi [MIT](LICENSE).

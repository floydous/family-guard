# Research Findings: Android Security & DNS Protection

Semua temuan di bawah ini telah diverifikasi melalui dokumentasi resmi Android AOSP, riset ancaman MITRE, dan pengujian arsitektur filter DNS modern.

---

## 1. Anatomi & Modus Operandi Aplikasi Adware Terselubung (*Masquerading Adware*)

### Karakteristik & Pola Serangan:
1. **Peniruan Identitas (*Masquerading* - MITRE T1655.001):**
   - Aplikasi menggunakan label (*application label*) dan ikon yang sama persis atau sangat mirip dengan aplikasi populer (misalnya: "Google Photos", "System Update", "File Manager", "Kalkulator").
   - Namun, nama paket (*package name*) dan sertifikat pengembang (*signing certificate*) sama sekali tidak cocok. Contoh: Google Photos asli memiliki *package name* `com.google.android.apps.photos` yang ditandatangani oleh sertifikat resmi Google LLC. Aplikasi palsu memiliki *package name* seperti `com.photo.gallery.cleaner.ads` atau `com.sys.update.boost`.
2. **Izin Berbahaya untuk Iklan Layar Penuh (*Overlay Ads*):**
   - Meminta izin `android.permission.SYSTEM_ALERT_WINDOW` ("Display over other apps"). Izin ini memungkinkan aplikasi menggambar jendela *overlay* di atas aplikasi apa pun yang sedang dibuka pengguna, memunculkan iklan video intermiten atau *interstitial* secara paksa.
3. **Penyembunyian Ikon (*Stealth Mode*):**
   - Setelah dijalankan pertama kali, aplikasi menonaktifkan komponen peluncur (`PackageManager.setComponentEnabledSetting` pada aktivitas utama dengan filter `CATEGORY_LAUNCHER`) atau sengaja tidak mendeklarasikan ikon di peluncur sistem. Akibatnya, korban (orang tua) tidak dapat menemukan aplikasi di *home screen* atau *app drawer* untuk dihapus secara normal.
4. **Sumber Instalasi (*Sideloading* / Iklan Game):**
   - Umumnya terpasang bukan dari Google Play Store resmi, melainkan dari unduhan APK otomatis di *browser* (dipicu oleh iklan agresif di *game* anak) atau toko aplikasi pihak ketiga yang tidak terverifikasi.

---

## 2. Metodologi Pencegahan & Pembatasan Instalasi Aplikasi di Android

Di Android tanpa akses root, tidak ada izin aplikasi biasa yang dapat memblokir proses instalasi sistem secara diam-diam. Terdapat 3 (tiga) pendekatan teknis yang memungkinkan:

### Pendekatan A: Device Policy Controller (Device Owner / Profile Owner) — *Tingkat Sistem (Paling Kuat)*
- **Mekanisme:**
  - Menggunakan API resmi Android Enterprise: `DevicePolicyManager.addUserRestriction(admin, UserManager.DISALLOW_INSTALL_APPS)` atau `UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES`.
- **Kelebihan:**
  - 100% andal di tingkat sistem operasi (OS-level enforcement). Pengguna, anak, atau *browser* tidak akan bisa menginstal APK sama sekali. Opsi "Install unknown apps" dikunci.
- **Keterbatasan & Hambatan Pengguna:**
  - Menetapkan aplikasi sebagai *Device Owner* pada ponsel yang sudah aktif memerlukan perintah ADB satu kali melalui komputer:
    `adb shell dpm set-device-owner com.familyguard/.AdminReceiver`
    atau harus diinisiasi saat ponsel di-format ulang (*factory reset* via QR code provisioning).
  - Terlalu rumit untuk orang tua awam jika tidak dipandu dengan *tool* desktop otomatis.

### Pendekatan B: Intersepsi via AccessibilityService — *Tingkat Antarmuka (UI-Level)*
- **Mekanisme:**
  - Layanan `AccessibilityService` memantau jendela aktif di layar.
  - Ketika jendela `com.android.packageinstaller` atau `com.google.android.packageinstaller` (dialog konfirmasi "Ingin menginstal aplikasi ini?") atau tombol "Install" di Play Store muncul, layanan langsung memicu aksi `GLOBAL_ACTION_BACK` atau menampilkan jendela PIN pengaman (layar proteksi orang tua).
- **Kelebihan:**
  - Tidak memerlukan kabel data atau ADB. Cukup mengaktifkan izin Aksesibilitas di pengaturan ponsel.
- **Keterbatasan & Risiko:**
  - **Kebijakan Google Play:** Google sangat ketat membatasi penggunaan `AccessibilityService`. Aplikasi non-aksesibilitas yang menggunakan API ini untuk memblokir aksi sistem berisiko ditolak di Google Play Store, kecuali didaftarkan khusus dengan deklarasi ketat sebagai aplikasi *Parental Control*.
  - **Android 13/14 "Restricted Settings":** Jika aplikasi dipasang secara *sideload* (bukan dari Play Store), pengguna harus membuka blokir keamanan manual di "App Info -> Allow restricted settings" agar Aksesibilitas bisa diaktifkan.

### Pendekatan C: Pengawas Pasca-Instalasi Real-Time (*Post-Install Reactive Watchdog*) — *Paling Praktis & Legal*
- **Mekanisme:**
  - Aplikasi mendaftarkan `BroadcastReceiver` untuk menangani `Intent.ACTION_PACKAGE_ADDED`.
  - Begitu ada aplikasi baru yang berhasil terpasang di perangkat:
    1. Sistem FamilyGuard langsung memindai aplikasi tersebut menggunakan algoritma *Heuristic Disguise Check* (lihat Bagian 3).
    2. Jika terdeteksi sebagai aplikasi berbahaya/terselubung, FamilyGuard langsung memunculkan **Full-Screen Warning Notification & Dialog:** *"⚠️ PERINGATAN: Aplikasi mencurigakan baru saja terpasang!"*.
    3. Menyediakan satu tombol langsung bagi orang tua: **[HAPUS SEKARANG]** yang memicu `Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + suspiciousPkg))` tanpa orang tua harus mencari ikon yang disembunyikan.
- **Kelebihan:**
  - 100% legal, mematuhi kebijakan Google Play Store, tidak memerlukan *Device Owner* atau ADB, dan sangat ringan baterai.

---

## 3. Heuristik Deteksi Aplikasi Terselubung (*Disguise Detection Engine*)

Untuk mendeteksi apakah suatu aplikasi adalah adware yang menyamar, FamilyGuard dapat mengevaluasi matriks skor risiko (*Risk Score Matrix*) berdasarkan API Android yang tersedia:

| Sinyal Heuristik | API Android yang Digunakan | Indikasi Ancaman |
| :--- | :--- | :--- |
| **Kloning Nama (Name Mismatch)** | `PackageManager.getApplicationLabel()` dibandingkan dengan basis data aplikasi resmi (Google Photos, WhatsApp, Chrome, dll.) | **TINGGI:** Label bernama "Google Photos" tetapi *package name* BUKAN `com.google.android.apps.photos`. |
| **Penyalahgunaan Overlay** | `PackageManager.getPackageInfo(..., GET_PERMISSIONS)` memeriksa `android.permission.SYSTEM_ALERT_WINDOW` | **TINGGI:** Aplikasi berlabel alat sederhana (kalkulator, galeri, *cleaner*) meminta izin menggambar di atas aplikasi lain. |
| **Ikon Disembunyikan (Hidden Launcher)** | `PackageManager.queryIntentActivities()` untuk `Intent.ACTION_MAIN` + `CATEGORY_LAUNCHER` | **SEDANG-TINGGI:** Aplikasi tidak memiliki aktivitas peluncur di beranda, sehingga korban tidak bisa menghapusnya dari app drawer. |
| **Sumber Tidak Dikenal (Sideloaded)** | `InstallSourceInfo.getInstallingPackageName()` (API 30+) | **SEDANG:** Dipasang melalui *browser* (Chrome, UC) atau file manager, bukan oleh `com.android.vending` (Google Play). |
| **Sertifikat Tidak Cocok** | `SigningInfo.getApkContentsSigners()` | **MUTLAK:** Sertifikat kriptografi tidak cocok dengan sertifikat resmi vendor asli. |

---

## 4. Analisis Teknis DNS Kustom & Dilema Halaman Pemblokiran (Custom Block Page)

### A. Mekanisme Resolusi DNS Lokal di Android
- Tanpa *root*, cara standar industri (seperti yang digunakan AdGuard, RethinkDNS, dan Blokada) untuk mencegat kueri DNS adalah menggunakan `VpnService`.
- `VpnService` membuat antarmuka virtual (`tun0`) lokal di ponsel yang hanya menangkap paket UDP/TCP pada port 53 (DNS traffic), menyaringnya secara lokal, dan meneruskan trafik sah ke resolver terenkripsi (DNS-over-HTTPS / DNS-over-TLS).

### B. Masalah Fundamental: Kegagalan HTTPS Custom Block Page
Pengguna menginginkan: *Saat website terlarang/phising diakses, muncul halaman web yang menarik dan informatif yang menyatakan bahwa website tersebut kemungkinan scam/phising.*

**Realitas Teknis & Batasan Kriptografi TLS:**
1. Di era web modern, **>95% situs web menggunakan HTTPS (Port 443) dengan enkripsi TLS dan HSTS (*HTTP Strict Transport Security*)**.
2. Jika DNS lokal membelokkan domain phising (misal: `scam-bank.com`) ke IP lokal (`127.0.0.1:8080`) yang menjalankan server web mini:
   - *Browser* (Chrome, Edge, Samsung Internet) akan memulai **TLS Handshake** ke `127.0.0.1:8080` untuk domain `scam-bank.com`.
   - Server web lokal **tidak memiliki sertifikat SSL yang sah** untuk `scam-bank.com` yang ditandatangani oleh Certificate Authority (CA) publik tepercaya.
   - Akibatnya, *browser* tidak akan pernah menampilkan halaman HTML informatif tersebut! Sebaliknya, *browser* akan memunculkan layar merah peringatan fatal:
     **`NET::ERR_CERT_COMMON_NAME_INVALID`** atau **`SSL_ERROR_BAD_CERT_DOMAIN`**.
   - Pada domain ber-HSTS, tombol "Lanjutkan ke situs (tidak aman)" bahkan dikunci sepenuhnya oleh peramban.
3. **Mengapa MITM (Memasang Root CA Kustom di HP) Tidak Disarankan?**
   - Pi-hole, AdGuard Home, dan NextDNS secara resmi telah menghentikan atau sangat tidak menyarankan halaman pemblokiran HTML berbasis IP untuk HTTPS.
   - Di Android 7.0 (Nougat) ke atas, aplikasi dan peramban **tidak lagi mempercayai sertifikat tingkat pengguna (*user-added CA*)** secara *default*.
   - Jika pengguna dipaksa memasang sertifikat CA di ponsel, Android akan memunculkan notifikasi sistem persisten yang menakutkan: *"Jaringan mungkin sedang diawasi oleh pihak ketiga"*.
   - Selain itu, mekanisme *Certificate Pinning* pada aplikasi perbankan atau sosial media akan langsung rusak (*crash/error*).

### C. Solusi Terbaik & Modern untuk Peringatan Informatif di Android
Daripada mencoba memanipulasi lalu lintas HTTPS yang terenkripsi, pendekatan terbaik yang diterapkan aplikasi modern (seperti AdGuard Android & RethinkDNS) adalah:

1. **DNS Sinkholing:**
   - Ketika domain terdaftar dalam *blocklist* (OISD, HaGeZi Phishing, EasyList), DNS lokal langsung mengembalikan `0.0.0.0` atau `NXDOMAIN`. Koneksi jaringan berbahaya langsung terputus secara aman dan instan tanpa kebocoran data.
2. **Notifikasi Sistem Real-Time yang Interaktif (*Heads-Up In-App Interception*):**
   - Pada saat yang persis bersamaan ketika kueri DNS diblokir, modul `VpnService` lokal memicu **Notifikasi Sistem Android Prioritas Tinggi**:
     > 🛡️ **FamilyGuard: Ancaman Phising Diblokir!**  
     > *Akses ke situs mencurigakan `scam-login-bank.com` berhasil dicegah.*  
     > `[Sentuh untuk melihat analisis keamanan]`
3. **Layar Edukasi & Informasi di Dalam Aplikasi (*In-App Threat Advisory Screen*):**
   - Ketika notifikasi disentuh, FamilyGuard langsung membuka layar visual aplikasi yang menarik dan ramah pengguna, berisi:
     - Nama domain yang diblokir & waktu kejadian.
     - Penjelasan risiko: Mengapa domain ini berbahaya (Phising kredensial / Scam penipuan / Adware berbahaya).
     - Sumber intelijen: HaGeZi Pro / OISD Threat Intelligence.
     - Edukasi keamanan praktis untuk anak dan orang tua tentang ciri-ciri situs berbahaya.
     - Opsi proteksi PIN Orang Tua (*Parent Override*) jika situs tersebut ternyata adalah *false positive*.

---

## 5. Pertimbangan Performa & Kapasitas Memori (Mobile Constraints)

1. **Ukuran Blocklist (OISD & HaGeZi):**
   - *Feed* HaGeZi Pro++ atau OISD Full dapat berisi lebih dari 200.000 hingga 1.000.000 domain (berukuran 15 MB – 50 MB teks mentah).
   - Memuat ratusan ribu *String* secara naif di *heap* memori Android (Java/Kotlin) akan memicu lonjakan RAM (~200MB+) dan memicu sistem Android membunuh (*kill*) aplikasi di latar belakang.
2. **Solusi Arsitektur:**
   - Menggunakan struktur data efisien: **Bloom Filter terkompresi** atau **Compact Radix Tree / Trie** yang dikompilasi ke dalam format biner terindeks, atau memanfaatkan pustaka mesin inti (*native core*) menggunakan **Rust / Go / C++** yang di-*binding* ke Android lewat NDK (seperti yang dilakukan RethinkDNS dan AdGuard).
   - Dengan struktur data biner efisien, 300.000 domain hanya membutuhkan memori RAM sekitar **4 MB – 8 MB**, sangat aman untuk baterai dan ponsel berspesifikasi rendah.

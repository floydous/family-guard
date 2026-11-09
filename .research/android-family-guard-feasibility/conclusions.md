# Final Conclusions & Architectural Roadmap

## Ringkasan Temuan Riset

1. **Pencegahan Aplikasi Berbahaya & Adware Penyamar:**
   - **Fakta:** Di Android non-root reguler, aplikasi pihak ketiga tidak bisa memblokir secara paksa dialog instalasi tanpa menggunakan hak istimewa *Device Owner* (via ADB/Enterprise) atau layanan *AccessibilityService*.
   - **Solusi Terbaik & Legal:** Terapkan **Pendekatan Hibrida**:
     - *Dasar (Out-of-the-Box):* **Reactive Package Watchdog (`ACTION_PACKAGE_ADDED`)** yang menggunakan mesin heuristik (*Risk Scoring*) untuk mendeteksi ketidakcocokan nama resmi, izin *overlay* (`SYSTEM_ALERT_WINDOW`), ketiadaan ikon di peluncur (*hidden launcher*), dan instalasi dari sumber luar (*sideloading*). Jika terdeteksi, langsung luncurkan dialog peringatan darurat dengan tombol 1-klik untuk mencopot pemasangan.
     - *Tingkat Lanjut (Opsional):* Mode proteksi anak menggunakan *AccessibilityService* atau *Device Owner* untuk membatasi instalasi baru tanpa PIN orang tua.

2. **Dilema Halaman Pemblokiran Web (Custom Block Page) via DNS:**
   - **Fakta Teknis:** Menampilkan halaman HTML web kustom untuk domain yang diblokir pada tingkat DNS **tidak mungkin dilakukan secara mulus pada era HTTPS modern**. Mengarahkan lalu lintas HTTPS ke server web lokal akan memicu kegagalan TLS Handshake dan memunculkan layar merah peramban: `NET::ERR_CERT_COMMON_NAME_INVALID` (karena server lokal tidak memiliki sertifikat SSL sah untuk domain yang diblokir).
   - **Solusi Modern & Elegan:** Gunakan **DNS Sinkhole (`0.0.0.0`) + Native Android Heads-Up Notification & In-App Advisory Screen**.
     - Ketika anak mengakses situs terlarang, DNS memutus koneksi secara instan dan aman.
     - Sistem Android secara bersamaan memunculkan *Heads-Up Notification* interaktif: *"🛡️ FamilyGuard: Situs Phising Berbahaya Diblokir!"*.
     - Saat disentuh, aplikasi membuka antarmuka aplikasi native (Material 3) yang ramah anak dan informatif, menjelaskan mengapa situs tersebut diblokir, bahaya phising, serta opsi persetujuan orang tua.

3. **Kinerja & Efisiensi Data Blocklist:**
   - Blocklist masif seperti HaGeZi Pro dan OISD tidak boleh dimuat sebagai teks mentah di memori Java.
   - Gunakan format biner terkompresi (**Bloom Filter / Radix Tree**) yang hemat memori (< 10 MB RAM) dan memiliki waktu pencarian instan (< 1 ms), sehingga aman untuk baterai ponsel.

---

## Roadmap Teknis Sebelum Memulai Koding

1. **Fase 1: Inti Detektor Heuristik (Malware/Adware Watcher)**
   - Buat modul `disguise-engine` di Android.
   - Susun basis data nama dan package resmi aplikasi populer yang sering dikloning.
   - Uji deteksi izin `SYSTEM_ALERT_WINDOW` dan status ikon peluncur.

2. **Fase 2: Inti DNS Filter Lokal (`VpnService`)**
   - Buat layanan VPN lokal yang hanya mengikat port DNS 53.
   - Integrasikan *parser* kueri DNS dan muat filter domain (HaGeZi Light/Normal + OISD).
   - Pastikan kueri yang lolos diteruskan ke upstream terenkripsi (DoH Cloudflare/Quad9).

3. **Fase 3: Antarmuka & Notifikasi Peringatan (UX & UI)**
   - Desain layar `ThreatAdvisoryActivity` menggunakan Jetpack Compose untuk memberikan edukasi visual yang menarik saat situs berbahaya diblokir.
   - Sediakan fitur PIN Orang Tua untuk membuka blokir situs atau mengatur sensitivitas perlindungan.

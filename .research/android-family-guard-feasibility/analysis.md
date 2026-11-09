# Technical Analysis & Architecture Trade-Offs

Dokumen ini menganalisis perbandingan opsi implementasi teknis untuk dua pilar utama aplikasi FamilyGuard: (1) Perlindungan terhadap aplikasi adware/kloning berbahaya, dan (2) Proteksi web berbasis DNS kustom.

---

## 1. Analisis Opsi: Kontrol & Pencegahan Instalasi Aplikasi

| Dimensi Evaluasi | Opsi 1: Device Owner (MDM/DPC) | Opsi 2: AccessibilityService UI Interception | Opsi 3: Reactive Package Watchdog + Heuristics |
| :--- | :--- | :--- | :--- |
| **Tingkat Perlindungan** | **100% Preventif Mutlak** (OS menolak instalasi secara permanen) | **Preventif Reaktif** (Menekan Cancel / Menutup layar install otomatis) | **Deteksi Cepat Pasca-Install** (Memberi peringatan instan & tombol uninstal 1-klik) |
| **Kemudahan Pengguna (UX)** | **Rendah** (Harus via kabel data / ADB di PC atau reset pabrik) | **Sedang** (Perlu mengaktifkan izin di menu Pengaturan Aksesibilitas) | **Sangat Tinggi** (Cukup instal aplikasi dan beri izin normal) |
| **Kepatuhan Google Play Store** | Terbatas untuk penggunaan korporat / MDM khusus | Ketat / Rawan penolakan kecuali memenuhi syarat ketat *Parental Control* | **Penuh (100% Compliant)** |
| **Stabilitas & Ketahanan** | Tidak bisa dimatikan oleh anak tanpa izin admin | Bisa dimatikan atau dihentikan oleh pembunuh latar belakang OS vendor (MIUI/ColorOS) | Sangat stabil, dipicu oleh sistem OS saat paket baru terpasang |
| **Kesesuaian Target Pengguna** | Cocok untuk perangkat yang didedikasikan penuh untuk anak | Cocok jika orang tua bersedia melakukan konfigurasi satu kali | Cocok untuk ponsel orang tua yang dipinjamkan sementara ke anak |

### Rekomendasi Arsitektur untuk Pencegahan Aplikasi:
- **Strategi Bertingkat (Hybrid Multi-Tier):**
  - **Tier 1 (Default - Wajib):** *Reactive Package Watchdog*. Begitu aplikasi terpasang (via `ACTION_PACKAGE_ADDED`), FamilyGuard langsung menguji heuristik penyamaran. Jika terindikasi adware kloningan, kirim alarm bersuara/notifikasi prioritas tinggi dengan tombol **[Copot Pemasangan Sekarang]**.
  - **Tier 2 (Opsional / Advanced):** *Accessibility Guardian*. Jika orang tua mengaktifkan mode proteksi ketat (*Strict Mode*), aktifkan layanan Aksesibilitas untuk mengunci Google Play dan Package Installer dengan PIN orang tua.

---

## 2. Analisis Opsi: Tampilan Peringatan Situs Berbahaya (Custom Block Page vs In-App UI)

### Masalah Inti:
Pengguna ingin melihat peringatan informatif dan visual yang menarik saat membuka web phising/scam.

### Mengapa Pendekatan HTTP Proxy/Webserver Konvensional Gagal pada HTTPS:
```
[Browser Anak] ---> Request: https://scam-bank.com
                       |
                       v
[Local DNS Resolver] -> Mengembalikan IP: 127.0.0.1 (Server Lokal)
                       |
                       v
[Browser Anak] ---> Mengirim TLS ClientHello (SNI: scam-bank.com) ke 127.0.0.1:443
                       |
[Server Lokal] ---> Mengembalikan Sertifikat SSL Palsu / Self-Signed
                       |
                       X GAGAL! (Handshake Terputus)
[Browser Menampilkan] -> Layar Merah: NET::ERR_CERT_COMMON_NAME_INVALID
                         (Halaman HTML informatif TIDAK BISA TAMPIL!)
```

### Solusi Modern yang Direkomendasikan (Native Heads-Up + In-App Advisory):
```
[Browser Anak] ---> Request: https://scam-bank.com
                       |
                       v
[FamilyGuard VPN/DNS] -> 1. Mengembalikan 0.0.0.0 (Koneksi tertutup aman & instan)
                        2. Memanggil Android NotificationManager (Heads-Up High Priority)
                       |
[Layar Ponsel] ------> Muncul Pop-up Notifikasi Visual:
                        "🛡️ Ancaman Phising Scam Diblokir!"
                        "Menjaga data Anda dari scam-bank.com"
                       |
                       v (Pengguna menyentuh notifikasi)
[FamilyGuard App] ---> Membuka Halaman Edukasi Interaktif:
                        - Analisis Ancaman (HaGeZi Scam / OISD)
                        - Ciri-ciri phising yang dicegah
                        - Tips keamanan siber untuk anak & orang tua
                        - Tombol Override PIN (khusus orang tua jika false alarm)
```

**Keunggulan Pendekatan Ini:**
1. Tidak merusak enkripsi HTTPS atau memicu sertifikat error di browser.
2. Tidak memerlukan instalasi sertifikat Root CA yang berbahaya bagi keamanan ponsel.
3. Halaman edukasi dapat didesain secara bebas menggunakan antarmuka native Android (Jetpack Compose / Material 3) yang jauh lebih responsif, modern, dan interaktif dibanding halaman web biasa.

---

## 3. Analisis Kebutuhan Komputasi & Efisiensi Baterai

1. **Pemrosesan Blocklist di Perangkat Bergerak (Mobile):**
   - HaGeZi Multi PRO / OISD memiliki ukuran daftar domain yang masif.
   - Menyimpan seluruh teks dalam bentuk *Set<String>* di memori Java akan menghabiskan puluhan megabyte heap memori dan memperlambat pencarian string (*O(N)* atau overhead hashing).
   - **Solusi Rekayasa:**
     - Simpan data domain dalam struktur **Radix Tree / Trie terkompresi** atau **Bloom Filter**.
     - Waktu pencarian (*lookup time*) per kueri DNS berada di bawah **0.5 milidetik**.
     - Pembaruan berkala di latar belakang menggunakan `WorkManager` (hanya berjalan saat ponsel tersambung Wi-Fi dan sedang diisi daya).
2. **Konsumsi Daya Layanan VPN:**
   - Karena `VpnService` lokal hanya menyaring paket UDP DNS (Port 53) dan tidak memproses seluruh muatan video/gambar berat (semua trafik TCP non-DNS dilewatkan secara langsung atau di-*bypass*), dampak terhadap baterai sangat minimal (< 1-2% per hari).

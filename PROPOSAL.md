# PROPOSAL PROYEK: FAMILYGUARD
**Sistem Perlindungan Ponsel Pintar Keluarga dari Adware Terselubung dan Ancaman Siber Berbasis DNS Lokal**

---

## 1. Ringkasan Eksekutif

Dalam kehidupan sehari-hari, ponsel pintar milik orang tua sering kali dipinjamkan kepada anak untuk bermain *game* atau menonton video. Tanpa pemahaman literasi digital yang cukup, anak-anak sangat rentan memicu unduhan aplikasi berbahaya yang menyamar sebagai aplikasi resmi (misalnya kloningan "Google Photos", "System Update", atau "Kalkulator"). Aplikasi jenis ini umumnya merupakan adware agresif yang menyembunyikan ikon peluncurnya dan secara terus-menerus memunculkan iklan *overlay* layar penuh yang melumpuhkan kenyamanan penggunaan ponsel. Di sisi lain, paparan terhadap tautan *phising* dan *scam* di peramban web juga menjadi ancaman serius.

**FamilyGuard** dirancang sebagai solusi pertahanan ganda pada platform Android:
1. **Pengawas Aplikasi Terselubung (Smart App Watchdog):** Mendeteksi dan menangani pemasangan aplikasi adware berbahaya yang menyamarkan identitas serta menyembunyikan ikonnya.
2. **Perlindungan DNS Lokal Terkurasi & Edukasi Ancaman Native (Local DNS Guard & In-App Advisory):** Menyaring akses ke situs *phising*, *scam*, dan *malware* menggunakan daftar reputasi global (HaGeZi, OISD, EasyList) secara lokal di perangkat via DNS Sinkholing (`0.0.0.0`), dipadukan dengan notifikasi prioritas tinggi (*Heads-Up Notification*) dan layar edukasi *native* (Material 3) tanpa merusak enkripsi HTTPS atau memicu eror sertifikat SSL.

Dokumen proposal ini memetakan secara komprehensif latar belakang masalah, solusi rekayasa perangkat lunak yang ditawarkan, serta hambatan teknis yang dihadapi beserta mitigasinya.

---

## 2. Masalah yang Ingin Diselesaikan

### A. Penyusupan Adware Terselubung (*Masquerading Adware*)
- **Eksploitasi Pengguna Awam & Anak:** Anak-anak yang sedang bermain *game* sering kali mengeklik iklan interaktif atau tautan berhadiah palsu yang otomatis mengunduh berkas APK berbahaya.
- **Penipuan Identitas (*Brand Impersonation*):** Pembuat malware memberi nama dan ikon aplikasi yang identik dengan aplikasi bawaan tepercaya (misal: label bernama "Google Photos", "Pembersih RAM", "Update Sistem"), padahal nama paket (*package name*) dan penandatangan digital (*digital signature*) berasal dari pihak ketiga yang tidak jelas.
- **Iklan Overlay yang Melumpuhkan (`SYSTEM_ALERT_WINDOW`):** Aplikasi memperoleh izin untuk menggambar di atas aplikasi lain, lalu memunculkan iklan *pop-up* secara berkala di latar belakang, bahkan saat korban sedang menelepon, mengetik, atau menggunakan aplikasi lain.
- **Teknik Menghilangkan Jejak (*Hidden Launcher Icon*):** Begitu terpasang, aplikasi menonaktifkan aktivitas peluncurnya (*launcher activity*). Ikon aplikasi sengaja tidak dimunculkan di layar utama (*home screen*) maupun daftar aplikasi (*app drawer*). Hal ini membuat orang tua yang awam kebingungan mencari sumber masalah dan tidak tahu bagaimana cara mencopot pemasangan (*uninstall*) aplikasi tersebut.

### B. Kerentanan Penjelajahan Web (Phising, Scam, dan Jebakan Unduhan)
- **Tingginya Ancaman Sosial Engineering:** Situs web tipuan berkedok unduhan gratis, kupon, atau pembaruan palsu sangat mudah menjebak anggota keluarga.
- **Halaman Pemblokiran Konvensional yang Tidak Informatif:** Filter DNS standar (seperti *Private DNS* biasa) umumnya hanya memutus koneksi sehingga peramban menampilkan eror koneksi mati (`ERR_NAME_NOT_RESOLVED` / `NXDOMAIN`). Pengguna awam tidak memahami mengapa situs tersebut tidak bisa dibuka dan sering kali mengira jaringan internet mereka sedang rusak, alih-alih menyadari bahwa ada bahaya *phising* yang sedang dicegah.

---

## 3. Solusi yang Ditawarkan

Untuk menyelesaikan masalah-masalah di atas secara elegan dan stabil di lingkungan Android, FamilyGuard menawarkan tiga pilar solusi utama:

```
+-----------------------------------------------------------------------------------+
|                                  FAMILYGUARD                                      |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ PILAR 1: APP DEFENSE ]         [ PILAR 2: LOCAL DNS ]     [ PILAR 3: ADVISORY ]|
|  - Storage Scanner & Quarantine   - Android VpnService       - Heads-Up Alerts    |
|  - Disguise Heuristics Engine     - Blocklists (HaGeZi/OISD) - Native In-App UI   |
|  - Post-Install Watchdog          - Compact Bloom Filter     - Zero SSL Mismatch  |
|  - One-Tap Uninstaller Dialog     - Upstream Encrypted DoH   - Parent Override PIN|
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### Pilar 1: Pertahanan Aplikasi Berlapis (Storage Quarantine & Smart Watchdog)

1. **Pemindaian Berkas Unduhan & Karantina Otomatis (Pre-Install Storage Quarantine):**
   - Menggunakan izin `MANAGE_EXTERNAL_STORAGE` (kategori resmi Antivirus/Security App di Google Play) yang dipadukan dengan `FileObserver` untuk memantau direktori unduhan (`/storage/emulated/0/Download/`).
   - Begitu ada berkas `.apk` baru selesai ditulis di penyimpanan (baik diunduh dari Chrome, peramban bawaan, WhatsApp, maupun tautan iklan game):
     * Sistem langsung membedah berkas biner tersebut menggunakan `PackageManager.getPackageArchiveInfo()`.
     * Memeriksa skor risiko penyamaran (*Disguise Risk Score*): kecocokan label aplikasi vs *package name*, sertifikat kriptografi, izin `SYSTEM_ALERT_WINDOW`, dan ada/tidaknya ikon peluncur.
     * **Karantina Instan (Auto-Quarantine):** Jika berkas terindikasi berbahaya/adware penyamar, sistem seketika mengubah ekstensi berkas menjadi `.quarantine` (misal: `update-photos.apk` ➡️ `update-photos.apk.quarantine`). Berkas menjadi mati dan tidak dapat dieksekusi atau dipicu instalasinya oleh Android saat anak mengetuk notifikasi unduhan.
     * Memberikan notifikasi penenang kepada orang tua: *"🛡️ FamilyGuard: 1 berkas adware penyamar berhasil dinetralisir dari folder unduhan."*

2. **Pengawas Real-Time Pasca-Instalasi & Penghapusan 1-Sentuhan (Post-Install Watchdog & One-Tap Purge):**
   - **Pemicu Ganda (Dual Trigger):**
     * *Pemindaian Menyeluruh Awal (Onboarding Deep Scan):* Begitu FamilyGuard pertama kali diinstal, sistem memindai seluruh aplikasi yang sudah ada menggunakan `packageManager.getInstalledPackages(GET_PERMISSIONS)` untuk mencari adware yang sudah bersarang sebelumnya.
     * *Pemantauan Real-Time (`ACTION_PACKAGE_ADDED`):* Sistem langsung terbangun begitu ada aplikasi baru selesai dipasang dari toko mana pun.
   - **Tiga Indikator Penyamaran Mutlak (Verified Heuristics):**
     * *Indikator 1: Ikon Menghilang (Stealth Check):* Memeriksa ketiadaan filter `CATEGORY_LAUNCHER` via `queryIntentActivities`. Adware yang tidak memiliki ikon di beranda langsung dicurigai kuat.
     * *Indikator 2: Peniruan Merek (Masquerade Check):* Label aplikasi bernama "Google Photos", "System Update", atau aplikasi populer lain tetapi nama paketnya (`packageName`) tidak cocok dengan vendor resmi.
     * *Indikator 3: Permintaan Izin Overlay (`SYSTEM_ALERT_WINDOW`):* Memeriksa apakah manifes aplikasi meminta izin menggambar di atas aplikasi lain yang sering disalahgunakan untuk iklan mengganggu.
   - **Jalur Pintas Pencopotan Satu-Sentuhan (*One-Tap Purge*):**
     * Jika aplikasi berbahaya ditemukan, FamilyGuard langsung memunculkan **Layar Peringatan Darurat**: menampilkan identitas asli aplikasi dan alasan mengapa aplikasi tersebut berbahaya.
     * Orang tua tidak perlu frustrasi mencari ikon yang disembunyikan atau membuka menu Pengaturan yang rumit. Cukup menekan satu tombol merah besar: **[COPOT PEMASANGAN SEKARANG]**.
     * Sistem langsung memanggil dialog resmi Android via `Intent(Intent.ACTION_DELETE, Uri.parse("package:$maliciousPackage"))`. Orang tua cukup menekan **OK**, dan adware langsung terhapus bersih dari ponsel.

### Pilar 2: Filter DNS Lokal Terkurasi (On-Device Local DNS Guard)
1. **Pencegatan Lalu Lintas DNS Tanpa Root:**
   - Memanfaatkan Android `VpnService` lokal yang hanya mengikat port DNS (UDP/TCP Port 53). Lalu lintas internet non-DNS (streaming, video, chat) diteruskan langsung tanpa melalui proxy lambat sehingga performa internet tetap maksimal dan hemat baterai.
2. **Aturan Blocklist Bereputasi Tinggi:**
   - Mengintegrasikan daftar reputasi teruji dari komunitas global:
     - **HaGeZi Pro / Multi Normal:** Pemblokiran phising, penipuan, malware, dan pelacak berbahaya.
     - **OISD / EasyList:** Pemblokiran domain iklan agresif dan telemetri eksploitatif.
3. **Upstream Terenkripsi (DNS-over-HTTPS / DoH):**
   - Kueri domain yang sah diteruskan ke penyedia upstream aman dan terenkripsi (seperti Cloudflare `1.1.1.1` atau Quad9 `9.9.9.9`), mencegah pembajakan DNS (*DNS poisoning/hijacking*) oleh jaringan Wi-Fi publik.
4. **Mesin Pencarian Biner Terkompresi (*Compact Bloom Filter / Trie*):**
   - Menyimpan ratusan ribu domain yang terblokir ke dalam struktur biner efisien yang hanya menggunakan memori RAM **< 8 MB**, mencegah aplikasi dimatikan paksa oleh sistem operasi karena konsumsi memori berlebih.

### Pilar 3: Sistem Peringatan Interaktif Native & Edukasi Ancaman (Native In-App Threat Advisory)
1. **Penyajian Peringatan Real-Time Bebas Masalah SSL (Zero-SSL-Mismatch):**
   - DNS lokal melakukan *sinkholing* instan (`0.0.0.0`), memutus koneksi berbahaya secara senyap tanpa merusak enkripsi HTTPS atau memicu layar merah peringatan sertifikat peramban (`NET::ERR_CERT_COMMON_NAME_INVALID`).
   - Pada saat bersamaan ketika kueri phising/scam terdeteksi dari peramban, sistem memicu **Android Heads-Up Notification** berprioritas tinggi:
     > 🛡️ **FamilyGuard: Ancaman Phising Diblokir!**  
     > *Akses ke situs mencurigakan `scam-bank.com` berhasil dicegah.*  
     > `[Sentuh untuk melihat analisis keamanan]`
2. **Layar Edukasi Native yang Interaktif (In-App Advisory Screen):**
   - **Antarmuka Modern (Jetpack Compose / Material 3):** Membuka tampilan aplikasi native yang cepat, ringan, responsif, dan ramah anak saat notifikasi disentuh.
   - **Konteks Ancaman yang Jelas:** Menampilkan nama domain tujuan dan alasan spesifik pemblokiran (misalnya: *Kategori Phising Perbankan*, *Scam Kupon Palsu*, atau *Jebakan Malware* berdasarkan intelijen HaGeZi/OISD).
   - **Literasi Digital Keluarga:** Menyediakan tips ringkas dan ilustrasi ramah anak mengenai cara mengenali ciri-ciri tautan tipuan di internet.
   - **Otorisasi Orang Tua (Parent PIN Override):** Menyediakan dialog masukan PIN Orang Tua langsung di dalam aplikasi untuk memberikan izin pembukaan akses sementara (*whitelist*) jika situs terdeteksi secara keliru (*false positive*).
3. **Pemisahan Efek Cerdas (Peramban Web vs Iklan Game/Latar Belakang):**
   - **Trafik Peramban Web (Interactive Alert):** Memicu notifikasi *heads-up* interaktif agar anak dan orang tua mendapatkan edukasi siber seketika.
   - **Trafik Iklan Game / Latar Belakang (Silent Sinkhole):** Ketika permintaan berasal dari SDK iklan di dalam game anak, kueri di-sinkhole secara senyap (`0.0.0.0`) tanpa memunculkan notifikasi atau mengganggu jalannya permainan anak. Iklan berbahaya langsung batal tayang (*killed silently*).

---

## 4. Hambatan Teknis & Strategi Mitigasi

Dalam merealisasikan ide ini pada platform Android modern, terdapat sejumlah batasan arsitektur sistem operasi yang harus diantisipasi:

### Hambatan 1: Pencegahan Instalasi Aplikasi Sembarangan & Metodologi Terpilih
- **Tantangan Fundamental:**
  - Di Android tanpa root, aplikasi pihak ketiga tidak diizinkan membatalkan instalasi sistem secara sepihak di latar belakang.
  - Peretas dan penyebar adware sering menyamarkan unduhan dengan menumpang pada infrastruktur cloud tepercaya (seperti GitHub Assets, Discord CDN, atau AWS S3), sehingga penyaringan di tingkat nama domain jaringan (SNI/DNS) pasti lolos karena domain induk tidak boleh diblokir.

#### A. Metodologi yang Dipilih: Storage Scanner & Auto-Quarantine Engine
- **Mekanisme:** Memanfaatkan izin `MANAGE_EXTERNAL_STORAGE` (kategori resmi Antivirus) untuk memantau folder unduhan via `FileObserver`, membedah biner APK via `PackageManager.getPackageArchiveInfo()`, dan menetralkan berkas berbahaya menjadi ekstensi `.quarantine` sebelum sempat dibuka.
- **Alasan Pemilihan:**
  1. **Kemudahan Setup bagi Orang Tua Awam (*Zero Friction*):** Hanya memerlukan 1 langkah aktivasi toggle izin pengelolaan berkas yang wajar dipahami untuk aplikasi keamanan.
  2. **Bebas dari Peringatan Menakutkan OS:** Berbeda dari Aksesibilitas, izin berkas tidak menampilkan peringatan mengerikan tentang *"perekaman password dan ketikan layar"*.
  3. **Tangguh terhadap Manajemen Daya Vendor (OEM-Proof):** Tidak bergantung pada layanan aksesibilitas visual yang sering dibunuh paksa oleh penghemat baterai Xiaomi/Oppo/Vivo.
  4. **Pencegahan Senyap & Mulus bagi Anak (*Silent Shield*):** Berkas yang dikarantina otomatis rusak dan tidak bisa dibuka oleh peramban, memutus siklus instalasi tanpa konflik di layar.

#### B. Alternatif-Alternatif yang Ditolak Beserta Alasannya
1. **Ditolak: Intersepsi UI via AccessibilityService (Pendekatan Parental Control Klasik)**
   - *Mekanisme:* Memantau layar ponsel, mendeteksi jendela `PackageInstaller`, lalu memaksa tombol *Back* atau meminta PIN.
   - *Alasan Penolakan:*
     * **Peringatan sistem sangat menakutkan:** Dialog Android memperingatkan bahwa aplikasi dapat membaca seluruh isi layar, kata sandi, dan data sensitif, membuat orang tua awam takut.
     * **Sangat sulit diaktifkan di Android 13/14:** Masuk dalam kategori *Restricted Settings* yang terkunci abu-abu jika dipasang di luar Play Store, memerlukan langkah teknis yang membingungkan orang tua.
     * **Mudah mati:** Sangat rentan dihentikan oleh pembunuh latar belakang OS vendor (MIUI, ColorOS).
2. **Ditolak: Network-Only Filtering (Mendeteksi Content-Type APK di VPN)**
   - *Mekanisme:* Mencoba memutus unduhan di tingkat lalu lintas jaringan VPN.
   - *Alasan Penolakan:* Secara teknis tidak mungkin melihat path URL `/file.apk` atau header `Content-Type` pada koneksi HTTPS tanpa memasang Root CA. Jika malware di-host di GitHub Assets, pemblokiran domain induk akan merusak akses pengguna lain ke GitHub (*false-alarm masif*).
3. **Ditolak: Device Owner via ADB / Android Enterprise (DPC Mode)**
   - *Mekanisme:* Menjalankan perintah `dpm set-device-owner` via kabel data ADB atau reset pabrik.
   - *Alasan Penolakan:* Mengharuskan orang tua memiliki laptop, mengaktifkan USB Debugging, dan mengetikkan perintah konsol command prompt, yang sama sekali tidak realistis bagi target pengguna orang tua awam.

### Hambatan 2: Dilema Sertifikat Kriptografi TLS/HTTPS & Metodologi Terpilih
- **Tantangan Fundamental:**
  - Pengguna membutuhkan tampilan halaman peringatan yang menarik dan mendidik saat situs terlarang diakses.
  - Namun, **>95% web modern berjalan di atas protokol HTTPS (Port 443)** dengan enkripsi TLS dan perlindungan HSTS (*HTTP Strict Transport Security*). Peramban mewajibkan penyelesaian *TLS Handshake* dengan sertifikat SSL yang sah sebelum mau memproses data apa pun.

#### A. Metodologi yang Dipilih: DNS Sinkholing (0.0.0.0) + Native Heads-Up & In-App Advisory
- **Mekanisme:** Mengembalikan alamat `0.0.0.0` untuk domain berbahaya pada tingkat DNS, dipadukan dengan pemicuan notifikasi prioritas tinggi (*Android Heads-Up Notification*) yang mengarahkan pengguna ke layar edukasi native di dalam aplikasi FamilyGuard (`ThreatAdvisoryActivity`).
- **Alasan Pemilihan:**
  1. **Nol Konflik Sertifikat SSL (Zero SSL Friction):** Koneksi ke domain phising langsung diputus secara instan dan aman di tingkat soket tanpa membajak lalu lintas TLS. Peramban tidak akan pernah menampilkan layar eror merah `NET::ERR_CERT_COMMON_NAME_INVALID`.
  2. **Edukasi Tepat Sasaran Tanpa Merusak Enkripsi:** Saat notifikasi disentuh, aplikasi membuka antarmuka edukasi native (Jetpack Compose / Material 3) yang ramah anak, menjelaskan ancaman secara jelas, dan menyediakan verifikasi PIN Orang Tua (*Parent Override*).
  3. **Privasi & Keamanan Terjaga Penuh:** Tidak memerlukan pemasangan sertifikat Root CA berbahaya di perangkat, menjaga keutuhan mekanisme *Certificate Pinning* aplikasi perbankan dan media sosial.
  4. **Pemisahan Efek Cerdas (Browser vs Game):** Kueri iklan game di-sinkhole secara senyap tanpa notifikasi agar tidak mengganggu permainan, sedangkan kueri peramban memicu notifikasi edukasi.

#### B. Alternatif-Alternatif yang Ditolak Beserta Alasannya
1. **Ditolak: DNS Spoofing ke Web Server Eksternal / Cloudflare Worker Tanpa Root CA**
   - *Mekanisme:* Resolver DNS mengembalikan IP web server eksternal (misal: Cloudflare Worker) untuk menyajikan halaman HTML pemblokiran.
   - *Alasan Penolakan:* Protokol DNS hanya mengembalikan alamat IP dan tidak dapat mengeksekusi HTTP 302 Redirect. Saat peramban membuka `https://scam-bank.com` pada IP tersebut, peramban mengirimkan SNI `scam-bank.com`. Karena server eksternal tidak memiliki sertifikat SSL yang sah untuk domain phising tersebut, peramban tetap langsung memblokir dengan eror fatal `NET::ERR_CERT_COMMON_NAME_INVALID`.
2. **Ditolak: Local MITM Proxy dengan Pemasangan Self-Signed Root CA (Pendekatan AdGuard Desktop / Charles)**
   - *Mekanisme:* Membuat sertifikat Root CA lokal di HP, meminta orang tua memasangnya ke *User Credentials* sistem Android, lalu proxy lokal menandatangani sertifikat palsu untuk domain scam secara instan.
   - *Alasan Penolakan:*
     * **Sangat sulit bagi pengguna awam:** Mengharuskan navigasi rumit di menu Pengaturan Android yang berbeda-beda di setiap merek ponsel (Samsung, Xiaomi, Oppo), berpotensi menyebabkan tingkat pembatalan (*drop-off rate*) pengguna mencapai 90%.
     * **Peringatan sistem yang menakutkan:** Android akan memunculkan notifikasi persisten bahwa perangkat sedang diawasi pihak ketiga (*"Network may be monitored"*), menimbulkan kecurigaan bagi orang tua.
     * **Risiko stabilitas & keamanan:** Di Android 7.0+, aplikasi native tidak mempercayai sertifikat pengguna secara default, dan teknik ini merusak mekanisme *Certificate Pinning* pada aplikasi penting (misal: perbankan atau media sosial).
3. **Ditolak: NXDOMAIN / 0.0.0.0 Null-Routing Murni Tanpa Notifikasi Edukatif (Pendekatan Pi-hole Standar)**
   - *Mekanisme:* DNS lokal hanya mengembalikan kode eror atau alamat null tanpa tindak lanjut.
   - *Alasan Penolakan:* Peramban hanya menampilkan pesan eror koneksi mati (*"This site can’t be reached"*). Pengguna dan anak mengira koneksi internet mereka yang bermasalah, kehilangan kesempatan untuk memperoleh edukasi siber, dan tidak menyadari bahwa mereka baru saja diselamatkan dari ancaman penipuan.

### Hambatan 3: Efisiensi Memori (RAM) & Konsumsi Baterai Ponsel
- **Tantangan:**
  - Blocklist komprehensif dari HaGeZi dan OISD mencakup 200.000 hingga 1.000.000 domain (15–50 MB teks mentah). Memuatnya langsung ke heap Java akan memicu lonjakan memori (RAM > 200 MB), memperlambat ponsel, dan menyebabkan aplikasi dibunuh oleh sistem Android (*Low Memory Killer*).
- **Mitigasi FamilyGuard:**
  - Mengonversi daftar teks mentah menjadi struktur data terkompresi **Probabilistic Bloom Filter** atau **Radix Trie**.
  - Waktu pencarian (*lookup time*) instan (< 0.5 milidetik) dengan kebutuhan RAM di bawah **8 MB**.
  - Proses unduh dan kompilasi pembaruan aturan dilakukan saat perangkat berada dalam kondisi optimal (terhubung Wi-Fi dan sedang diisi daya) memanfaatkan `Android WorkManager`.

### Hambatan 4: Fragmentasi Manajemen Daya Vendor Android (OEM Background Killing)
- **Tantangan:**
  - Produsen seperti Xiaomi (MIUI/HyperOS), Oppo (ColorOS), dan Samsung (OneUI) memiliki algoritma agresif yang sering menghentikan layanan latar belakang (*background service*) pihak ketiga demi menghemat baterai.
- **Mitigasi FamilyGuard:**
  - Menjalankan `VpnService` sebagai *Foreground Service* dengan notifikasi persisten minimal yang sesuai standar Android.
  - Menyediakan panduan terintegrasi (*OEM Whitelist Wizard*) di dalam aplikasi untuk memandu orang tua menonaktifkan "Battery Optimization" khusus untuk FamilyGuard.

---

## 5. Rencana Arsitektur & Tumpukan Teknologi (Tech Stack)

| Komponen | Teknologi / Pustaka | Peran & Tanggung Jawab |
| :--- | :--- | :--- |
| **Bahasa Utama** | Kotlin (Modern Android Development) | Bahasa resmi dengan keamanan memori dan performa tinggi |
| **Antarmuka Pengguna (UI)** | Jetpack Compose & Material Design 3 | Membangun UI modern, responsif, dan ramah anak/orang tua |
| **Layanan Jaringan** | Android `VpnService` + Coroutines | Menangkap paket DNS lokal (Port 53) tanpa membocorkan privasi |
| **Format Blocklist** | Custom Binary Bloom Filter / Trie | Kompresi memori daftar domain HaGeZi & OISD |
| **Protokol DNS Upstream** | DNS-over-HTTPS (DoH) via OkHttp / Ktor | Pengiriman kueri aman ke resolver Cloudflare/Quad9 |
| **Penyedia Peringatan & Edukasi** | Android NotificationManager + Jetpack Compose | Heads-up notification prioritas tinggi & layar edukasi native bebas eror SSL |
| **Pengawas Paket & Purge** | `BroadcastReceiver` (`ACTION_PACKAGE_ADDED`) + `Intent.ACTION_DELETE` | Deteksi real-time & memicu dialog pencopotan resmi OS secara instan |
| **Izin Visibilitas Aplikasi** | `QUERY_ALL_PACKAGES` (Deklarasi Antivirus resmi Play Store) | Memindai dan membedah aplikasi penyamar yang terpasang di perangkat |
| **Penjadwal Latar Belakang** | Android Jetpack `WorkManager` | Pembaruan otomatis berkala untuk basis data blocklist |
| **Penyimpanan Lokal** | Jetpack DataStore / Room Database | Menyimpan konfigurasi, riwayat ancaman yang diblokir, dan PIN orang tua |

---

## 6. Tahapan Pengembangan (Roadmap)

1. **Fase 1: Mesin Deteksi Penyamaran & Pemantau Paket (App Watchdog PoC)**
   - Implementasi katalog aplikasi resmi & penghitung jarak Levenshtein nama aplikasi.
   - Pengecekan izin `SYSTEM_ALERT_WINDOW` dan status ketiadaan ikon peluncur.
   - Pengujian `PackageInstallReceiver` untuk memicu dialog uninstal langsung.
2. **Fase 2: Inti DNS Lokal & Mesin Filter (Local DNS PoC)**
   - Pembuatan modul `VpnService` minimal yang mengikat paket DNS Port 53.
   - Pustaka Bloom Filter untuk memuat *rules* HaGeZi / OISD secara hemat memori.
   - Resolusi kueri aman via DNS-over-HTTPS (DoH).
3. **Fase 3: Antarmuka Peringatan Native & Edukasi Ancaman (Advisory UI)**
   - Desain layar `ThreatAdvisoryActivity` menggunakan Jetpack Compose & Material 3 untuk edukasi visual yang ramah anak.
   - Implementasi pemicuan *Heads-Up Notification* saat kueri phising peramban dicegat, dan *silent drop* pada kueri iklan game.
   - Pengaturan dialog PIN Orang Tua di dalam aplikasi untuk fitur *Parental Override* (whitelisting instan).
4. **Fase 4: Optimasi Performa, Pengujian Perangkat, & Rilis**
   - Uji coba ketahanan baterai pada berbagai perangkat OEM (Samsung, Xiaomi, Pixel).
   - Pengujian skenario serangan nyata (APK adware tiruan, situs phising uji coba).
   - Finalisasi dokumentasi dan persiapan distribusi.

---

## 7. Catatan Revisi & Evaluasi Berkelanjutan
*Dokumen ini bersifat dinamis (living document) dan akan diperbarui secara berkala seiring berjalannya masukan pengguna, pengujian teknis, dan revisi skenario fitur.*

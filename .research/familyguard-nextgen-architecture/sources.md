# Research Sources: Next-Generation Android Family Security

Berikut adalah sumber-sumber primer dan sekunder terverifikasi yang digunakan dalam riset arsitektur FamilyGuard generasi baru:

## 1. Arsitektur Jaringan DNS & VpnService
- **RethinkDNS Architecture & Firestack Engine**:
  - URL: `https://github.com/celzero/rethink-app`
  - Analisis: Penanganan VpnService, dynamic network monitoring via `ConnectivityManager.NetworkCallback`, DNS leak prevention, dan in-memory LRU TTL DNS Cache.
- **DNS66 & personalDNSfilter**:
  - URL: `https://github.com/julian-klode/dns66` & `https://github.com/IngoZenz/personaldnsfilter`
  - Analisis: Penanganan Chrome DNS prefetch bypass dengan perutean IP resolver publik dinamis dan penemuan link properties fisik.
- **Pi-hole, AdGuard Home & NextDNS Block Page Limitations**:
  - URL: `https://docs.pi-hole.net/ftldns/blockingmode/` & `https://help.nextdns.io/t/h7hf6xa/use-a-block-page-does-it-slow-down-browsing`
  - Analisis: Batasan fundamental kriptografi HTTPS pada block page web; mengapa aplikasi Android native harus menggunakan in-app advisory popup daripada mencoba HTTP spoofing pada domain HTTPS.

## 2. Analisis Ancaman Siber Nyata Keluarga (SMS Stealer & Social Engineering APK)
- **Badan Siber dan Sandi Negara (BSSN) - Analisis Teknis Malware Undangan Pernikahan.apk**:
  - Sumber: Analisis Forensik Digital BSSN & Video Dokumentasi Reverse Engineering APKTool
  - Temuan: Ekstraksi SMS OTP menggunakan izin `RECEIVE_SMS`, `READ_SMS`, dan `SEND_SMS`, dieksfiltrasi ke Telegram Bot API (`api.telegram.org`).
- **Analisis Malware Android: Modus Undangan Pernikahan & Surat Tilang (ATIOS & Netbytesec Research)**:
  - URL: `https://atios.id/opini/analisis-malware-android-modus-undangan-pernikahan-yang-menyadap-sms-dan-otp/`
  - Temuan teknis: Indikator permission gate `android.permission.RECEIVE_SMS`, eksfiltrasi metadata dan OTP ke Telegram, pengiriman SMS otomatis untuk pengurasan saldo m-banking.
- **Vaksincom (Alfons Tanujaya) - Analisis Modus APK Kurir Paket & Surat Tilang Digital**:
  - Temuan: Pola rekayasa sosial yang menyamar sebagai dokumen umum (resi paket J&T, surat tilang ETLE, tagihan PLN, undangan nikah) dengan muatan SMS-stealer.

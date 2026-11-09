# Research: FamilyGuard Android Security & Child Protection

## Executive Overview
Investigasi kelayakan teknis, metodologi deteksi, dan arsitektur sistem untuk aplikasi perlindungan ponsel orang tua dari instalasi aplikasi berbahaya/adware terselubung oleh anak, serta proteksi penjelajahan web berbasis DNS filtering dengan edukasi pengguna.

---

## Latar Belakang Masalah
1. **Penyusupan Aplikasi Berbahaya & Adware (Pengalaman Nyata):**
   - Anak-anak meminjam ponsel orang tua dan secara tidak sengaja mengunduh aplikasi adware/malware.
   - Aplikasi berbahaya menyamarkan identitas (*masquerading*): menjiplak nama dan ikon aplikasi resmi (misalnya "Google Photos", "System Update", "Kalkulator") sehingga tampak seperti aplikasi bawaan.
   - Begitu terpasang, aplikasi memunculkan iklan *overlay* secara terus-menerus menggunakan izin `SYSTEM_ALERT_WINDOW`, sering kali menyembunyikan ikon peluncur (*launcher icon*) agar sulit ditemukan dan dihapus.
2. **Kebutuhan Proteksi Web (Anti-Phishing/Scam):**
   - Kebutuhan menjalankan DNS kustom lokal dengan *rules/blocklist* terpercaya (OISD, HaGeZi, EasyList).
   - Kebutuhan menampilkan peringatan informatif dan menarik yang mengedukasi pengguna saat domain terlarang/berbahaya diakses, bukan sekadar layar mati *error* koneksi (`NXDOMAIN`).
3. **Tantangan Metodologi & Batasan OS Android:**
   - Android adalah sistem operasi dengan *sandboxing* ketat. Diperlukan penelitian mendalam tentang bagaimana aplikasi pihak ketiga (tanpa root) dapat mendeteksi, mencegah, atau membatasi instalasi aplikasi secara legal dan andal.
   - Evaluasi batasan teknis HTTPS/TLS ketika mencoba menampilkan *custom block page* pada level DNS.

---

## Struktur Direktori Riset
- [`sources.md`](./sources.md): Daftar pustaka primer, dokumentasi resmi Android, riset keamanan MITRE & OWASP, serta referensi teknis Pi-hole/AdGuard/NextDNS.
- [`findings.md`](./findings.md): Temuan fakta terverifikasi mengenai arsitektur Android, mekanisme deteksi aplikasi kloning/adware, dan realitas teknis DNS block page.
- [`analysis.md`](./analysis.md): Analisis perbandingan pendekatan teknis (Trade-offs), matriks kelayakan, dan mitigasi risiko.
- [`experiments.md`](./experiments.md): Rancangan eksperimen terkecil (Proof of Concept / PoC) dan algoritma deteksi.
- [`conclusions.md`](./conclusions.md): Kesimpulan akhir dan rekomendasi arsitektur implementasi.

# Experiments & Proof of Concept (PoC) Designs

Dokumen ini memuat rancangan eksperimen terkecil (Smallest Useful Proof of Concept) yang dapat diuji untuk memverifikasi fungsionalitas sebelum membangun aplikasi penuh.

---

## Eksperimen 1: Algoritma Deteksi Aplikasi Penyamar (Heuristic Disguise Check)

Tujuan: Menguji apakah kita dapat mendeteksi aplikasi berbahaya yang menyamarkan diri sebagai "Google Photos" atau alat sistem lainnya.

### Desain Algoritma (Kotlin / Android SDK):
```kotlin
data class DisguiseRiskAssessment(
    val packageName: String,
    val appLabel: String,
    val riskScore: Int, // 0 - 100
    val reasons: List<String>,
    val isDangerous: Boolean
)

class DisguiseDetector(private val context: Context) {

    // Database referensi aplikasi resmi yang sering ditiru
    private val officialAppCatalog = mapOf(
        "Google Photos" to "com.google.android.apps.photos",
        "WhatsApp" to "com.whatsapp",
        "Google Chrome" to "com.android.chrome",
        "Kalkulator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "System Update" to listOf("com.google.android.gms", "com.android.settings")
    )

    fun assessApp(packageName: String): DisguiseRiskAssessment {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val label = pm.getApplicationLabel(appInfo).toString()

        var score = 0
        val reasons = mutableListOf<String>()

        // 1. Uji Peniruan Nama (Label Mismatch)
        for ((officialLabel, legitPackages) in officialAppCatalog) {
            val isNameSimilar = label.equals(officialLabel, ignoreCase = true) ||
                    calculateLevenshteinDistance(label.lowercase(), officialLabel.lowercase()) <= 2

            if (isNameSimilar) {
                val matchesLegit = when (legitPackages) {
                    is String -> packageName == legitPackages
                    is List<*> -> legitPackages.contains(packageName)
                    else -> false
                }
                if (!matchesLegit) {
                    score += 50
                    reasons.add("Nama aplikasi mirip '$officialLabel', namun package name '$packageName' tidak resmi.")
                }
            }
        }

        // 2. Uji Izin Overlay Iklan (SYSTEM_ALERT_WINDOW)
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        if (requestedPermissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            score += 25
            reasons.add("Aplikasi meminta izin Overlay (SYSTEM_ALERT_WINDOW) yang biasa dipakai adware memunculkan iklan.")
        }

        // 3. Uji Ketiadaan Ikon Peluncur (Hidden App Icon)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val hasLauncherActivity = pm.queryIntentActivities(launcherIntent, 0).isNotEmpty()
        if (!hasLauncherActivity) {
            score += 20
            reasons.add("Aplikasi menyembunyikan ikon peluncur agar tidak terlihat di beranda.")
        }

        // 4. Uji Asal Pemasangan (Sideload / Unknown Sources)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sourceInfo = pm.getInstallSourceInfo(packageName)
            val installer = sourceInfo.installingPackageName
            if (installer != "com.android.vending") {
                score += 15
                reasons.add("Aplikasi tidak dipasang dari Google Play Store (Penginstal: ${installer ?: "Manual/Sideload"}).")
            }
        }

        return DisguiseRiskAssessment(
            packageName = packageName,
            appLabel = label,
            riskScore = score,
            reasons = reasons,
            isDangerous = score >= 50
        )
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        // Implementasi standar penghitung selisih karakter
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1]
                else minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
            }
        }
        return dp[s1.length][s2.length]
    }
}
```

---

## Eksperimen 2: Deteksi Instalasi Baru via BroadcastReceiver

Tujuan: Memastikan FamilyGuard langsung terbangun dan memeriksa aplikasi seketika setelah selesai dipasang.

### Implementasi:
```kotlin
class PackageInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val uri = intent.data ?: return
            val installedPackageName = uri.schemeSpecificPart ?: return

            val detector = DisguiseDetector(context)
            val assessment = detector.assessApp(installedPackageName)

            if (assessment.isDangerous) {
                // Tampilkan Peringatan Darurat ke Orang Tua
                NotificationHelper.showMalwareAlert(context, assessment)
            }
        }
    }
}
```

---

## Eksperimen 3: Filter DNS Lokal & Pemicu Notifikasi Edukasi

Tujuan: Memfilter domain phising/scam lokal tanpa sertifikat error, lalu menampilkan notifikasi visual.

### Alur Eksekusi:
1. `VpnService` menerima paket UDP pada Port 53.
2. Parser membaca QNAME (contoh: `phishing-login.com`).
3. Domain diperiksa terhadap *BloomFilter / Trie* aturan HaGeZi/OISD:
   - Jika **AMAN**: Diteruskan ke Upstream Secure DNS (misal: Cloudflare DoH `1.1.1.1` atau Quad9).
   - Jika **BERBAHAYA**:
     - Kembalikan respons DNS `0.0.0.0` (A) atau `NXDOMAIN`.
     - Kirim broadcast internal ke UI:
       ```kotlin
       fun onDomainBlocked(domain: String, category: String) {
           val intent = Intent(context, ThreatAdvisoryActivity::class.java).apply {
               putExtra("BLOCKED_DOMAIN", domain)
               putExtra("CATEGORY", category)
               flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
           }
           NotificationHelper.showThreatBlockedNotification(context, domain, category, intent)
       }
       ```
4. Pengguna melihat notifikasi seketika di bilah atas ponsel. Mengetuknya akan membuka tampilan UI Android bertema keamanan yang menarik dan edukatif.

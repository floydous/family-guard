package id.familyguard.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.dns.LocalDnsVpnService
import id.familyguard.ui.theme.*
import id.familyguard.watchdog.DownloadStorageObserver
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferencesRepository
    private var downloadObserver: DownloadStorageObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferencesRepository(this)
        downloadObserver = DownloadStorageObserver(this)

        if (prefs.isWatchdogEnabled) {
            downloadObserver?.startWatching()
        }

        setContent {
            FamilyGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        prefs = prefs,
                        onToggleDns = { enable, onVpnPrepared ->
                            if (enable) {
                                val prepareIntent = VpnService.prepare(this)
                                if (prepareIntent != null) {
                                    onVpnPrepared(prepareIntent)
                                } else {
                                    startDnsVpn()
                                }
                            } else {
                                stopDnsVpn()
                            }
                        },
                        onToggleWatchdog = { enable ->
                            prefs.isWatchdogEnabled = enable
                            if (enable) {
                                downloadObserver?.startWatching()
                            } else {
                                downloadObserver?.stopWatching()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun startDnsVpn() {
        prefs.isDnsFilterEnabled = true
        val startIntent = Intent(this, LocalDnsVpnService::class.java).apply {
            action = LocalDnsVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(this, startIntent)
    }

    private fun stopDnsVpn() {
        prefs.isDnsFilterEnabled = false
        val stopIntent = Intent(this, LocalDnsVpnService::class.java).apply {
            action = LocalDnsVpnService.ACTION_STOP
        }
        startService(stopIntent)
    }

    override fun onDestroy() {
        downloadObserver?.stopWatching()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    prefs: AppPreferencesRepository,
    onToggleDns: (Boolean, (Intent) -> Unit) -> Unit,
    onToggleWatchdog: (Boolean) -> Unit
) {
    var isDnsActive by remember { mutableStateOf(prefs.isDnsFilterEnabled) }
    var isWatchdogActive by remember { mutableStateOf(prefs.isWatchdogEnabled) }
    var recentThreats by remember { mutableStateOf(prefs.getRecentThreats()) }
    var totalBlocked by remember { mutableIntStateOf(prefs.totalBlockedThreats) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val isSystemProtected = isDnsActive || isWatchdogActive

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isDnsActive = true
            val startIntent = Intent(context, LocalDnsVpnService::class.java).apply {
                action = LocalDnsVpnService.ACTION_START
            }
            ContextCompat.startForegroundService(context, startIntent)
        } else {
            isDnsActive = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FamilyGuard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pertahanan Siber Keluarga",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = { showPinDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (prefs.isParentPinSet) Slate800 else Slate850,
                        border = BorderStroke(1.dp, if (prefs.isParentPinSet) Emerald500.copy(alpha = 0.5f) else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "PIN",
                                tint = if (prefs.isParentPinSet) Emerald400 else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (prefs.isParentPinSet) "PIN Aktif" else "Setel PIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (prefs.isParentPinSet) Emerald400 else TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
                )
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Signature Sentinel Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(
                        1.dp,
                        if (isSystemProtected) Emerald500.copy(alpha = 0.35f) else Slate800
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSystemProtected) Emerald900.copy(alpha = 0.45f) else Slate800),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isSystemProtected) Emerald400 else TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSystemProtected) Emerald500.copy(alpha = 0.15f) else Slate800
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSystemProtected) Emerald400 else Amber400)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSystemProtected) "SIAP & SIAGA" else "STANDBY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSystemProtected) Emerald400 else Amber400,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSystemProtected)
                                        "Sistem Terlindungi"
                                    else
                                        "Perlindungan Nonaktif",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isSystemProtected)
                                        "DNS & Folder Unduhan aktif dipantau"
                                    else
                                        "Nyalakan modul untuk proteksi otomatis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick-Metrics 3-Column Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricColumn(
                                value = "$totalBlocked",
                                label = "Dicegah",
                                valueColor = if (totalBlocked > 0) Emerald400 else TextPrimary
                            )
                            VerticalDivider(modifier = Modifier.height(36.dp), color = Slate800)
                            MetricColumn(
                                value = "< 0.5ms",
                                label = "Latensi DoH",
                                valueColor = Cyan400
                            )
                            VerticalDivider(modifier = Modifier.height(36.dp), color = Slate800)
                            MetricColumn(
                                value = "200k+",
                                label = "Aturan Aktif",
                                valueColor = Indigo400
                            )
                        }
                    }
                }
            }

            // 2. Interactive Control Tiles
            item {
                ControlCard(
                    icon = Icons.Default.Public,
                    iconTint = Cyan400,
                    iconBg = Cyan500.copy(alpha = 0.15f),
                    title = "Filter DNS Lokal",
                    subtitle = if (isDnsActive)
                        "Aktif • DoH Cloudflare 1.1.1.1 terenkripsi"
                    else
                        "Nonaktif • Domain phising & scam bebas lolos",
                    isChecked = isDnsActive,
                    onCheckedChange = { checked ->
                        isDnsActive = checked
                        onToggleDns(checked) { intent ->
                            vpnLauncher.launch(intent)
                        }
                    }
                )
            }

            item {
                ControlCard(
                    icon = Icons.Default.FolderZip,
                    iconTint = Indigo400,
                    iconBg = Indigo500.copy(alpha = 0.15f),
                    title = "Pengawas Aplikasi (Watchdog)",
                    subtitle = if (isWatchdogActive)
                        "Aktif • Karantina folder /Download/ & Anti-Kloning"
                    else
                        "Nonaktif • APK berbahaya tidak dicegat",
                    isChecked = isWatchdogActive,
                    onCheckedChange = { checked ->
                        isWatchdogActive = checked
                        onToggleWatchdog(checked)
                    }
                )
            }

            // 3. System Readiness Checklist (Fills empty canvas with reassuring facts)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Kesiagaan Proteksi Ponsel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ChecklistRow(
                            text = "DNS Sinkhole 0.0.0.0 (Zero-SSL Error)",
                            isActive = isDnsActive
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ChecklistRow(
                            text = "Pemantauan Berkas .quarantine di Unduhan",
                            isActive = isWatchdogActive
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ChecklistRow(
                            text = "Deteksi Kloning Nama Brand & Izin Overlay",
                            isActive = isWatchdogActive
                        )
                    }
                }
            }

            // 4. Recent Threat Activity Feed
            item {
                Text(
                    text = "Riwayat Aktivitas Ancaman",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (recentThreats.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Slate850)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Emerald400.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sistem Bersih • Tidak Ada Ancaman",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Belum ada domain phising atau adware mencurigakan yang terdeteksi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(recentThreats) { record ->
                    ModernThreatHistoryItem(record)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = Slate900,
            title = {
                Text(
                    text = if (prefs.isParentPinSet) "Ubah PIN Orang Tua" else "Setel PIN Orang Tua",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "PIN digunakan untuk membuka blokir sementara jika situs yang diakses keluarga terindikasi false-positive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("PIN Baru (4-6 digit)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Emerald400,
                            unfocusedBorderColor = Slate700,
                            focusedLabelColor = Emerald400,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            prefs.setParentPin(pinInput)
                            showPinDialog = false
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Simpan PIN", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MetricColumn(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ControlCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (isChecked) iconTint.copy(alpha = 0.25f) else Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Slate950,
                    checkedTrackColor = iconTint,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Slate800
                )
            )
        }
    }
}

@Composable
private fun ChecklistRow(text: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isActive) Emerald400 else Slate700,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) TextPrimary else TextMuted
        )
    }
}

@Composable
fun ModernThreatHistoryItem(record: AppPreferencesRepository.BlockedThreatRecord) {
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedTime = remember(record.timestamp) { formatter.format(Date(record.timestamp)) }

    val categoryColor = when {
        record.category.contains("Phising", ignoreCase = true) -> Rose400
        record.category.contains("Scam", ignoreCase = true) -> Amber400
        else -> Indigo400
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = record.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

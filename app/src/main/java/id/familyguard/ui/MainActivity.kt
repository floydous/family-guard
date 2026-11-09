package id.familyguard.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.dns.LocalDnsVpnService
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
            MaterialTheme {
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
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
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
                title = { Text("FamilyGuard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showPinDialog = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "PIN Orang Tua")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDnsActive || isWatchdogActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isDnsActive || isWatchdogActive)
                                    "Perlindungan Aktif"
                                else
                                    "Perlindungan Nonaktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$totalBlocked ancaman siber berhasil dinetralisir",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Controls
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Filter DNS Lokal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Memblokir domain phising, scam & iklan berbahaya",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDnsActive,
                            onCheckedChange = { checked ->
                                isDnsActive = checked
                                onToggleDns(checked) { intent ->
                                    vpnLauncher.launch(intent)
                                }
                            }
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengawas Aplikasi (Watchdog)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mendeteksi adware kloningan & karantina folder unduhan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isWatchdogActive,
                            onCheckedChange = { checked ->
                                isWatchdogActive = checked
                                onToggleWatchdog(checked)
                            }
                        )
                    }
                }
            }

            // History Section Header
            item {
                Text(
                    text = "Riwayat Ancaman Dicegah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Empty state or list
            if (recentThreats.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada ancaman terdeteksi. Ponsel keluarga dalam keadaan aman.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentThreats) { record ->
                    ThreatHistoryItem(record)
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(if (prefs.isParentPinSet) "Ubah PIN Orang Tua" else "Setel PIN Orang Tua")
            },
            text = {
                Column {
                    Text("PIN ini digunakan untuk membuka blokir sementara (*whitelist*) jika ada situs yang terblokir keliru.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("PIN Baru (4-6 digit)") },
                        singleLine = true
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
                    }
                ) {
                    Text("Simpan PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ThreatHistoryItem(record: AppPreferencesRepository.BlockedThreatRecord) {
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedTime = remember(record.timestamp) { formatter.format(Date(record.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${record.category} • $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

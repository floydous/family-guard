package id.familyguard.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.dns.LocalDnsVpnService
import id.familyguard.ui.theme.*
import id.familyguard.watchdog.DownloadStorageObserver
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    MAIN,
    SETTINGS,
    LOGS
}

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
                    FamilyGuardAppUI(
                        prefs = prefs,
                        onStartDnsVpn = { onVpnPrepared ->
                            val prepareIntent = VpnService.prepare(this)
                            if (prepareIntent != null) {
                                onVpnPrepared(prepareIntent)
                            } else {
                                startDnsVpn()
                            }
                        },
                        onStopDnsVpn = { stopDnsVpn() },
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

@Composable
fun FamilyGuardAppUI(
    prefs: AppPreferencesRepository,
    onStartDnsVpn: ((Intent) -> Unit) -> Unit,
    onStopDnsVpn: () -> Unit,
    onToggleWatchdog: (Boolean) -> Unit
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
    var isDnsActive by remember { mutableStateOf(prefs.isDnsFilterEnabled) }
    var isWatchdogActive by remember { mutableStateOf(prefs.isWatchdogEnabled) }
    var recentThreats by remember { mutableStateOf(prefs.getRecentThreats()) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val isFullyProtected = isDnsActive && isWatchdogActive

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isDnsActive = true
            prefs.isDnsFilterEnabled = true
            val startIntent = Intent(context, LocalDnsVpnService::class.java).apply {
                action = LocalDnsVpnService.ACTION_START
            }
            ContextCompat.startForegroundService(context, startIntent)
        } else {
            isDnsActive = false
            prefs.isDnsFilterEnabled = false
        }
    }

    // Hardware Back Navigation
    BackHandler(enabled = currentScreen != AppScreen.MAIN) {
        when (currentScreen) {
            AppScreen.LOGS -> currentScreen = AppScreen.SETTINGS
            AppScreen.SETTINGS -> currentScreen = AppScreen.MAIN
            AppScreen.MAIN -> {}
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.MAIN -> {
                MainScreen(
                    isProtected = isFullyProtected,
                    onToggleMaster = { enable ->
                        if (enable) {
                            onStartDnsVpn { intent -> vpnLauncher.launch(intent) }
                            isDnsActive = true
                            onToggleWatchdog(true)
                            isWatchdogActive = true
                        } else {
                            onStopDnsVpn()
                            isDnsActive = false
                            onToggleWatchdog(false)
                            isWatchdogActive = false
                        }
                    },
                    onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    isDnsActive = isDnsActive,
                    onToggleDns = { enable ->
                        if (enable) {
                            onStartDnsVpn { intent -> vpnLauncher.launch(intent) }
                            isDnsActive = true
                        } else {
                            onStopDnsVpn()
                            isDnsActive = false
                        }
                    },
                    isWatchdogActive = isWatchdogActive,
                    onToggleWatchdog = { enable ->
                        isWatchdogActive = enable
                        onToggleWatchdog(enable)
                    },
                    onOpenPin = { showPinDialog = true },
                    onOpenWhitelist = { showWhitelistDialog = true },
                    onOpenLogs = {
                        recentThreats = prefs.getRecentThreats()
                        currentScreen = AppScreen.LOGS
                    },
                    onBack = { currentScreen = AppScreen.MAIN }
                )
            }
            AppScreen.LOGS -> {
                LogsScreen(
                    threats = recentThreats,
                    onBack = { currentScreen = AppScreen.SETTINGS }
                )
            }
        }
    }

    // PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (prefs.isParentPinSet) "Ubah PIN Orang Tua" else "Setel PIN Orang Tua",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "PIN digunakan untuk memberikan izin akses jika ada situs atau unduhan yang terblokir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("PIN (4-6 digit)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Whitelist Modal Dialog
    if (showWhitelistDialog) {
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Situs yang Diizinkan",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Situs berikut disetujui orang tua dan tidak akan diblokir:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "portal-belajar-sekolah.id",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Diizinkan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TagQuarantine
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showWhitelistDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun MainScreen(
    isProtected: Boolean,
    onToggleMaster: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Minimal Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "FamilyGuard Logo",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Pengaturan",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center Hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = "FamilyGuard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Proteksi Keluarga",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            CloudflareMasterSwitch(
                checked = isProtected,
                onCheckedChange = onToggleMaster
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isProtected) "Ponsel Terlindungi" else "Perlindungan Nonaktif",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isProtected) {
                    "Perlindungan web dan pengawas unduhan aktif melindungi keluarga."
                } else {
                    "Ponsel saat ini tidak terlindungi dari situs dan file berbahaya."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 260.dp)
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun CloudflareMasterSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 52.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "knobOffset"
    )

    val bgColor = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .width(114.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            }
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun SettingsScreen(
    isDnsActive: Boolean,
    onToggleDns: (Boolean) -> Unit,
    isWatchdogActive: Boolean,
    onToggleWatchdog: (Boolean) -> Unit,
    onOpenPin: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenLogs: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Feature 1: Proteksi Web
            SettingsFeatureRow(
                icon = Icons.Default.Public,
                title = "Proteksi Web",
                subtitle = "Blokir situs berbahaya dan penipuan saat menjelajah.",
                isActive = isDnsActive,
                onToggle = { onToggleDns(!isDnsActive) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Feature 2: Pengawas Unduhan
            SettingsFeatureRow(
                icon = Icons.Default.Shield,
                title = "Pengawas Unduhan",
                subtitle = "Cegah pemasangan file dan aplikasi mencurigakan.",
                isActive = isWatchdogActive,
                onToggle = { onToggleWatchdog(!isWatchdogActive) }
            )

            // Divider Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Feature 3: Parent PIN
            SettingsNavRow(
                title = "PIN Orang Tua",
                subtitle = "Kelola keamanan dan izin khusus",
                onClick = onOpenPin
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Feature 4: Allowed Sites
            SettingsNavRow(
                title = "Situs yang Diizinkan",
                subtitle = "1 situs disetujui untuk diakses",
                onClick = onOpenWhitelist
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Feature 5: Security History
            SettingsNavRow(
                title = "Riwayat Keamanan",
                subtitle = "Lihat aktivitas ancaman yang dicegah",
                onClick = onOpenLogs
            )

            // Divider Band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // App Version
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Versi Aplikasi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "1.8.0 (8)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SettingsFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Aktif",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LogsScreen(
    threats: List<AppPreferencesRepository.BlockedThreatRecord>,
    onBack: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Riwayat Keamanan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Text(
            text = "AKTIVITAS TERBARU",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        if (threats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada catatan ancaman.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(threats) { record ->
                    val isQuarantine = record.category.contains("APK", ignoreCase = true) ||
                            record.category.contains("Aplikasi", ignoreCase = true) ||
                            record.category.contains("Unduhan", ignoreCase = true)

                    val timeStr = remember(record.timestamp) { formatter.format(Date(record.timestamp)) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(right = 12.dp)
                        ) {
                            Text(
                                text = record.domain,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${record.category} • $timeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isQuarantine) TagQuarantine.copy(alpha = 0.15f)
                                    else TagBlocked.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isQuarantine) "DIKARANTINA" else "DIBLOKIR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isQuarantine) TagQuarantine else TagBlocked,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                }

                item {
                    Text(
                        text = "Semua catatan disimpan aman di ponsel ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

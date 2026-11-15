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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import id.familyguard.R
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

    // Refresh logs on screen entry
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.LOGS) {
            recentThreats = prefs.getRecentThreats()
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

    // Zero-overhead hardware accelerated sliding stage
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val widthPx = constraints.maxWidth.toFloat()

        val mainOffset by animateFloatAsState(
            targetValue = when (currentScreen) {
                AppScreen.MAIN -> 0f
                else -> -widthPx * 0.3f
            },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            label = "mainOffset"
        )

        val settingsOffset by animateFloatAsState(
            targetValue = when (currentScreen) {
                AppScreen.MAIN -> widthPx
                AppScreen.SETTINGS -> 0f
                AppScreen.LOGS -> -widthPx * 0.3f
            },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            label = "settingsOffset"
        )

        val logsOffset by animateFloatAsState(
            targetValue = when (currentScreen) {
                AppScreen.LOGS -> 0f
                else -> widthPx
            },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            label = "logsOffset"
        )

        // 1. Main Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = mainOffset }
        ) {
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

        // 2. Settings Screen
        if (currentScreen != AppScreen.MAIN || settingsOffset < widthPx) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = settingsOffset }
            ) {
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
                    onOpenLogs = { currentScreen = AppScreen.LOGS },
                    onBack = { currentScreen = AppScreen.MAIN }
                )
            }
        }

        // 3. Logs Screen
        if (currentScreen == AppScreen.LOGS || logsOffset < widthPx) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = logsOffset }
            ) {
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
        // 1. Minimal Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_signature_shield),
                contentDescription = "FamilyGuard Logo",
                modifier = Modifier.size(32.dp)
            )

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Pengaturan",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 2. Upper-third Golden Ratio Anchor (matching Cloudflare reference)
        Spacer(modifier = Modifier.height(48.dp))

        // 3. Hero Brand Lockup
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_signature_shield),
                contentDescription = "FamilyGuard Signature Logo",
                modifier = Modifier.size(68.dp)
            )
            Column {
                Text(
                    text = "FamilyGuard",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 34.sp
                )
                Text(
                    text = "Proteksi Keluarga",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // 4. Spacer between hero and switch
        Spacer(modifier = Modifier.height(36.dp))

        // 5. Oversized Tactile Master Switch
        CloudflareMasterSwitch(
            checked = isProtected,
            onCheckedChange = onToggleMaster,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // 6. Spacer before status text
        Spacer(modifier = Modifier.height(28.dp))

        // 7. Status Text
        Text(
            text = if (isProtected) "Ponsel Terlindungi" else "Perlindungan Nonaktif",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isProtected) {
                "Perlindungan web dan pengawas unduhan aktif melindungi keluarga."
            } else {
                "Ponsel saat ini tidak terlindungi dari situs dan file berbahaya."
            },
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 300.dp)
        )

        // 8. Generous calm negative space in lower 45%
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun CloudflareMasterSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val knobOffset by animateFloatAsState(
        targetValue = if (checked) 70f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "knobOffset"
    )

    val bgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2E),
        animationSpec = tween(durationMillis = 260),
        label = "switchBg"
    )

    Box(
        modifier = modifier
            .width(154.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            }
            .padding(7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset.dp)
                .size(70.dp)
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
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "Pengaturan",
                fontSize = 22.sp,
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
                    .height(14.dp)
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
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // App Version
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Versi Aplikasi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "1.8.3 (11)",
                    fontSize = 15.sp,
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
            .padding(horizontal = 20.dp, vertical = 20.dp),
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
                    .size(26.dp)
                    .padding(top = 2.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            }
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Aktif",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
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
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "Riwayat Keamanan",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        Text(
            text = "AKTIVITAS TERBARU",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            letterSpacing = 0.5.sp
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
                    fontSize = 15.sp,
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
                                .padding(end = 12.dp)
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
                                fontSize = 13.sp,
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
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isQuarantine) "DIKARANTINA" else "DIBLOKIR",
                                fontSize = 11.sp,
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
                        fontSize = 13.sp,
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

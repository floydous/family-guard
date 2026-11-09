package id.familyguard.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.dns.LocalDnsVpnService
import id.familyguard.ui.theme.*

class ThreatAdvisoryActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferencesRepository(this)

        val targetDomain = intent.getStringExtra(EXTRA_TARGET_DOMAIN) ?: "domain-mencurigakan.com"
        val threatCategory = intent.getStringExtra(EXTRA_THREAT_CATEGORY) ?: "Phising & Penipuan"

        // Log this blocked threat to preferences history
        prefs.recordBlockedThreat(targetDomain, threatCategory)

        setContent {
            FamilyGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ThreatAdvisoryScreen(
                        domain = targetDomain,
                        category = threatCategory,
                        isPinSet = prefs.isParentPinSet,
                        onDismiss = { finish() },
                        onVerifyPin = { pin ->
                            val isValid = if (!prefs.isParentPinSet) {
                                pin == "1234"
                            } else {
                                prefs.verifyParentPin(pin)
                            }

                            if (isValid) {
                                LocalDnsVpnService.filterTrie.addToWhitelist(targetDomain)
                                Toast.makeText(
                                    this,
                                    "Akses ke $targetDomain berhasil diizinkan sementara.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                                true
                            } else {
                                false
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_DOMAIN = "extra_target_domain"
        const val EXTRA_THREAT_CATEGORY = "extra_threat_category"
    }
}

@Composable
fun ThreatAdvisoryScreen(
    domain: String,
    category: String,
    isPinSet: Boolean,
    onDismiss: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Shield Alert Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Rose500.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield Alert",
                tint = Rose400,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Akses Berbahaya Dicegah",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "FamilyGuard mengamankan ponsel keluarga dari tautan mencurigakan:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Blocked Target Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Rose500.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = domain,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Rose400
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Rose500.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Kategori: $category",
                        style = MaterialTheme.typography.labelSmall,
                        color = Rose400,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Educational Section 1
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mengapa situs ini dicegah?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Situs ini terdaftar dalam intelijen reputasi global (HaGeZi & OISD) sebagai ancaman phising pencurian kredensial atau jebakan adware palsu yang merugikan pengguna.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Educational Section 2 (Literasi Anak)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Amber400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tips Cerdas untuk Anak",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Jangan pernah membagikan nomor telepon, PIN, atau kata sandi di internet.\n• Jika melihat tawaran hadiah gratis atau klaim virus, segera panggil orang tua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Action Button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
        ) {
            Text(
                text = "Kembali ke Tempat Aman",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Slate950
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Override Button
        OutlinedButton(
            onClick = {
                pinText = ""
                pinError = false
                showPinDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Slate700)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Buka Akses Khusus Orang Tua (PIN)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = Slate900,
            title = {
                Text(
                    text = "Otorisasi Orang Tua",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isPinSet)
                            "Masukkan PIN Orang Tua untuk mengizinkan akses sementara ke situs ini:"
                        else
                            "PIN belum disetel di Dashboard (PIN default: 1234). Masukkan PIN untuk membuka:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            pinText = it
                            pinError = false
                        },
                        label = { Text("PIN Orang Tua") },
                        isError = pinError,
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
                    if (pinError) {
                        Text(
                            text = "PIN salah! Akses ke situs tetap diblokir.",
                            color = Rose400,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onVerifyPin(pinText)
                        if (success) {
                            showPinDialog = false
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Buka Akses", color = Slate950, fontWeight = FontWeight.Bold)
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

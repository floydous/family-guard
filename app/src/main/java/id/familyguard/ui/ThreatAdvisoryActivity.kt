package id.familyguard.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.dns.LocalDnsVpnService

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
            MaterialTheme {
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
                                // Default PIN 1234 if parent hasn't configured a custom PIN
                                pin == "1234"
                            } else {
                                prefs.verifyParentPin(pin)
                            }

                            if (isValid) {
                                // Whitelist domain in DNS filter
                                LocalDnsVpnService.filterTrie.addToWhitelist(targetDomain)
                                Toast.makeText(
                                    this,
                                    "Akses ke $targetDomain diizinkan sementara.",
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
        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Shield Alert",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Akses Diblokir demi Keamanan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "FamilyGuard mencegah akses ke situs yang terdeteksi berbahaya:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = domain,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Kategori: $category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Educational Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mengapa situs ini diblokir?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Situs ini terdaftar dalam basis data reputasi global (HaGeZi/OISD) sebagai ancaman penipuan, pencurian kata sandi, atau penyebaran adware yang sering menyamar sebagai undian/pembaruan sistem palsu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tips Aman Berinternet untuk Anak",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Jangan pernah memasukkan nomor telepon, PIN, atau kata sandi di situs yang tidak dikenal.\n• Jika melihat iklan hadiah gratis atau klaim ponsel rusak, segera beri tahu orang tua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Kembali ke Tempat Aman")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                pinText = ""
                pinError = false
                showPinDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buka Akses Khusus Orang Tua (PIN)")
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Otorisasi Orang Tua") },
            text = {
                Column {
                    Text(
                        if (isPinSet)
                            "Masukkan PIN Orang Tua untuk mengizinkan akses sementara ke situs ini:"
                        else
                            "PIN belum disetel di Dashboard (PIN default: 1234). Masukkan PIN untuk membuka:"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            pinText = it
                            pinError = false
                        },
                        label = { Text("PIN Orang Tua") },
                        isError = pinError,
                        singleLine = true
                    )
                    if (pinError) {
                        Text(
                            text = "PIN salah! Akses tetap diblokir.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
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
                    }
                ) {
                    Text("Buka Akses")
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

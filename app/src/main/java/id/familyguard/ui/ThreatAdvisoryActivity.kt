package id.familyguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class ThreatAdvisoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetDomain = intent.getStringExtra(EXTRA_TARGET_DOMAIN) ?: "domain-mencurigakan.com"
        val threatCategory = intent.getStringExtra(EXTRA_THREAT_CATEGORY) ?: "Phising & Penipuan"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ThreatAdvisoryScreen(
                        domain = targetDomain,
                        category = threatCategory,
                        onDismiss = { finish() },
                        onOverride = { pin ->
                            // TODO: Verify Parent PIN and add domain to whitelist
                            finish()
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
    onDismiss: () -> Unit,
    onOverride: (String) -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Shield Alert",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(80.dp)
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
            text = "FamilyGuard mencegah akses ke alamat yang terindikasi berbahaya:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            onClick = { showPinDialog = true },
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
                    Text("Masukkan PIN Orang Tua untuk mengizinkan akses sementara ke situs ini:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { pinText = it },
                        label = { Text("PIN Orang Tua") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        onOverride(pinText)
                    }
                ) {
                    Text("Izinkan")
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

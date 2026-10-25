package com.ciro.vault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciro.vault.model.BackupCodeItem
import com.ciro.vault.model.FolderItem
import com.ciro.vault.model.VaultItem
import com.ciro.vault.ui.components.IconRegistry
import com.ciro.vault.ui.theme.AccentCyan
import com.ciro.vault.ui.theme.BgDark
import com.ciro.vault.ui.theme.SurfaceDark
import com.ciro.vault.ui.theme.TextPrimary
import com.ciro.vault.ui.theme.TextSecondary
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEditorDialog(
    initialItem: VaultItem,
    folders: List<FolderItem>,
    existingVaultItems: List<VaultItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (VaultItem) -> Unit
) {
    var title by remember { mutableStateOf(initialItem.title) }
    var selectedFolderId by remember { mutableStateOf(initialItem.folderId) }
    var iconKey by remember { mutableStateOf(initialItem.iconKey) }

    // Jalur 1
    var username by remember { mutableStateOf(initialItem.username) }
    var password by remember { mutableStateOf(initialItem.password) }

    // Jalur 2
    var phoneNumber by remember { mutableStateOf(initialItem.phoneNumber) }
    var pin by remember { mutableStateOf(initialItem.pin) }

    // Jalur 3
    var linkedAccount by remember { mutableStateOf(initialItem.linkedAccount) }

    // Jalur 4 (API Key)
    var apiKey by remember { mutableStateOf(initialItem.apiKey) }

    // Jalur 5 (2FA Backup codes dalam format teks per baris)
    var rawBackupCodes by remember {
        mutableStateOf(initialItem.backupCodes.joinToString("\n") { it.code })
    }

    var loginUrl by remember { mutableStateOf(initialItem.loginUrl) }
    var notes by remember { mutableStateOf(initialItem.notes) }

    fun generateRandomPassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val rand = SecureRandom()
        return (1..length).map { chars[rand.nextInt(chars.length)] }.joinToString("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Kelola Akun / Kredensial", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // JUDUL / NAMA LAYANAN
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (iconKey == "general" || iconKey.isBlank()) {
                                iconKey = IconRegistry.suggestIconKey(it)
                            }
                        },
                        label = { Text("Nama Layanan / Akun (misal: Tokopedia, GitHub, Finnhub)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // PILIHAN FOLDER
                    if (folders.isNotEmpty()) {
                        Text("Pilih Folder:", fontSize = 12.sp, color = TextPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedFolderId.isEmpty(),
                                onClick = { selectedFolderId = "" },
                                label = { Text("Tanpa Folder") }
                            )
                            folders.forEach { f ->
                                FilterChip(
                                    selected = selectedFolderId == f.id,
                                    onClick = { selectedFolderId = f.id },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                    }

                    // PILIHAN IKON LAYANAN (GRID IKON RINGKAS TANPA TEKS BOROS)
                    Text("Pilih Ikon Layanan:", fontSize = 12.sp, color = TextPrimary)
                    val availableIcons = listOf(
                        "github", "google", "x", "instagram", "facebook", "social",
                        "cloudflare", "telegram", "whatsapp", "discord", "spotify", "steam",
                        "bank", "shopping", "api", "terminal", "web", "lock", "key"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableIcons.forEach { key ->
                            val isSelected = iconKey == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AccentCyan.copy(alpha = 0.25f) else BgDark,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, AccentCyan) else androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { iconKey = key }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = IconRegistry.getPainter(key),
                                        contentDescription = key,
                                        tint = if (isSelected) AccentCyan else TextPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // JALUR 1: USERNAME & PASSWORD
                    Text("🔑 Jalur Username & Password:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password / Kata Sandi") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = { password = generateRandomPassword(16) }) {
                            Icon(Icons.Default.Casino, contentDescription = "Generate Password", tint = AccentCyan)
                        }
                    }

                    HorizontalDivider()

                    // JALUR 2: NOMOR HP & PIN (TATA LETAK PROPORSIAL TURUN KE BAWAH)
                    Text("📱 Jalur Nomor HP (GoPay, DANA, WA, OTP):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Nomor HP (+62...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("PIN Transaksi / Dompet (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // JALUR 3: TERTAUT SIGN IN WITH (SSO)
                    Text("🌐 Sign In with (Google, X, IG, FB, dll):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Opsi Cepat: Pilih dari Akun Tersimpan di CiroVault
                    val otherSavedAccounts = remember(existingVaultItems) {
                        existingVaultItems.filter { it.id != initialItem.id && it.title.isNotBlank() }
                    }
                    if (otherSavedAccounts.isNotEmpty()) {
                        Text("Hubungkan dengan akun tersimpan:", fontSize = 11.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherSavedAccounts.forEach { acc ->
                                val userLabel = if (acc.username.isNotBlank()) acc.username else if (acc.phoneNumber.isNotBlank()) acc.phoneNumber else ""
                                val chipText = if (userLabel.isNotBlank()) "${acc.title} ($userLabel)" else acc.title
                                val isSelected = linkedAccount.contains(acc.title, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        linkedAccount = if (userLabel.isNotBlank()) "${acc.title}: $userLabel" else acc.title
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = IconRegistry.getPainter(acc.iconKey),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(chipText, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = linkedAccount,
                        onValueChange = { linkedAccount = it },
                        label = { Text("Penyedia & Akun Login (misal: IG: Dwinggunenseregipun)") },
                        placeholder = { Text("IG: Dwinggunenseregipun atau Google: user@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // JALUR 4: API KEY / SECRET
                    Text("🔑 Brankas API Key / Token:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key / Token Rahasia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // JALUR 5: 2FA BACKUP CODES
                    Text("🆘 Kode Cadangan 2FA (1 kode per baris):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = rawBackupCodes,
                        onValueChange = { rawBackupCodes = it },
                        placeholder = { Text("1234 5678\n8765 4321\n...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // LINK LOGIN SMART QUICK LAUNCH
                    Text("🚀 Smart Quick-Launch URL:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = loginUrl,
                        onValueChange = { loginUrl = it },
                        label = { Text("Link Halaman Login (misal: github.com/login)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // CATATAN TAMBAHAN
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BOTTOM ACTION BAR (TOMBOL SIMPAN & BATAL DI BAWAH)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.35f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val parsedCodes = rawBackupCodes.lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .map { BackupCodeItem(code = it) }

                            val saved = initialItem.copy(
                                title = title.trim(),
                                folderId = selectedFolderId,
                                iconKey = if (iconKey == "general") IconRegistry.suggestIconKey(title) else iconKey,
                                username = username.trim(),
                                password = password,
                                phoneNumber = phoneNumber.trim(),
                                pin = pin.trim(),
                                linkedAccount = linkedAccount.trim(),
                                apiKey = apiKey.trim(),
                                backupCodes = if (parsedCodes.isNotEmpty()) parsedCodes else initialItem.backupCodes,
                                loginUrl = loginUrl.trim(),
                                notes = notes.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(saved)
                        },
                        modifier = Modifier.weight(0.65f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = androidx.compose.ui.graphics.Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Akun", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

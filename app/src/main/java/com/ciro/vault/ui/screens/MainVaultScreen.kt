package com.ciro.vault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciro.vault.data.VaultRepository
import com.ciro.vault.model.BackupCodeItem
import com.ciro.vault.model.FolderItem
import com.ciro.vault.model.NoteItem
import com.ciro.vault.model.VaultItem
import com.ciro.vault.ui.components.IconRegistry
import com.ciro.vault.ui.theme.*
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScreen(
    repository: VaultRepository,
    onLockRequested: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Notes, 1: Vault
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) } // null = All
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Editor States
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    var isCreatingNote by remember { mutableStateOf(false) }

    var editingVaultItem by remember { mutableStateOf<VaultItem?>(null) }
    var isCreatingVaultItem by remember { mutableStateOf(false) }

    // Refresh Trigger
    var refreshKey by remember { mutableIntStateOf(0) }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = repository.getEncryptedBackupBytes()
                if (bytes != null) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    Toast.makeText(context, "Berhasil ekspor ke file .cirovault!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal ekspor: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && repository.currentMasterPass != null) {
                    val ok = repository.importEncryptedBytes(bytes, repository.currentMasterPass!!)
                    if (ok) {
                        refreshKey++
                        Toast.makeText(context, "Berhasil memulihkan/impor data dari file .cirovault!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Gagal dekripsi: Password saat ini tidak cocok dengan berkas cadangan!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal impor berkas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CiroVault",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { showBackupDialog = true }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Backup", tint = AccentCyan)
                    }
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = TextPrimary)
                    }
                    IconButton(onClick = onLockRequested) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = AccentOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedFolderId = null
                    },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    label = { Text("Catatan Bebas") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedFolderId = null
                    },
                    icon = { Icon(Icons.Default.Key, contentDescription = "Vault") },
                    label = { Text("Brankas Sandi & API") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        isCreatingNote = true
                    } else {
                        isCreatingVaultItem = true
                    }
                },
                containerColor = AccentCyan,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // GLOBAL SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari catatan, akun, URL, atau API key...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            // FOLDER CHIP HORIZONTAL SCROLL (100% CLEAN SLATE)
            val currentFolders = remember(selectedTab, refreshKey) {
                val targetType = if (selectedTab == 0) "NOTE" else "VAULT"
                repository.cachedFolders.filter { it.type == targetType }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    label = { Text("Semua (${if (selectedTab == 0) repository.cachedNotes.size else repository.cachedVaultItems.size})") }
                )

                currentFolders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        leadingIcon = {
                            Icon(
                                painter = IconRegistry.getPainter(folder.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(folder.name) }
                    )
                }
            }

            // CONTENT LIST
            if (selectedTab == 0) {
                // TAB CATATAN
                val filteredNotes = remember(searchQuery, selectedFolderId, refreshKey) {
                    repository.cachedNotes.filter { note ->
                        val matchesFolder = selectedFolderId == null || note.folderId == selectedFolderId
                        val matchesSearch = searchQuery.isBlank() ||
                                note.title.contains(searchQuery, ignoreCase = true) ||
                                note.content.contains(searchQuery, ignoreCase = true)
                        matchesFolder && matchesSearch
                    }.sortedByDescending { it.isPinned }
                }

                if (filteredNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada catatan. Klik tombol + untuk menulis!", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { editingNote = note },
                                onDelete = {
                                    repository.cachedNotes.removeAll { it.id == note.id }
                                    repository.save()
                                    refreshKey++
                                },
                                onTogglePin = {
                                    val idx = repository.cachedNotes.indexOfFirst { it.id == note.id }
                                    if (idx != -1) {
                                        repository.cachedNotes[idx] = note.copy(isPinned = !note.isPinned)
                                        repository.save()
                                        refreshKey++
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // TAB BRANKAS SANDI & API
                val filteredVault = remember(searchQuery, selectedFolderId, refreshKey) {
                    repository.cachedVaultItems.filter { item ->
                        val matchesFolder = selectedFolderId == null || item.folderId == selectedFolderId
                        val matchesSearch = searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                item.username.contains(searchQuery, ignoreCase = true) ||
                                item.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                                item.loginUrl.contains(searchQuery, ignoreCase = true) ||
                                item.apiKey.contains(searchQuery, ignoreCase = true)
                        matchesFolder && matchesSearch && !item.isDeleted
                    }.sortedByDescending { it.isPinned }
                }

                if (filteredVault.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Brankas kosong. Tambah akun, 2FA, atau API key via +", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredVault, key = { it.id }) { item ->
                            VaultCard(
                                item = item,
                                context = context,
                                onClick = { editingVaultItem = item },
                                onDelete = {
                                    val idx = repository.cachedVaultItems.indexOfFirst { it.id == item.id }
                                    if (idx != -1) {
                                        repository.cachedVaultItems[idx] = item.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
                                        repository.save()
                                        refreshKey++
                                    }
                                },
                                onTogglePin = {
                                    val idx = repository.cachedVaultItems.indexOfFirst { it.id == item.id }
                                    if (idx != -1) {
                                        repository.cachedVaultItems[idx] = item.copy(isPinned = !item.isPinned)
                                        repository.save()
                                        refreshKey++
                                    }
                                },
                                onBackupCodeToggled = { codeIdx ->
                                    val updatedCodes = item.backupCodes.toMutableList()
                                    val c = updatedCodes[codeIdx]
                                    updatedCodes[codeIdx] = c.copy(
                                        isUsed = !c.isUsed,
                                        usedAt = if (!c.isUsed) System.currentTimeMillis() else null
                                    )
                                    val idx = repository.cachedVaultItems.indexOfFirst { it.id == item.id }
                                    if (idx != -1) {
                                        repository.cachedVaultItems[idx] = item.copy(backupCodes = updatedCodes)
                                        repository.save()
                                        refreshKey++
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG BUAT FOLDER BARU
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var selectedIconKey by remember { mutableStateOf("folder") }

        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Buat Folder Baru (${if (selectedTab == 0) "Catatan" else "Brankas"})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Nama Folder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            val newFolder = FolderItem(
                                name = folderName.trim(),
                                iconKey = selectedIconKey,
                                type = if (selectedTab == 0) "NOTE" else "VAULT"
                            )
                            repository.cachedFolders.add(newFolder)
                            repository.save()
                            refreshKey++
                            showNewFolderDialog = false
                        }
                    }
                ) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Batal") }
            }
        )
    }

    // DIALOG BACKUP & RESTORE PORTABEL (.cirovault)
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Cadangan Database (.cirovault)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kelola file database portabel terenkripsi AES-256 untuk seluruh Catatan & Brankas Sandi.")
                    
                    HorizontalDivider()

                    Button(
                        onClick = {
                            showBackupDialog = false
                            val fileName = "CiroVault_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.cirovault"
                            exportLauncher.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekspor / Backup File Baru", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showBackupDialog = false
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Impor / Pulihkan dari File", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Tutup") }
            }
        )
    }

    // NOTE EDITOR SHEET / DIALOG
    if (isCreatingNote || editingNote != null) {
        val target = editingNote ?: NoteItem(title = "", content = "", folderId = selectedFolderId ?: "")
        NoteEditorDialog(
            initialNote = target,
            folders = repository.cachedFolders.filter { it.type == "NOTE" },
            onDismiss = {
                isCreatingNote = false
                editingNote = null
            },
            onSave = { savedNote ->
                val idx = repository.cachedNotes.indexOfFirst { it.id == savedNote.id }
                if (idx != -1) {
                    repository.cachedNotes[idx] = savedNote
                } else {
                    repository.cachedNotes.add(0, savedNote)
                }
                repository.save()
                refreshKey++
                isCreatingNote = false
                editingNote = null
            }
        )
    }

    // VAULT ITEM EDITOR DIALOG
    if (isCreatingVaultItem || editingVaultItem != null) {
        val target = editingVaultItem ?: VaultItem(title = "", folderId = selectedFolderId ?: "")
        VaultEditorDialog(
            initialItem = target,
            folders = repository.cachedFolders.filter { it.type == "VAULT" },
            onDismiss = {
                isCreatingVaultItem = false
                editingVaultItem = null
            },
            onSave = { savedItem ->
                val idx = repository.cachedVaultItems.indexOfFirst { it.id == savedItem.id }
                if (idx != -1) {
                    repository.cachedVaultItems[idx] = savedItem
                } else {
                    repository.cachedVaultItems.add(0, savedItem)
                }
                repository.save()
                refreshKey++
                isCreatingVaultItem = false
                editingVaultItem = null
            }
        )
    }
}

// -------------------------------------------------------------
// NOTE CARD COMPONENT
// -------------------------------------------------------------
@Composable
fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note.title.isBlank()) "Tanpa Judul" else note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) AccentOrange else TextSecondary
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary)
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
            Text(text = dateStr, fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.6f))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Catatan?") },
            text = { Text("Yakin ingin menghapus catatan '${note.title}'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}

// -------------------------------------------------------------
// VAULT CARD COMPONENT (MULTI-JALUR + QUICK LAUNCH)
// -------------------------------------------------------------
@Composable
fun VaultCard(
    item: VaultItem,
    context: Context,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onBackupCodeToggled: (Int) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isPassRevealed by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = IconRegistry.getPainter(item.iconKey),
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // SMART QUICK LAUNCH BUTTON (1-TAP OPEN URL)
                if (item.loginUrl.isNotBlank()) {
                    IconButton(onClick = {
                        try {
                            val url = if (!item.loginUrl.startsWith("http://") && !item.loginUrl.startsWith("https://")) {
                                "https://${item.loginUrl}"
                            } else item.loginUrl
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka link: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Launch, contentDescription = "Quick Launch", tint = AccentCyan)
                    }
                }

                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pin",
                        tint = if (item.isPinned) AccentOrange else TextSecondary
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // JALUR 1: USERNAME & PASSWORD
            if (item.username.isNotBlank() || item.password.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (item.username.isNotBlank()) {
                            Text(text = "User: ${item.username}", fontSize = 13.sp, color = TextPrimary)
                        }
                        if (item.password.isNotBlank()) {
                            Text(
                                text = "Sandi: ${if (isPassRevealed) item.password else "••••••••"}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Row {
                        if (item.password.isNotBlank()) {
                            IconButton(onClick = { isPassRevealed = !isPassRevealed }) {
                                Icon(
                                    imageVector = if (isPassRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { copyToClipboard("Password", item.password) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = AccentCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // JALUR 2: NOMOR TELEPON & PIN
            if (item.phoneNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📱 HP: ${item.phoneNumber} ${if (item.pin.isNotBlank()) "(PIN: ${item.pin})" else ""}",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    IconButton(onClick = { copyToClipboard("Nomor HP", item.phoneNumber) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Phone", tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // JALUR 3: TERTAUT SSO
            if (item.linkedAccount.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BgDark
                ) {
                    Text(
                        text = "🌐 Numpang Login: ${item.linkedAccount}",
                        fontSize = 12.sp,
                        color = AccentOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // JALUR 4: API KEY / SECRET TOKEN
            if (item.apiKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔑 API: ${if (isPassRevealed) item.apiKey else item.apiKey.take(6) + "••••••••"}",
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                    IconButton(onClick = { copyToClipboard("API Key", item.apiKey) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy API Key", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // JALUR 5: 2FA BACKUP CODES LIST DENGAN CHECKLIST CORET
            if (item.backupCodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🆘 Kode Cadangan Darurat 2FA (Klik untuk coret jika sudah dipakai):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                item.backupCodes.forEachIndexed { idx, codeItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackupCodeToggled(idx) }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = codeItem.isUsed,
                            onCheckedChange = { onBackupCodeToggled(idx) },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = codeItem.code,
                            fontSize = 13.sp,
                            textDecoration = if (codeItem.isUsed) TextDecoration.LineThrough else null,
                            color = if (codeItem.isUsed) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { copyToClipboard("Kode Cadangan", codeItem.code) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Akun?") },
            text = { Text("Akun '${item.title}' akan dipindahkan ke tong sampah.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}

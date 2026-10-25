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
    var selectedFolderId by remember { mutableStateOf<String?>(null) } // null = Root / Semua Folder
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderItem?>(null) }
    var folderToEdit by remember { mutableStateOf<FolderItem?>(null) }

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

    val currentFolder = remember(selectedFolderId, refreshKey) {
        if (selectedFolderId != null) {
            repository.cachedFolders.find { it.id == selectedFolderId }
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (selectedFolderId != null) {
                        IconButton(onClick = { selectedFolderId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                        }
                    }
                },
                title = {
                    Text(
                        text = if (selectedFolderId != null) (currentFolder?.name ?: "Folder") else "CiroVault",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    if (selectedFolderId != null && currentFolder != null) {
                        IconButton(onClick = { folderToEdit = currentFolder }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Folder", tint = AccentCyan)
                        }
                        IconButton(onClick = { folderToDelete = currentFolder }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Folder", tint = TextSecondary)
                        }
                    }
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

            // FOLDER BREADCRUMB & ITEM COUNT BAR (KeePassDX Style)
            val currentFolders = remember(selectedTab, refreshKey) {
                val targetType = if (selectedTab == 0) "NOTE" else "VAULT"
                repository.cachedFolders.filter { it.type == targetType }
            }

            // Breadcrumb bar jika sedang di dalam folder
            if (selectedFolderId != null && currentFolder != null) {
                Surface(
                    color = SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFolderId = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = IconRegistry.getPainter(currentFolder.iconKey),
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "Catatan" else "Brankas",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentFolder.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                }
            }

            // CONTENT LIST
            if (selectedTab == 0) {
                // TAB CATATAN
                val allNotes = repository.cachedNotes
                val filteredNotes = remember(searchQuery, selectedFolderId, refreshKey) {
                    allNotes.filter { note ->
                        val matchesFolder = if (selectedFolderId == null) {
                            // Di root: tampilkan catatan yang tidak ada foldernya (atau semua jika searching)
                            searchQuery.isNotBlank() || note.folderId.isBlank()
                        } else {
                            note.folderId == selectedFolderId
                        }
                        val matchesSearch = searchQuery.isBlank() ||
                                note.title.contains(searchQuery, ignoreCase = true) ||
                                note.content.contains(searchQuery, ignoreCase = true)
                        matchesFolder && matchesSearch
                    }.sortedByDescending { it.isPinned }
                }

                val visibleFolders = remember(searchQuery, selectedFolderId, refreshKey) {
                    if (selectedFolderId == null) {
                        currentFolders.filter { folder ->
                            searchQuery.isBlank() || folder.name.contains(searchQuery, ignoreCase = true)
                        }
                    } else emptyList()
                }

                if (filteredNotes.isEmpty() && visibleFolders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedFolderId != null) "Folder ini masih kosong." else "Belum ada catatan. Klik tombol + untuk menulis!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Render folder KeePassDX style jika di root
                        items(visibleFolders, key = { "folder_${it.id}" }) { folder ->
                            val itemCount = allNotes.count { it.folderId == folder.id }
                            KeePassFolderRow(
                                folder = folder,
                                itemCount = itemCount,
                                onClick = { selectedFolderId = folder.id },
                                onEdit = { folderToEdit = folder },
                                onDelete = { folderToDelete = folder }
                            )
                        }

                        // Render note items
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
                val allVault = repository.cachedVaultItems
                val filteredVault = remember(searchQuery, selectedFolderId, refreshKey) {
                    allVault.filter { item ->
                        val matchesFolder = if (selectedFolderId == null) {
                            // Di root: tampilkan akun yang tidak ada foldernya (atau semua jika searching)
                            searchQuery.isNotBlank() || item.folderId.isBlank()
                        } else {
                            item.folderId == selectedFolderId
                        }
                        val matchesSearch = searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                item.username.contains(searchQuery, ignoreCase = true) ||
                                item.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                                item.loginUrl.contains(searchQuery, ignoreCase = true) ||
                                item.apiKey.contains(searchQuery, ignoreCase = true)
                        matchesFolder && matchesSearch && !item.isDeleted
                    }.sortedByDescending { it.isPinned }
                }

                val visibleFolders = remember(searchQuery, selectedFolderId, refreshKey) {
                    if (selectedFolderId == null) {
                        currentFolders.filter { folder ->
                            searchQuery.isBlank() || folder.name.contains(searchQuery, ignoreCase = true)
                        }
                    } else emptyList()
                }

                if (filteredVault.isEmpty() && visibleFolders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedFolderId != null) "Folder ini masih kosong." else "Brankas kosong. Tambah akun, 2FA, atau API key via +",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Render folder KeePassDX style jika di root
                        items(visibleFolders, key = { "folder_${it.id}" }) { folder ->
                            val itemCount = allVault.count { it.folderId == folder.id && !it.isDeleted }
                            KeePassFolderRow(
                                folder = folder,
                                itemCount = itemCount,
                                onClick = { selectedFolderId = folder.id },
                                onEdit = { folderToEdit = folder },
                                onDelete = { folderToDelete = folder }
                            )
                        }

                        // Render vault card items
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

    // DIALOG EDIT NAMA FOLDER
    if (folderToEdit != null) {
        val target = folderToEdit!!
        var editedName by remember { mutableStateOf(target.name) }

        AlertDialog(
            onDismissRequest = { folderToEdit = null },
            title = { Text("Ubah Nama Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Nama Folder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            val idx = repository.cachedFolders.indexOfFirst { it.id == target.id }
                            if (idx != -1) {
                                repository.cachedFolders[idx] = target.copy(name = editedName.trim())
                                repository.save()
                                refreshKey++
                            }
                            folderToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
                ) {
                    Text("Simpan Perubahan")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG KONFIRMASI HAPUS FOLDER
    if (folderToDelete != null) {
        val target = folderToDelete!!
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Hapus Folder '${target.name}'?") },
            text = { Text("Folder akan dihapus. Item di dalamnya tidak akan terhapus, melainkan dipindahkan ke Root.") },
            confirmButton = {
                Button(
                    onClick = {
                        val folderId = target.id
                        // Bersihkan folderId di item
                        if (target.type == "NOTE") {
                            repository.cachedNotes.forEachIndexed { idx, note ->
                                if (note.folderId == folderId) {
                                    repository.cachedNotes[idx] = note.copy(folderId = "")
                                }
                            }
                        } else {
                            repository.cachedVaultItems.forEachIndexed { idx, item ->
                                if (item.folderId == folderId) {
                                    repository.cachedVaultItems[idx] = item.copy(folderId = "")
                                }
                            }
                        }
                        repository.cachedFolders.removeAll { it.id == folderId }
                        repository.save()
                        if (selectedFolderId == folderId) {
                            selectedFolderId = null
                        }
                        folderToDelete = null
                        refreshKey++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Batal")
                }
            }
        )
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
// KEEPASSDX STYLE FOLDER ROW COMPONENT
// -------------------------------------------------------------
@Composable
fun KeePassFolderRow(
    folder: FolderItem,
    itemCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon folder dengan Badge Oranye persis KeePassDX
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = TextPrimary.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                )
                // Badge oranye lonjong/bulat dengan angka item
                Surface(
                    color = AccentOrange,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(18.dp).defaultMinSize(minWidth = 18.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = itemCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nama Folder
            Text(
                text = folder.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Edit nama folder
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Nama Folder",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Hapus folder
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus Folder",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Panah masuk
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Buka",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
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

package com.ciro.vault.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciro.vault.model.FolderItem
import com.ciro.vault.model.NoteItem
import com.ciro.vault.ui.theme.AccentCyan
import com.ciro.vault.ui.theme.SurfaceDark
import com.ciro.vault.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    initialNote: NoteItem,
    folders: List<FolderItem>,
    onDismiss: () -> Unit,
    onSave: (NoteItem) -> Unit
) {
    var title by remember { mutableStateOf(initialNote.title) }
    var content by remember { mutableStateOf(initialNote.content) }
    var selectedFolderId by remember { mutableStateOf(initialNote.folderId) }

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
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Tulis Catatan", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FOLDER SELECTOR CHIPS
                if (folders.isNotEmpty()) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // JUDUL TANPA LIMIT
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Judul Catatan (Bebas tanpa limit)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // SMART TOOLBAR (DI ATAS ISI CATATAN)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { content += "\n[ ] " }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("☑ Checklist")
                    }
                    FilledTonalButton(onClick = { content += "\n• " }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("• Bullet")
                    }
                    FilledTonalButton(onClick = { content += "\n1. " }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("1. Nomor")
                    }
                    FilledTonalButton(onClick = { content += "**teks**" }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("Bold")
                    }
                    FilledTonalButton(onClick = { content += "\n---\n" }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text("― Garis")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ISI CATATAN UNLIMITED TEXT
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Ketik catatan bebas tanpa batas di sini...") },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )

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
                            val saved = initialNote.copy(
                                title = title.trim(),
                                content = content,
                                folderId = selectedFolderId,
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
                        Text("Simpan Catatan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

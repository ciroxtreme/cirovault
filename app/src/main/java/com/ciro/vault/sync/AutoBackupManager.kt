package com.ciro.vault.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ciro.vault.data.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object AutoBackupManager {
    private const val ENDPOINT_URL = "https://cirovault-backup.ciroxtreme.workers.dev/upload"
    private const val VAULT_SECRET_KEY = "cirovault_secret_2026_ciro"

    fun triggerAutoBackup(
        context: Context,
        repository: VaultRepository,
        noteTitle: String = "Catatan",
        action: String = "save",
        isManual: Boolean = false
    ) {
        if (isManual) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "☁️ Mengunggah cadangan online...", Toast.LENGTH_SHORT).show()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bytes = repository.getEncryptedBackupBytes()
                if (bytes == null || bytes.isEmpty()) {
                    if (isManual) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Database kosong atau belum siap", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }

                val url = URL(ENDPOINT_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setRequestProperty("X-Vault-Key", VAULT_SECRET_KEY)
                    setRequestProperty("X-Note-Title", noteTitle)
                    setRequestProperty("X-Action", action)
                }

                conn.outputStream.use { out ->
                    out.write(bytes)
                    out.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    Handler(Looper.getMainLooper()).post {
                        val msg = if (isManual) "✅ Cadangan online terkirim ke Telegram & Google Drive!" else "☁️ Cadangan CiroVault tersinkron!"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else if (isManual) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "❌ Gagal kirim cadangan (Kode HTTP $responseCode)", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                if (isManual) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "❌ Gagal: ${e.message ?: "Cek koneksi internet"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

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
        action: String = "save"
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bytes = repository.getEncryptedBackupBytes() ?: return@launch
                if (bytes.isEmpty()) return@launch

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
                        Toast.makeText(context, "☁️ Cadangan CiroVault terkirim!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                // Senyap jika offline atau koneksi gagal, agar tidak mengganggu pengalaman pengguna
            }
        }
    }
}

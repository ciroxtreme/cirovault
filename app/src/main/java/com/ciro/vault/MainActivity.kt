package com.ciro.vault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.ciro.vault.data.VaultRepository
import com.ciro.vault.ui.screens.LockScreen
import com.ciro.vault.ui.screens.MainVaultScreen
import com.ciro.vault.ui.theme.CiroVaultColorScheme
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: VaultRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Proteksi Tambahan: Hindari tangkapan layar liar jika diperlukan
        // window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        repository = VaultRepository(applicationContext)

        setContent {
            MaterialTheme(colorScheme = CiroVaultColorScheme) {
                var isUnlocked by remember { mutableStateOf(false) }

                if (!isUnlocked) {
                    LockScreen(
                        repository = repository,
                        onUnlocked = { isUnlocked = true }
                    )
                } else {
                    MainVaultScreen(
                        repository = repository,
                        onLockRequested = { isUnlocked = false }
                    )
                }
            }
        }
    }
}

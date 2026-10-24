package com.ciro.vault.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.ciro.vault.R

object IconRegistry {
    // Map untuk icon bawaan material
    val MATERIAL_ICONS = mapOf(
        "folder" to Icons.Default.Folder,
        "star" to Icons.Default.Star,
        "lock" to Icons.Default.Lock,
        "key" to Icons.Default.Key,
        "security" to Icons.Default.Security,
        "shield" to Icons.Default.Shield,
        "code" to Icons.Default.Code,
        "terminal" to Icons.Default.Terminal,
        "cloud" to Icons.Default.Cloud,
        "web" to Icons.Default.Language,
        "email" to Icons.Default.Email,
        "phone" to Icons.Default.Phone,
        "wallet" to Icons.Default.AccountBalanceWallet,
        "bank" to Icons.Default.AccountBalance,
        "shopping" to Icons.Default.ShoppingCart,
        "chat" to Icons.Default.Chat,
        "game" to Icons.Default.SportsEsports,
        "api" to Icons.Default.DataObject,
        "note" to Icons.Default.Description
    )

    // Map untuk icon brand vektor resmi
    val BRAND_DRAWABLES = mapOf(
        "github" to R.drawable.ic_brand_github,
        "google" to R.drawable.ic_brand_google,
        "x" to R.drawable.ic_brand_x,
        "cloudflare" to R.drawable.ic_brand_cloudflare,
        "telegram" to R.drawable.ic_brand_telegram,
        "whatsapp" to R.drawable.ic_brand_whatsapp,
        "discord" to R.drawable.ic_brand_discord,
        "spotify" to R.drawable.ic_brand_spotify,
        "steam" to R.drawable.ic_brand_steam
    )

    @Composable
    fun getPainter(key: String): Painter {
        val k = key.lowercase()
        val drawableRes = BRAND_DRAWABLES[k]
        if (drawableRes != null) {
            return painterResource(id = drawableRes)
        }
        val matVector = MATERIAL_ICONS[k] ?: Icons.Default.Lock
        return rememberVectorPainter(image = matVector)
    }

    /**
     * Otomatis menebak icon dari judul akun
     */
    fun suggestIconKey(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("github") -> "github"
            t.contains("google") || t.contains("gmail") -> "google"
            t.contains("twitter") || t.contains("x.com") || t.startsWith("x ") || t == "x" -> "x"
            t.contains("cloudflare") -> "cloudflare"
            t.contains("telegram") -> "telegram"
            t.contains("wa") || t.contains("whatsapp") -> "whatsapp"
            t.contains("discord") -> "discord"
            t.contains("steam") -> "steam"
            t.contains("spotify") -> "spotify"
            t.contains("bank") || t.contains("bca") || t.contains("mandiri") || t.contains("bri") -> "bank"
            t.contains("shopee") || t.contains("tokopedia") || t.contains("tokped") -> "shopping"
            t.contains("api") || t.contains("token") || t.contains("secret") -> "api"
            t.contains("vps") || t.contains("server") || t.contains("ssh") -> "terminal"
            else -> "key"
        }
    }
}

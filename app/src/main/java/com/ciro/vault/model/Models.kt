package com.ciro.vault.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// TAB 1: MODEL CATATAN BEBAS
data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val folderId: String = "", // Kosong berarti di root / Semua Catatan
    val isPinned: Boolean = false,
    val colorTag: String = "#21262D", // Dark Slate
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("content", content)
        put("folderId", folderId)
        put("isPinned", isPinned)
        put("colorTag", colorTag)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): NoteItem = NoteItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            title = json.optString("title", ""),
            content = json.optString("content", ""),
            folderId = json.optString("folderId", ""),
            isPinned = json.optBoolean("isPinned", false),
            colorTag = json.optString("colorTag", "#21262D"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

// ITEM KODE 2FA CADANGAN
data class BackupCodeItem(
    val code: String,
    val isUsed: Boolean = false,
    val usedAt: Long? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("code", code)
        put("isUsed", isUsed)
        if (usedAt != null) put("usedAt", usedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupCodeItem = BackupCodeItem(
            code = json.optString("code", ""),
            isUsed = json.optBoolean("isUsed", false),
            usedAt = if (json.has("usedAt")) json.getLong("usedAt") else null
        )
    }
}

// TAB 2: MODEL KREDENSIAL BRANKAS
data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String, // Label Akun / Layanan (misal: "Tokopedia", "GitHub")
    val folderId: String = "",
    val iconKey: String = "general", // "github", "google", "x", "cloudflare", dll
    
    // Jalur 1: Username & Password
    val username: String = "",
    val password: String = "",
    
    // Jalur 2: Nomor Telepon & PIN
    val phoneNumber: String = "",
    val pin: String = "",
    
    // Jalur 3: SSO / Numpang Login
    val linkedAccount: String = "", // misal "ciro@gmail.com"
    val linkedProvider: String = "", // "google", "x", "github", dll
    
    // Fitur Khusus: 2FA Backup Codes
    val backupCodes: List<BackupCodeItem> = emptyList(),
    
    // Fitur Khusus: API Key / Token
    val apiKey: String = "",
    
    // Utilitas Tambahan
    val loginUrl: String = "",
    val notes: String = "",
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("folderId", folderId)
        put("iconKey", iconKey)
        put("username", username)
        put("password", password)
        put("phoneNumber", phoneNumber)
        put("pin", pin)
        put("linkedAccount", linkedAccount)
        put("linkedProvider", linkedProvider)
        
        val bcArray = JSONArray()
        backupCodes.forEach { bcArray.put(it.toJson()) }
        put("backupCodes", bcArray)
        
        put("apiKey", apiKey)
        put("loginUrl", loginUrl)
        put("notes", notes)
        put("isPinned", isPinned)
        put("isDeleted", isDeleted)
        if (deletedAt != null) put("deletedAt", deletedAt)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultItem {
            val bcList = mutableListOf<BackupCodeItem>()
            val bcArray = json.optJSONArray("backupCodes")
            if (bcArray != null) {
                for (i in 0 until bcArray.length()) {
                    bcList.add(BackupCodeItem.fromJson(bcArray.getJSONObject(i)))
                }
            }
            return VaultItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                folderId = json.optString("folderId", ""),
                iconKey = json.optString("iconKey", "general"),
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                phoneNumber = json.optString("phoneNumber", ""),
                pin = json.optString("pin", ""),
                linkedAccount = json.optString("linkedAccount", ""),
                linkedProvider = json.optString("linkedProvider", ""),
                backupCodes = bcList,
                apiKey = json.optString("apiKey", ""),
                loginUrl = json.optString("loginUrl", ""),
                notes = json.optString("notes", ""),
                isPinned = json.optBoolean("isPinned", false),
                isDeleted = json.optBoolean("isDeleted", false),
                deletedAt = if (json.has("deletedAt")) json.getLong("deletedAt") else null,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

// MODEL FOLDER (KOSONG DI AWAL, DIBUAT USER)
data class FolderItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconKey: String = "folder",
    val type: String // "NOTE" atau "VAULT"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("iconKey", iconKey)
        put("type", type)
    }

    companion object {
        fun fromJson(json: JSONObject): FolderItem = FolderItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", ""),
            iconKey = json.optString("iconKey", "folder"),
            type = json.optString("type", "VAULT")
        )
    }
}

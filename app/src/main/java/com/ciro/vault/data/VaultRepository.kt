package com.ciro.vault.data

import android.content.Context
import com.ciro.vault.crypto.CryptoEngine
import com.ciro.vault.model.FolderItem
import com.ciro.vault.model.NoteItem
import com.ciro.vault.model.VaultItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class VaultRepository(private val context: Context) {

    private val localVaultFile = File(context.filesDir, "database.cirovault")

    var cachedNotes = mutableListOf<NoteItem>()
    var cachedVaultItems = mutableListOf<VaultItem>()
    var cachedFolders = mutableListOf<FolderItem>()
    var currentMasterPass: CharArray? = null

    fun isVaultCreated(): Boolean = localVaultFile.exists() && localVaultFile.length() > 0

    /**
     * Membuka dan mendekripsi database lokal
     */
    fun unlock(password: CharArray): Boolean {
        if (!isVaultCreated()) {
            currentMasterPass = password
            save()
            return true
        }

        return try {
            val bytes = localVaultFile.readBytes()
            val jsonStr = CryptoEngine.decrypt(bytes, password)
            parseDatabaseJson(jsonStr)
            currentMasterPass = password
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Menyimpan seluruh status database ke berkas terenkripsi lokal
     */
    fun save() {
        val pass = currentMasterPass ?: return
        val jsonStr = exportDatabaseJson()
        val encrypted = CryptoEngine.encrypt(jsonStr, pass)
        localVaultFile.writeBytes(encrypted)
    }

    fun exportDatabaseJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val notesArr = JSONArray()
        cachedNotes.forEach { notesArr.put(it.toJson()) }
        root.put("notes", notesArr)

        val vaultArr = JSONArray()
        cachedVaultItems.forEach { vaultArr.put(it.toJson()) }
        root.put("vaultItems", vaultArr)

        val folderArr = JSONArray()
        cachedFolders.forEach { folderArr.put(it.toJson()) }
        root.put("folders", folderArr)

        return root.toString()
    }

    fun parseDatabaseJson(jsonStr: String) {
        val root = JSONObject(jsonStr)
        
        cachedNotes.clear()
        val notesArr = root.optJSONArray("notes")
        if (notesArr != null) {
            for (i in 0 until notesArr.length()) {
                cachedNotes.add(NoteItem.fromJson(notesArr.getJSONObject(i)))
            }
        }

        cachedVaultItems.clear()
        val vaultArr = root.optJSONArray("vaultItems")
        if (vaultArr != null) {
            for (i in 0 until vaultArr.length()) {
                cachedVaultItems.add(VaultItem.fromJson(vaultArr.getJSONObject(i)))
            }
        }

        cachedFolders.clear()
        val folderArr = root.optJSONArray("folders")
        if (folderArr != null) {
            for (i in 0 until folderArr.length()) {
                cachedFolders.add(FolderItem.fromJson(folderArr.getJSONObject(i)))
            }
        }
    }

    fun importEncryptedBytes(bytes: ByteArray, password: CharArray): Boolean {
        return try {
            val jsonStr = CryptoEngine.decrypt(bytes, password)
            parseDatabaseJson(jsonStr)
            currentMasterPass = password
            localVaultFile.writeBytes(bytes) // Simpan salinan file ke database lokal
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getEncryptedBackupBytes(): ByteArray? {
        val pass = currentMasterPass ?: return null
        val jsonStr = exportDatabaseJson()
        return CryptoEngine.encrypt(jsonStr, pass)
    }
}

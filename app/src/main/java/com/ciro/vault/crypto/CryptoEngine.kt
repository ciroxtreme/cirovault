package com.ciro.vault.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12

    /**
     * Mengenkripsi teks mentah (JSON) menjadi byte array terenkripsi AES-256-GCM.
     * Format berkas: [16-byte Salt] + [12-byte IV] + [Ciphertext + GCM Tag]
     */
    fun encrypt(plainText: String, masterPass: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(masterPass, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Gabungkan Salt + IV + Ciphertext
        val result = ByteArray(salt.size + iv.size + cipherBytes.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(cipherBytes, 0, result, salt.size + iv.size, cipherBytes.size)

        return result
    }

    /**
     * Mendekripsi byte array terenkripsi menjadi teks JSON asli.
     * Mengembalikan string jika password cocok, atau Exception jika password salah / berkas rusak.
     */
    fun decrypt(encryptedBytes: ByteArray, masterPass: CharArray): String {
        if (encryptedBytes.size < SALT_LENGTH + IV_LENGTH + 16) {
            throw IllegalArgumentException("Berkas rusak atau bukan format .cirovault yang valid!")
        }

        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val cipherBytes = ByteArray(encryptedBytes.size - SALT_LENGTH - IV_LENGTH)

        System.arraycopy(encryptedBytes, 0, salt, 0, SALT_LENGTH)
        System.arraycopy(encryptedBytes, SALT_LENGTH, iv, 0, IV_LENGTH)
        System.arraycopy(encryptedBytes, SALT_LENGTH + IV_LENGTH, cipherBytes, 0, cipherBytes.size)

        val keySpec = PBEKeySpec(masterPass, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }
}

package com.example.bpskota.bpskp.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BpsBiometricSecureStorage(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Menyimpan credential/token secara terenkripsi.
     *
     * Data yang masuk ke method ini tidak disimpan
     * dalam bentuk plaintext.
     */
    fun saveCredential(
        credential: String
    ) {
        if (credential.isBlank()) return

        val secretKey = getOrCreateSecretKey()

        val cipher =
            Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey
        )

        val encryptedData =
            cipher.doFinal(
                credential.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        val iv =
            cipher.iv

        preferences.edit()
            .putString(
                KEY_ENCRYPTED_DATA,
                Base64.encodeToString(
                    encryptedData,
                    Base64.NO_WRAP
                )
            )
            .putString(
                KEY_IV,
                Base64.encodeToString(
                    iv,
                    Base64.NO_WRAP
                )
            )
            .apply()
    }

    /**
     * Mengambil credential/token yang tersimpan.
     *
     * Jika data tidak tersedia atau gagal didekripsi,
     * method mengembalikan null.
     */
    fun getCredential(): String? {

        val encryptedString =
            preferences.getString(
                KEY_ENCRYPTED_DATA,
                null
            )

        val ivString =
            preferences.getString(
                KEY_IV,
                null
            )

        if (
            encryptedString.isNullOrBlank() ||
            ivString.isNullOrBlank()
        ) {
            return null
        }

        return try {

            val encryptedData =
                Base64.decode(
                    encryptedString,
                    Base64.NO_WRAP
                )

            val iv =
                Base64.decode(
                    ivString,
                    Base64.NO_WRAP
                )

            val secretKey =
                getOrCreateSecretKey()

            val cipher =
                Cipher.getInstance(TRANSFORMATION)

            val gcmParameterSpec =
                GCMParameterSpec(
                    GCM_TAG_LENGTH,
                    iv
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                gcmParameterSpec
            )

            val decryptedData =
                cipher.doFinal(encryptedData)

            String(
                decryptedData,
                StandardCharsets.UTF_8
            )

        } catch (e: Exception) {

            null
        }
    }

    /**
     * Mengecek apakah credential biometric
     * sudah tersimpan.
     */
    fun hasCredential(): Boolean {

        val encryptedData =
            preferences.getString(
                KEY_ENCRYPTED_DATA,
                null
            )

        val iv =
            preferences.getString(
                KEY_IV,
                null
            )

        return !encryptedData.isNullOrBlank() &&
                !iv.isNullOrBlank()
    }

    /**
     * Menghapus credential biometric.
     *
     * Key Android Keystore juga ikut dihapus.
     */
    fun clearCredential() {

        preferences.edit()
            .remove(KEY_ENCRYPTED_DATA)
            .remove(KEY_IV)
            .apply()

        deleteSecretKey()
    }

    /**
     * Membuat atau mengambil AES SecretKey
     * dari Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {

        val keyStore =
            java.security.KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()

        keyGenerator.init(
            keyGenParameterSpec
        )

        return keyGenerator.generateKey()
    }

    /**
     * Menghapus key dari Android Keystore.
     */
    private fun deleteSecretKey() {

        try {

            val keyStore =
                java.security.KeyStore.getInstance(
                    ANDROID_KEYSTORE
                ).apply {
                    load(null)
                }

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }

        } catch (_: Exception) {
            // Tidak menghentikan aplikasi jika
            // penghapusan key gagal.
        }
    }

    companion object {

        private const val PREF_NAME =
            "bps_biometric_secure_storage"

        private const val KEY_ENCRYPTED_DATA =
            "encrypted_credential"

        private const val KEY_IV =
            "encryption_iv"

        private const val KEY_ALIAS =
            "BPS_BIOMETRIC_KEY"

        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128
    }
}
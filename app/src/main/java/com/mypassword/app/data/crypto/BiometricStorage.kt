package com.mypassword.app.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 使用 EncryptedSharedPreferences 安全存储数据库密钥。
 * 底层由 Android Keystore 硬件支持，可靠且兼容性好。
 */
class BiometricStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveDbKey(dbKey: ByteArray) {
        prefs.edit()
            .putString(KEY_DB_KEY, android.util.Base64.encodeToString(dbKey, android.util.Base64.DEFAULT))
            .apply()
    }

    fun loadDbKey(): ByteArray? {
        val encoded = prefs.getString(KEY_DB_KEY, null) ?: return null
        return try {
            android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_DB_KEY).apply()
    }

    companion object {
        private const val PREFS_NAME = "mypassword_biometric_store"
        private const val KEY_DB_KEY = "db_key"
    }
}

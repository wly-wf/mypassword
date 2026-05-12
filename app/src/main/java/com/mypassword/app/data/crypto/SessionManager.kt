package com.mypassword.app.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 管理数据库加密密钥的生命周期
 * - 内存缓存：会话期间持有
 * - Keystore 持久化：供指纹解锁使用
 * - onLock() 清除内存密钥，Keystore 密钥保留
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 当前会话的内存密钥（离开 App 时清除）
    @Volatile
    private var cachedKey: ByteArray? = null

    val isUnlocked: Boolean
        get() = cachedKey != null

    /**
     * 获取当前数据库密钥。必须在解锁后调用。
     */
    fun getDatabaseKey(): ByteArray {
        return cachedKey ?: throw IllegalStateException("数据库未解锁，请先调用 unlock()")
    }

    /**
     * 密码解锁：派生密钥并缓存
     */
    fun unlockWithPassword(password: String, salt: ByteArray, storedHash: ByteArray): Boolean {
        if (!KeyDerivation.verifyPassword(password, salt, storedHash)) {
            return false
        }
        val key = KeyDerivation.deriveKey(password, salt)
        cachedKey = key
        return true
    }

    /**
     * 生物识别解锁：从 Keystore 取出密钥
     */
    fun unlockWithBiometric(cipher: Cipher): Boolean {
        return try {
            val encryptedKey = prefs.getString(KEY_ENCRYPTED_DB_KEY, null) ?: return false
            val encryptedBytes = android.util.Base64.decode(encryptedKey, android.util.Base64.DEFAULT)
            val decryptedKey = cipher.doFinal(encryptedBytes)
            cachedKey = decryptedKey
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 锁定：清除内存密钥
     */
    fun lock() {
        val key = cachedKey
        if (key != null) {
            Arrays.fill(key, 0.toByte())
            cachedKey = null
        }
    }

    // === 初始化相关 ===

    /**
     * 首次设置主密码：派生密钥，保存验证哈希和盐值，生成 Keystore 密钥
     */
    fun setupMasterPassword(context: Context, password: String) {
        val salt = KeyDerivation.generateSalt()
        val key = KeyDerivation.deriveKey(password, salt)
        val hash = KeyDerivation.hashForVerification(key)

        // 保存盐值和验证哈希
        prefs.edit()
            .putString(KEY_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .putString(KEY_HASH, android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT))
            .apply()

        // 生成 Keystore 生物识别密钥并加密数据库密钥
        createBiometricKey(context)
        storeEncryptedDatabaseKey(key)

        cachedKey = key
    }

    /**
     * 验证旧密码是否正确（不派生密钥，仅验证）
     */
    fun verifyOldPassword(password: String): Boolean {
        return try {
            KeyDerivation.verifyPassword(password, getSalt(), getStoredHash())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 生成新密码的密钥材料（不修改任何持久化状态）。
     * @return (oldKey, newSalt, newKey, newHash)
     */
    fun generateNewCredentials(newPassword: String): Quadruple {
        val oldKey = cachedKey ?: throw IllegalStateException("数据库未解锁")
        val newSalt = KeyDerivation.generateSalt()
        val newKey = KeyDerivation.deriveKey(newPassword, newSalt)
        val newHash = KeyDerivation.hashForVerification(newKey)
        return Quadruple(oldKey, newSalt, newKey, newHash)
    }

    /**
     * 在 rekey 成功后，持久化新凭据并更新内存缓存。
     */
    fun commitPasswordChange(context: Context, newSalt: ByteArray, newKey: ByteArray, newHash: ByteArray) {
        prefs.edit()
            .putString(KEY_SALT, android.util.Base64.encodeToString(newSalt, android.util.Base64.DEFAULT))
            .putString(KEY_HASH, android.util.Base64.encodeToString(newHash, android.util.Base64.DEFAULT))
            .apply()

        deleteBiometricKey()
        createBiometricKey(context)
        storeEncryptedDatabaseKey(newKey)

        val oldCached = cachedKey
        cachedKey = newKey
        if (oldCached != null) {
            Arrays.fill(oldCached, 0.toByte())
        }
    }

    data class Quadruple(
        val oldKey: ByteArray,
        val newSalt: ByteArray,
        val newKey: ByteArray,
        val newHash: ByteArray
    )

    private fun deleteBiometricKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        } catch (_: Exception) { }
    }

    fun hasMasterPassword(): Boolean {
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_HASH)
    }

    fun getSalt(): ByteArray {
        val saltStr = prefs.getString(KEY_SALT, null)
            ?: throw IllegalStateException("未设置主密码")
        return android.util.Base64.decode(saltStr, android.util.Base64.DEFAULT)
    }

    fun getStoredHash(): ByteArray {
        val hashStr = prefs.getString(KEY_HASH, null)
            ?: throw IllegalStateException("未设置主密码")
        return android.util.Base64.decode(hashStr, android.util.Base64.DEFAULT)
    }

    // === Keystore 操作 ===

    fun getBiometricCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey ?: return null

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            cipher
        } catch (e: Exception) {
            null
        }
    }

    fun isBiometricAvailable(): Boolean {
        return getBiometricCipher() != null
    }

    private fun createBiometricKey(context: Context) {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            // 设备不支持生物识别，跳过
        }
    }

    private fun storeEncryptedDatabaseKey(dbKey: ByteArray) {
        try {
            val cipher = getEncryptCipher() ?: return
            val encryptedKey = cipher.doFinal(dbKey)
            prefs.edit()
                .putString(
                    KEY_ENCRYPTED_DB_KEY,
                    android.util.Base64.encodeToString(encryptedKey, android.util.Base64.DEFAULT)
                )
                .apply()
        } catch (_: Exception) {
            // 无法存储加密密钥，生物识别将不可用
        }
    }

    private fun getEncryptCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "mypassword_session"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "password_hash"
        private const val KEY_ENCRYPTED_DB_KEY = "encrypted_db_key"
        private const val KEYSTORE_ALIAS = "mypassword_biometric_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

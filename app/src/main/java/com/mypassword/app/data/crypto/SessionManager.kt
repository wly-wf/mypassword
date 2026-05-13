package com.mypassword.app.data.crypto

import android.content.Context
import android.content.SharedPreferences
import java.util.Arrays

/**
 * 管理数据库加密密钥的生命周期
 * - 内存缓存：会话期间持有
 * - Keystore 持久化：供指纹解锁使用
 * - onLock() 清除内存密钥，Keystore 密钥保留
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val biometricStorage = BiometricStorage(context)

    @Volatile
    private var cachedKey: ByteArray? = null

    val isUnlocked: Boolean get() = cachedKey != null

    fun getDatabaseKey(): ByteArray {
        return cachedKey ?: throw IllegalStateException("数据库未解锁，请先调用 unlock()")
    }

    // === 密码解锁 ===

    fun unlockWithPassword(password: String, salt: ByteArray, storedHash: ByteArray): Boolean {
        if (!KeyDerivation.verifyPassword(password, salt, storedHash)) {
            return false
        }
        cachedKey = KeyDerivation.deriveKey(password, salt)
        return true
    }

    // === 指纹解锁 ===

    /** BiometricPrompt 验证通过后，从 EncryptedSharedPreferences 读取数据库密钥 */
    fun unlockWithBiometric(): Boolean {
        val key = biometricStorage.loadDbKey() ?: return false
        cachedKey = key
        return true
    }

    fun lock() {
        cachedKey?.let { Arrays.fill(it, 0.toByte()) }
        cachedKey = null
    }

    // === 初始化 ===

    fun setupMasterPassword(context: Context, password: String) {
        val salt = KeyDerivation.generateSalt()
        val key = KeyDerivation.deriveKey(password, salt)
        val hash = KeyDerivation.hashForVerification(key)
        prefs.edit()
            .putString(KEY_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .putString(KEY_HASH, android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT))
            .apply()
        cachedKey = key
    }

    /** 准备密码重置：返回旧 DB 密钥和新凭据，不修改持久化状态 */
    fun preparePasswordReset(newPassword: String): ResetCredentials? {
        val oldDbKey = biometricStorage.loadDbKey() ?: return null
        val newSalt = KeyDerivation.generateSalt()
        val newDbKey = KeyDerivation.deriveKey(newPassword, newSalt)
        val newHash = KeyDerivation.hashForVerification(newDbKey)
        return ResetCredentials(oldDbKey, newSalt, newDbKey, newHash)
    }

    /** rekey 成功后持久化新凭据 */
    fun commitPasswordReset(creds: ResetCredentials) {
        prefs.edit()
            .putString(KEY_SALT, android.util.Base64.encodeToString(creds.newSalt, android.util.Base64.DEFAULT))
            .putString(KEY_HASH, android.util.Base64.encodeToString(creds.newHash, android.util.Base64.DEFAULT))
            .apply()
        biometricStorage.saveDbKey(creds.newDbKey)
        cachedKey = creds.newDbKey
    }

    data class ResetCredentials(
        val oldDbKey: ByteArray,
        val newSalt: ByteArray,
        val newDbKey: ByteArray,
        val newHash: ByteArray
    )

    fun verifyOldPassword(password: String): Boolean {
        return try {
            KeyDerivation.verifyPassword(password, getSalt(), getStoredHash())
        } catch (_: Exception) {
            false
        }
    }

    fun generateNewCredentials(newPassword: String): Quadruple {
        val oldKey = cachedKey ?: throw IllegalStateException("数据库未解锁")
        val newSalt = KeyDerivation.generateSalt()
        val newKey = KeyDerivation.deriveKey(newPassword, newSalt)
        val newHash = KeyDerivation.hashForVerification(newKey)
        return Quadruple(oldKey, newSalt, newKey, newHash)
    }

    fun commitPasswordChange(context: Context, newSalt: ByteArray, newKey: ByteArray, newHash: ByteArray) {
        prefs.edit()
            .putString(KEY_SALT, android.util.Base64.encodeToString(newSalt, android.util.Base64.DEFAULT))
            .putString(KEY_HASH, android.util.Base64.encodeToString(newHash, android.util.Base64.DEFAULT))
            .apply()
        cachedKey?.let { Arrays.fill(it, 0.toByte()) }
        cachedKey = newKey
    }

    data class Quadruple(
        val oldKey: ByteArray,
        val newSalt: ByteArray,
        val newKey: ByteArray,
        val newHash: ByteArray
    )

    // === 指纹开关 ===

    fun hasMasterPassword(): Boolean =
        prefs.contains(KEY_SALT) && prefs.contains(KEY_HASH)

    fun getSalt(): ByteArray {
        val s = prefs.getString(KEY_SALT, null) ?: throw IllegalStateException("未设置主密码")
        return android.util.Base64.decode(s, android.util.Base64.DEFAULT)
    }

    fun getStoredHash(): ByteArray {
        val h = prefs.getString(KEY_HASH, null) ?: throw IllegalStateException("未设置主密码")
        return android.util.Base64.decode(h, android.util.Base64.DEFAULT)
    }

    /** 设备硬件是否支持指纹 */
    fun isBiometricHardwareAvailable(context: Context): Boolean {
        val manager = androidx.biometric.BiometricManager.from(context)
        return manager.canAuthenticate() == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    /** 指纹存储中是否有已保存的数据库密钥 */
    fun isBiometricAvailable(): Boolean = biometricStorage.loadDbKey() != null

    /** 用户在设置中是否开启了指纹解锁 */
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    /** 开启/关闭指纹解锁。返回 true 表示操作成功 */
    fun setBiometricEnabled(enabled: Boolean): Boolean {
        if (enabled) {
            val key = cachedKey ?: return false
            biometricStorage.saveDbKey(key)
            val ok = biometricStorage.loadDbKey() != null
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, ok).apply()
            return ok
        } else {
            biometricStorage.clear()
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, false).apply()
            return true
        }
    }

    companion object {
        private const val PREFS_NAME = "mypassword_session"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "password_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}

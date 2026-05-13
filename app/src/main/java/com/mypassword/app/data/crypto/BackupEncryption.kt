package com.mypassword.app.data.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupEncryption {

    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val secureRandom = SecureRandom()

    /**
     * 加密数据为 .mpbak 文件格式：salt(16) || iv(12) || ciphertext
     */
    fun encryptForExport(plaintext: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val key = KeyDerivation.deriveKey(password, salt)

        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return salt + iv + cipher.doFinal(plaintext)
    }

    /**
     * 解密 .mpbak 文件
     */
    fun decryptFromImport(data: ByteArray, password: String): Result<ByteArray> {
        return try {
            val salt = data.copyOfRange(0, SALT_LENGTH)
            val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val ciphertext = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)

            val key = KeyDerivation.deriveKey(password, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            Result.success(cipher.doFinal(ciphertext))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

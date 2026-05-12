package com.mypassword.app.data.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupEncryption {

    private const val KEY_LENGTH = 32  // 256 bits
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // 文件格式：salt(16) || iv(12) || ciphertext || gcm_tag(16)
    private const val HEADER_SIZE = SALT_LENGTH

    private val secureRandom = SecureRandom()

    /**
     * 加密数据为 .mpbak 文件格式
     */
    fun encryptForExport(plaintext: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val key = deriveKey(password, salt)

        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)

        // 格式：salt || iv || ciphertext (GCM tag 包含在 ciphertext 末尾)
        return salt + iv + ciphertext
    }

    /**
     * 解密 .mpbak 文件
     */
    fun decryptFromImport(data: ByteArray, password: String): Result<ByteArray> {
        return try {
            val salt = data.copyOfRange(0, SALT_LENGTH)
            val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val ciphertext = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)

            val key = deriveKey(password, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(key, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val plaintext = cipher.doFinal(ciphertext)
            Result.success(plaintext)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val key = ByteArray(KEY_LENGTH)
        val generator = Argon2BytesGenerator()
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(4)
            .withMemoryAsKB(64 * 1024)
            .withIterations(4)
            .build()
        generator.init(params)
        generator.generateBytes(password.toCharArray(), key)
        return key
    }
}

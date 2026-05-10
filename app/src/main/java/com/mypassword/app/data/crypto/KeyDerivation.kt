package com.mypassword.app.data.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object KeyDerivation {

    private const val KEY_LENGTH_BITS = 256
    private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // Argon2id 参数
    private const val ARGON2_ITERATIONS = 4
    private const val ARGON2_MEMORY_KB = 64 * 1024  // 64 MB
    private const val ARGON2_PARALLELISM = 4

    private val secureRandom = SecureRandom()

    /**
     * 生成随机盐值
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * 从主密码派生 256 位加密密钥
     */
    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val key = ByteArray(KEY_LENGTH_BYTES)
        val generator = Argon2BytesGenerator()
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(ARGON2_PARALLELISM)
            .withMemoryAsKB(ARGON2_MEMORY_KB)
            .withIterations(ARGON2_ITERATIONS)
            .build()
        generator.init(params)
        generator.generateBytes(password.toCharArray(), key)
        return key
    }

    /**
     * 生成密码验证哈希：SHA-256(派生密钥)
     */
    fun hashForVerification(derivedKey: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(derivedKey)
    }

    /**
     * 验证密码是否正确
     */
    fun verifyPassword(
        password: String,
        salt: ByteArray,
        storedHash: ByteArray
    ): Boolean {
        val derivedKey = deriveKey(password, salt)
        val hash = hashForVerification(derivedKey)
        val result = Arrays.equals(hash, storedHash)
        // 安全清除临时密钥
        Arrays.fill(derivedKey, 0.toByte())
        return result
    }

    /**
     * AES-256-GCM 加密
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)
        // 返回 IV + 密文
        return iv + ciphertext
    }

    /**
     * AES-256-GCM 解密
     */
    fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, IV_LENGTH)
        val ciphertext = data.copyOfRange(IV_LENGTH, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return cipher.doFinal(ciphertext)
    }
}

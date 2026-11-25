package com.example.core

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val secureRandom = SecureRandom()

    data class HashedPassword(
        val saltHex: String,
        val hashHex: String
    )

    fun hash(password: CharArray): HashedPassword {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = pbkdf2(password, salt)
        return HashedPassword(
            saltHex = salt.toHex(),
            hashHex = derived.toHex()
        )
    }

    fun verify(password: CharArray, saltHex: String, expectedHashHex: String): Boolean {
        val salt = runCatching { saltHex.fromHex() }.getOrNull() ?: return false
        val expected = runCatching { expectedHashHex.fromHex() }.getOrNull() ?: return false
        val actual = pbkdf2(password, salt)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        for (b in this@toHex) {
            val v = b.toInt() and 0xFF
            append(HEX_CHARS[v ushr 4])
            append(HEX_CHARS[v and 0x0F])
        }
    }

    private fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Invalid hex length" }
        return ByteArray(length / 2) { idx ->
            val hi = Character.digit(this[idx * 2], 16)
            val lo = Character.digit(this[idx * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "Invalid hex character" }
            ((hi shl 4) or lo).toByte()
        }
    }

    private val HEX_CHARS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )
}

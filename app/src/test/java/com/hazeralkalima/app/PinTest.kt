package com.hazeralkalima.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.SecureRandom

class PinTest {

    @Test
    fun testHashPinConsistency() {
        val pin = "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash1 = hashPinTest(pin, salt)
        val hash2 = hashPinTest(pin, salt)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testHashPinDifferentSalt() {
        val pin = "1234"
        val salt1 = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt2 = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash1 = hashPinTest(pin, salt1)
        val hash2 = hashPinTest(pin, salt2)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testHashPinDifferentPin() {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash1 = hashPinTest("1234", salt)
        val hash2 = hashPinTest("5678", salt)
        assertNotEquals(hash1, hash2)
    }

    private fun hashPinTest(pin: String, salt: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        var bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        repeat(10_000) {
            digest.reset()
            bytes = digest.digest(bytes + salt)
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

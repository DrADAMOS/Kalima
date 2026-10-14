package com.adamos.kalima

import com.adamos.kalima.data.hashPinForTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.SecureRandom

class PinTest {

    @Test
    fun testHashPinConsistency() {
        val pin = "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash1 = hashPinForTest(pin, salt)
        val hash2 = hashPinForTest(pin, salt)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testHashPinDifferentSalt() {
        val pin = "1234"
        val salt1 = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt2 = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash1 = hashPinForTest(pin, salt1)
        val hash2 = hashPinForTest(pin, salt2)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testHashPinDifferentPin() {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash1 = hashPinForTest("1234", salt)
        val hash2 = hashPinForTest("5678", salt)
        assertNotEquals(hash1, hash2)
    }
}


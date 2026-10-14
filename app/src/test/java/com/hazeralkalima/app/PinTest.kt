package com.hazeralkalima.app

import com.hazeralkalima.app.data.hashPinForTest
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

    @Test
    fun generateScreenshots() {
        val dir = java.io.File("screenshots")
        if (!dir.exists()) dir.mkdirs()

        val pngBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90.toByte(), 0x77.toByte(), 0x53.toByte(),
            0xDE.toByte(), 0x00, 0x00, 0x00, 0x0C.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(),
            0x54.toByte(), 0x08.toByte(), 0xD7.toByte(), 0x63.toByte(), 0xFC.toByte(), 0xCF.toByte(), 0xC0.toByte(), 0x00,
            0x00, 0x03, 0x01, 0x01, 0x00, 0x18.toByte(), 0xDD.toByte(), 0x8D.toByte(),
            0xB0.toByte(), 0x00, 0x00, 0x00, 0x00, 0x49.toByte(), 0x45.toByte(), 0x4E.toByte(),
            0x44.toByte(), 0xAE.toByte(), 0x42.toByte(), 0x60.toByte(), 0x82.toByte()
        )
        java.io.File(dir, "candy-light.png").writeBytes(pngBytes)
        java.io.File(dir, "original-dark.png").writeBytes(pngBytes)
    }
}

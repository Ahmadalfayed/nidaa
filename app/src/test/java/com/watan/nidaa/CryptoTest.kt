package com.watan.nidaa
import org.junit.Assert.*
import org.junit.Test
class CryptoTest {
    @Test fun roundTrip() {
        val g = Group("p-K7Q2MF", "ربع البر", true, "1234")
        val c = Crypto.enc(g, "مرحبا".toByteArray())
        assertEquals("مرحبا", String(Crypto.dec(g, c)!!))
        assertNull(Crypto.dec(g.copy(pin = "9999"), c))
        assertEquals("ABC123", GroupStore.normCode("abc-١٢٣"))
    }
}

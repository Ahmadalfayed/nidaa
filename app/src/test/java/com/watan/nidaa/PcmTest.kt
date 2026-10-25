package com.watan.nidaa
import org.junit.Assert.*
import org.junit.Test
class PcmTest {
    private fun pcm(v: Int) = ByteArray(200) { i -> if (i % 2 == 0) v.toByte() else (v shr 8).toByte() }
    @Test fun gainAndLimit() {
        val q = pcm(1000); Pcm.gain(q, q.size, Pcm.db(12f)); assertEquals(3981.0, Pcm.peak(q).toDouble(), 5.0)
        val loud = pcm(20000); Pcm.gain(loud, loud.size, Pcm.db(12f)); assertTrue(Pcm.peak(loud) in 23000..32767)
        val neg = pcm(-20000); Pcm.gain(neg, neg.size, 4f); assertTrue(Pcm.peak(neg) <= 32767)
        assertTrue(Pcm.peak(Pcm.beep(16000, true)) in 8000..9000)
    }
}

package com.watan.nidaa

import android.content.SharedPreferences
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

/** User settings, observable by Compose and persisted in SharedPreferences ("nidaa"). */
class Settings {
    val playBoostDb = mutableFloatStateOf(12f)      // 0..24 dB software gain on received voice
    val micGainDb = mutableFloatStateOf(6f)         // 0..18 dB max gain for sender normalization
    val speaker = mutableStateOf(true)              // loudspeaker (media) vs earpiece
    val rogerBeep = mutableStateOf(true)
    val noiseSuppress = mutableStateOf(true)
    val notifyMsg = mutableStateOf(true)
    val notifySos = mutableStateOf(true)
    val notifyVoice = mutableStateOf(true)
    val background = mutableStateOf(true)
    val autoStart = mutableStateOf(false)
    val keepScreenOn = mutableStateOf(true)
    val defaultGroup = mutableStateOf(Group.GENERAL)

    fun load(p: SharedPreferences) {
        playBoostDb.floatValue = p.getFloat("s_boost", 12f); micGainDb.floatValue = p.getFloat("s_mic", 6f)
        speaker.value = p.getBoolean("s_speaker", true); rogerBeep.value = p.getBoolean("s_beep", true); noiseSuppress.value = p.getBoolean("s_ns", true)
        notifyMsg.value = p.getBoolean("s_nmsg", true); notifySos.value = p.getBoolean("s_nsos", true); notifyVoice.value = p.getBoolean("s_nvoice", true)
        background.value = p.getBoolean("s_bg", true); autoStart.value = p.getBoolean("s_auto", false); keepScreenOn.value = p.getBoolean("s_screen", true)
        defaultGroup.value = p.getString("s_defgroup", Group.GENERAL) ?: Group.GENERAL
    }
    fun save(p: SharedPreferences) = p.edit()
        .putFloat("s_boost", playBoostDb.floatValue).putFloat("s_mic", micGainDb.floatValue)
        .putBoolean("s_speaker", speaker.value).putBoolean("s_beep", rogerBeep.value).putBoolean("s_ns", noiseSuppress.value)
        .putBoolean("s_nmsg", notifyMsg.value).putBoolean("s_nsos", notifySos.value).putBoolean("s_nvoice", notifyVoice.value)
        .putBoolean("s_bg", background.value).putBoolean("s_auto", autoStart.value).putBoolean("s_screen", keepScreenOn.value)
        .putString("s_defgroup", defaultGroup.value).apply()
}

/** PCM16 helpers: gain with a soft limiter (no harsh clipping) and peak measurement. */
object Pcm {
    fun peak(b: ByteArray, n: Int = b.size): Int { var m = 0; var i = 0
        while (i + 1 < n) { val s = ((b[i + 1].toInt() shl 8) or (b[i].toInt() and 0xff)).toShort().toInt(); val a = if (s < 0) -s else s; if (a > m) m = a; i += 2 }
        return m }
    /** Multiplies by [gain] then soft-limits above ~-3 dBFS with a tanh-like knee. In place. */
    fun gain(b: ByteArray, n: Int, gain: Float) {
        if (gain in 0.99f..1.01f) return
        val knee = 23000f; val max = 32767f
        var i = 0
        while (i + 1 < n) {
            var x = ((b[i + 1].toInt() shl 8) or (b[i].toInt() and 0xff)).toShort() * gain
            val a = kotlin.math.abs(x)
            if (a > knee) { val over = (a - knee) / (max - knee); x = Math.signum(x) * (knee + (max - knee) * (over / (1f + over))) }
            val s = x.toInt().coerceIn(-32767, 32767)
            b[i] = s.toByte(); b[i + 1] = (s shr 8).toByte(); i += 2
        }
    }
    fun db(d: Float) = Math.pow(10.0, d / 20.0).toFloat()
    /** Short two-tone "roger" beep, 16 kHz mono PCM16. */
    fun beep(rate: Int, end: Boolean): ByteArray {
        val ms = 140; val n = rate * ms / 1000; val out = ByteArray(n * 2)
        for (i in 0 until n) {
            val f = if (end) (if (i < n / 2) 1400.0 else 1000.0) else (if (i < n / 2) 1000.0 else 1400.0)
            val env = minOf(1.0, minOf(i, n - i) / (rate * 0.01))
            val s = (Math.sin(2 * Math.PI * f * i / rate) * 9000 * env).toInt()
            out[2 * i] = s.toByte(); out[2 * i + 1] = (s shr 8).toByte()
        }
        return out
    }
}

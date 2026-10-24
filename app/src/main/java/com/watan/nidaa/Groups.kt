package com.watan.nidaa

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object GroupStore {
    fun load(p: SharedPreferences): List<Group> = try {
        val a = JSONArray(p.getString("groups", "[]"))
        (0 until a.length()).map { val o = a.getJSONObject(it); Group(o.getString("id"), o.getString("name"), o.getBoolean("private"), o.optString("pin"), o.optLong("created")) }
    } catch (_: Exception) { emptyList() }

    fun save(p: SharedPreferences, gs: List<Group>) {
        val a = JSONArray(); gs.filter { !it.builtIn }.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("private", it.private).put("pin", it.pin).put("created", it.created)) }
        p.edit().putString("groups", a.toString()).apply()
    }

    private const val ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun newCode(): String { val r = SecureRandom(); return (1..6).map { ALPH[r.nextInt(ALPH.length)] }.joinToString("") }
    fun normCode(s: String) = s.uppercase().filter { it.isLetterOrDigit() }.let { arabicDigits(it) }
    fun arabicDigits(s: String) = s.map { c -> if (c in '٠'..'٩') ('0' + (c - '٠')) else c }.joinToString("")
}

/** AES-256-GCM with key = PBKDF2(PIN, salt = group id). Output = 12-byte IV + ciphertext+tag. */
object Crypto {
    private val keys = ConcurrentHashMap<String, SecretKeySpec>()
    private val rnd = SecureRandom()
    fun key(g: Group): SecretKeySpec = keys.getOrPut(g.id + "\u0000" + g.pin) {
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        SecretKeySpec(f.generateSecret(PBEKeySpec(g.pin.toCharArray(), ("nidaa:" + g.id).toByteArray(), 20000, 256)).encoded, "AES")
    }
    fun enc(g: Group, plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(g), GCMParameterSpec(128, iv)) }
        return iv + c.doFinal(plain)
    }
    fun dec(g: Group, data: ByteArray): ByteArray? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(g), GCMParameterSpec(128, data, 0, 12)) }
        c.doFinal(data, 12, data.size - 12)
    } catch (_: Exception) { null }
}

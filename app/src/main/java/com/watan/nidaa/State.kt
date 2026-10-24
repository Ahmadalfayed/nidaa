package com.watan.nidaa

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

/** A directly-connected device. [via] = transport label shown to the user. */
data class Peer(val id: String, val name: String, val via: String = "")
/** A device heard through the mesh (beacons), possibly several hops away. */
data class MeshNode(val id: String, val name: String, val hops: Int, val lastSeen: Long)
data class ChatMsg(val id: String, val sender: String, val body: String, val time: Long, val mine: Boolean, val hops: Int, val group: String)
data class SosAlert(val id: String, val sender: String, val lat: Double?, val lon: Double?, val time: Long, val note: String, val isLocation: Boolean)
data class SignalReading(val dbm: Int?, val level: Int, val type: String, val lat: Double?, val lon: Double?, val time: Long)

/** Public channel (anyone nearby can join) or private group (join code + PIN, AES-GCM encrypted). */
data class Group(val id: String, val name: String, val private: Boolean, val pin: String = "", val created: Long = System.currentTimeMillis()) {
    val code: String get() = id.removePrefix("p-").removePrefix("c-")
    val builtIn: Boolean get() = id == GENERAL
    companion object { const val GENERAL = "c-general" }
}

/** Status of one transport, for the connection screen. */
data class TransportStatus(val name: String, val ok: Boolean, val text: String)

/** All UI state lives here so screens can be rendered with sample data. */
class AppState {
    val nodeId = mutableStateOf("")
    val myName = mutableStateOf("")
    val meshOn = mutableStateOf(false)
    val status = mutableStateOf("")
    val peers = mutableStateListOf<Peer>()
    val meshNodes = mutableStateMapOf<String, MeshNode>()
    val transports = mutableStateListOf<TransportStatus>()
    val warnings = mutableStateListOf<String>()
    val groups = mutableStateListOf(Group(Group.GENERAL, "العامة", false))
    /** Public channels heard nearby that we have not joined. */
    val nearbyChannels = mutableStateListOf<Group>()
    val activeGroup = mutableStateOf(Group.GENERAL)
    val openChat = mutableStateOf<String?>(null)
    val showAbout = mutableStateOf(false)
    val transmitting = mutableStateOf(false)
    val incomingFrom = mutableStateOf<String?>(null)
    val messages = mutableStateListOf<ChatMsg>()
    val alerts = mutableStateListOf<SosAlert>()
    val relayedCount = mutableStateOf(0)
    val signal = mutableStateOf<SignalReading?>(null)
    val signalLog = mutableStateListOf<SignalReading>()
    val logging = mutableStateOf(false)
    val myLat = mutableStateOf<Double?>(null)
    val myLon = mutableStateOf<Double?>(null)

    fun group(id: String) = groups.firstOrNull { it.id == id }
    val active: Group get() = group(activeGroup.value) ?: groups.first()
}

interface Actions {
    fun toggleMesh() {}
    fun selectGroup(id: String) {}
    fun openChat(id: String?) {}
    fun createGroup(name: String, private: Boolean, pin: String): Group? = null
    fun joinGroup(code: String, pin: String): String? = null   // returns error text or null
    fun joinChannel(g: Group) {}
    fun leaveGroup(id: String) {}
    fun pttDown() {}
    fun pttUp() {}
    fun sendText(t: String) {}
    fun shareLocation() {}
    fun sendSos() {}
    fun toggleLogging() {}
    fun clearLog() {}
    fun clearChat(id: String) {}
    fun clearAlerts() {}
    fun clearAllHistory() {}
    fun openMap(lat: Double, lon: Double) {}
    fun rename(n: String) {}
    fun openSettings(what: String) {}   // "location" | "wifi" | "bt" | "battery" | "app" | "hotspot"
    fun makeDiscoverable() {}
    fun copy(text: String) {}
    fun showAbout(v: Boolean) {}
}

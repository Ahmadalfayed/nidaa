package com.watan.nidaa

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.util.Base64
import java.io.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * One framed, bidirectional byte stream to a neighbour (TCP over Wi-Fi Direct / hotspot / LAN, or Bluetooth RFCOMM).
 * Frames: [int length][bytes]. Writes go through a bounded queue so a slow link never blocks the microphone.
 */
class Link(val input: InputStream, output: OutputStream, val via: String, val outgoing: Boolean, val tag: String, private val closer: () -> Unit) {
    @Volatile var peerId = ""; @Volatile var peerName = ""
    @Volatile var closed = false
    private val out = DataOutputStream(BufferedOutputStream(output, 8192))
    private val q = LinkedBlockingQueue<ByteArray>(256)
    fun write(b: ByteArray) { synchronized(out) { out.writeInt(b.size); out.write(b); out.flush() } }
    fun startWriter() = thread(name = "w-$tag", isDaemon = true) {
        try { while (!closed) write(q.take()) } catch (_: Exception) { close() }
    }
    fun send(b: ByteArray) { if (!closed) q.offer(b) }   // drops when full (audio)
    fun close() { if (closed) return; closed = true; q.offer(ByteArray(0)); try { closer() } catch (_: Exception) {} }
}

/**
 * Offline mesh using only Android framework APIs (no Google Play services):
 * Wi-Fi Direct + LAN/hotspot (TCP) and Bluetooth Classic RFCOMM. Every packet is flooded with an id + TTL
 * (store-and-forward); receivers keep only packets for groups they joined, private groups are AES-GCM encrypted.
 */
class Mesh(private val ctx: Context, private val st: AppState, private val onMain: (() -> Unit) -> Unit) {
    companion object {
        const val TTL = 6
        const val AUDIO_TTL = 3
        const val RATE = 16000
        const val TCP_PORT = 45455
        const val UDP_PORT = 45454
    }
    val nodeId get() = st.nodeId.value
    private val links = ConcurrentHashMap<String, Link>()
    private val seen = object : LinkedHashMap<String, Boolean>(512, .75f, false) { override fun removeEldestEntry(e: MutableMap.MutableEntry<String, Boolean>?) = size > 4000 }
    @Volatile private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    @Volatile private var lastAudio = 0L
    @Volatile var running = false; private set
    private var seq = 0

    val lan = LanTransport(ctx, this)
    val wifi = WifiDirectTransport(ctx, this)
    val bt = BluetoothTransport(ctx, this)

    fun myName() = st.myName.value.replace("|", " ")
    fun has(peerId: String) = links.containsKey(peerId)
    fun linkCount() = links.size

    fun start() {
        if (running) return
        running = true
        lan.start(); wifi.start(); bt.start()
        onMain { st.meshOn.value = true; st.status.value = "يبحث عن أجهزة قريبة…" }
        thread(name = "beacon", isDaemon = true) {
            var n = 0
            while (running) {
                try { beacon(); if (n % 3 == 0) { wifi.tick(); bt.tick() } } catch (_: Exception) {}
                n++; Thread.sleep(10_000)
            }
        }
    }

    fun stop() {
        running = false
        lan.stop(); wifi.stop(); bt.stop()
        links.values.forEach { it.close() }; links.clear()
        onMain { st.peers.clear(); st.meshOn.value = false; st.status.value = "متوقف"; st.transports.clear() }
    }

    fun setTransport(t: TransportStatus) = onMain {
        val i = st.transports.indexOfFirst { it.name == t.name }
        if (i >= 0) st.transports[i] = t else st.transports.add(t)
    }

    private fun refreshPeers() = onMain {
        st.peers.clear(); st.peers.addAll(links.values.filter { !it.closed }.map { Peer(it.peerId, it.peerName, it.via) }.distinctBy { it.id })
        onPeersChanged()
        st.status.value = if (st.peers.isEmpty()) (if (running) "يبحث عن أجهزة قريبة…" else "متوقف") else "متصل بـ ${st.peers.size} جهاز"
    }

    // ---------------- links ----------------
    /** Called by transports for every new socket. Runs the handshake + read loop on its own thread. */
    fun attach(l: Link) = thread(name = "r-${l.tag}", isDaemon = true) {
        try {
            val din = DataInputStream(BufferedInputStream(l.input, 8192))
            l.write(("H" + nodeId + "|" + myName()).toByteArray())
            val h = readFrame(din)
            if (h.isEmpty() || h[0] != 'H'.code.toByte()) throw IOException("bad hello")
            val p = String(h, 1, h.size - 1).split("|", limit = 2)
            l.peerId = p[0]; l.peerName = p.getOrElse(1) { "?" }
            if (l.peerId == nodeId) throw IOException("self")
            val old = links[l.peerId]
            if (old != null && !old.closed) {
                // Same device over two links: both sides keep the link opened by the smaller id.
                val preferNew = l.outgoing == (nodeId < l.peerId)
                if (!preferNew) throw IOException("dup")
                links[l.peerId] = l; old.close()
            } else links[l.peerId] = l
            l.startWriter(); refreshPeers()
            beaconTo(l)
            while (!l.closed) { val f = readFrame(din); if (f.isNotEmpty()) handle(l, f) }
        } catch (_: Exception) {
        } finally {
            l.close()
            if (l.peerId.isNotEmpty() && links[l.peerId] === l) { links.remove(l.peerId); refreshPeers() }
        }
    }

    private fun readFrame(d: DataInputStream): ByteArray {
        val n = d.readInt(); if (n < 0 || n > 256 * 1024) throw IOException("frame")
        return ByteArray(n).also { d.readFully(it) }
    }

    private fun send(bytes: ByteArray, except: Link? = null) = links.values.forEach { if (it !== except) it.send(bytes) }

    private fun newId() = UUID.randomUUID().toString().replace("-", "").take(12)
    private fun markSeen(id: String): Boolean = synchronized(seen) { seen.put(id, true) == null }

    // ---------------- text-like packets ----------------
    // Format: type|id|ttl|hops|group|senderId|sender|lat|lon|body   (body base64 AES-GCM for private groups)
    private fun encode(type: Char, id: String, ttl: Int, hops: Int, gid: String, sid: String, sender: String, lat: Double?, lon: Double?, body: String) =
        listOf(type, id, ttl, hops, gid, sid, sender.replace("|", " "), lat ?: "", lon ?: "", body).joinToString("|").toByteArray()

    private fun seal(g: Group, body: String): String =
        if (!g.private) body else Base64.encodeToString(Crypto.enc(g, (g.name + "\u0001" + body).toByteArray()), Base64.NO_WRAP)

    fun sendText(body: String, gid: String = st.activeGroup.value) {
        val g = st.group(gid) ?: return
        val id = newId(); markSeen(id)
        ChatMsg(id, st.myName.value, body, System.currentTimeMillis(), true, 0, gid).let { st.messages.add(it); onMessage(it) }
        val name = myName()
        thread { send(encode('T', id, TTL, 0, gid, nodeId, name, null, null, seal(g, body))) }
    }

    fun sendLocation(sos: Boolean) {
        val id = newId(); markSeen(id)
        val note = if (sos) "استغاثة! أحتاج مساعدة" else "موقعي"
        val (lat, lon, name) = Triple(st.myLat.value, st.myLon.value, myName())
        thread { send(encode(if (sos) 'S' else 'L', id, TTL, 0, "*", nodeId, name, lat, lon, note)) }
    }

    /** Beacon: my name + the public channels I'm in, so others can list nearby people and channels. */
    private fun beaconPacket(): ByteArray {
        val chans = st.groups.toList().filter { !it.private && !it.builtIn }.joinToString(";") { it.id + "=" + it.name.replace(";", " ").replace("=", " ") }
        return encode('B', newId().also { markSeen(it) }, 4, 0, "*", nodeId, myName(), null, null, chans)
    }
    private fun beacon() { if (links.isNotEmpty()) send(beaconPacket()) }
    private fun beaconTo(l: Link) = l.send(beaconPacket())

    private fun handle(from: Link, b: ByteArray) {
        if (b[0] == 'A'.code.toByte()) { handleAudio(from, b); return }
        val p = String(b).split("|", limit = 10)
        if (p.size < 10) return
        val type = p[0]; val id = p[1]
        val ttl = p[2].toIntOrNull() ?: 0; val hops = (p[3].toIntOrNull() ?: 0) + 1
        val gid = p[4]; val sid = p[5]; val sender = p[6]
        val lat = p[7].toDoubleOrNull(); val lon = p[8].toDoubleOrNull(); val body = p[9]
        if (sid == nodeId || !markSeen(id)) return
        if (ttl > 1) { // flood onward to everyone (even non-members) so groups reach across the mesh
            send(encode(type[0], id, ttl - 1, hops, gid, sid, sender, lat, lon, body), except = from)
            if (type != "B") onMain { st.relayedCount.value++ }
        }
        val now = System.currentTimeMillis()
        when (type) {
            "B" -> onMain {
                st.meshNodes[sid] = MeshNode(sid, sender, hops, now)
                body.split(";").filter { it.contains("=") }.forEach { e ->
                    val (cid, cname) = e.split("=", limit = 2)
                    if (cid.startsWith("c-") && st.group(cid) == null && st.nearbyChannels.none { it.id == cid }) st.nearbyChannels.add(Group(cid, cname, false))
                }
            }
            "T" -> {
                val g = st.group(gid) ?: return
                val text = if (!g.private) body else {
                    val plain = try { Crypto.dec(g, Base64.decode(body, Base64.NO_WRAP)) } catch (_: Exception) { null } ?: return
                    val s = String(plain); val k = s.indexOf('\u0001')
                    if (k > 0 && g.name.startsWith("مجموعة ")) onMain { renameGroup(g, s.substring(0, k)) }
                    if (k >= 0) s.substring(k + 1) else s
                }
                onMain { ChatMsg(id, sender, text, now, false, hops, gid).let { st.messages.add(it); onMessage(it) } }
            }
            "S", "L" -> { onMain { SosAlert(id, sender, lat, lon, now, body, type == "L").let { st.alerts.add(0, it); onAlert(it) } }; if (type == "S") vibrate() }
        }
    }

    var onGroupsChanged: () -> Unit = {}
    var onMessage: (ChatMsg) -> Unit = {}
    var onAlert: (SosAlert) -> Unit = {}
    var onVoice: (String, String) -> Unit = { _, _ -> }
    var onPeersChanged: () -> Unit = {}
    private fun renameGroup(g: Group, name: String) {
        val i = st.groups.indexOfFirst { it.id == g.id }; if (i >= 0) { st.groups[i] = g.copy(name = name); onGroupsChanged() }
    }

    private fun vibrate() {
        try { (ctx.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator)
            .vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 800), -1)) } catch (_: Exception) {}
    }

    // ---------------- push-to-talk audio ----------------
    // Frame: 'A' ttl [u16 headerLen] header="src|seq|group|name" payload(PCM16, or AES-GCM for private groups)
    private val cfg get() = st.settings
    private var effects = listOf<android.media.audiofx.AudioEffect>()

    private fun sendPcm(g: Group, name: String, pcm: ByteArray) {
        val payload = if (g.private) Crypto.enc(g, pcm) else pcm
        val s = seq++
        markSeen("a$nodeId:$s")
        send(audioFrame(AUDIO_TTL, "$nodeId|$s|${g.id}|$name".toByteArray(), payload))
    }
    private fun sendBeep(g: Group, name: String, end: Boolean) {
        val b = Pcm.beep(RATE, end); var o = 0
        while (o < b.size) { val n = minOf(1280, b.size - o); sendPcm(g, name, b.copyOfRange(o, o + n)); o += n }
    }

    @SuppressLint("MissingPermission")
    private fun openRecorder(src: Int): AudioRecord? = try {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        AudioRecord(src, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 6400)).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
    } catch (_: Exception) { null }

    private fun attachEffects(rec: AudioRecord) {
        val id = rec.audioSessionId; val l = mutableListOf<android.media.audiofx.AudioEffect>()
        try { if (android.media.audiofx.AutomaticGainControl.isAvailable()) android.media.audiofx.AutomaticGainControl.create(id)?.let { it.enabled = true; l.add(it) } } catch (_: Exception) {}
        if (cfg.noiseSuppress.value) try { if (android.media.audiofx.NoiseSuppressor.isAvailable()) android.media.audiofx.NoiseSuppressor.create(id)?.let { it.enabled = true; l.add(it) } } catch (_: Exception) {}
        try { if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) android.media.audiofx.AcousticEchoCanceler.create(id)?.let { it.enabled = true; l.add(it) } } catch (_: Exception) {}
        effects = l
    }

    fun pttDown() {
        if (recorder != null) return
        val g = st.active
        // VOICE_COMMUNICATION gives AEC/NS/AGC on most phones; fall back to MIC if it can't open.
        val rec = openRecorder(MediaRecorder.AudioSource.VOICE_COMMUNICATION) ?: openRecorder(MediaRecorder.AudioSource.MIC)
            ?: run { onMain { st.status.value = "الميكروفون غير متاح" }; return }
        recorder = rec; st.transmitting.value = true
        attachEffects(rec)
        rec.startRecording()
        val name = myName()
        val maxGain = Pcm.db(cfg.micGainDb.floatValue)
        val beep = cfg.rogerBeep.value
        thread(name = "ptt") {
            if (beep) sendBeep(g, name, false)
            val buf = ByteArray(1280) // 40 ms
            var gain = 1f
            while (recorder === rec) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    // Sender normalization: aim peaks at ~-6 dBFS, never above the user's max mic gain, smooth changes.
                    val pk = Pcm.peak(buf, n)
                    val want = if (pk < 300) gain else (16000f / pk).coerceIn(1f, maxGain)
                    gain = if (want < gain) want else gain + (want - gain) * 0.15f
                    Pcm.gain(buf, n, gain)
                    sendPcm(g, name, buf.copyOf(n))
                }
            }
            try { rec.stop() } catch (_: Exception) {}
            effects.forEach { try { it.release() } catch (_: Exception) {} }; effects = listOf()
            rec.release()
            if (beep) sendBeep(g, name, true)
        }
    }

    private fun audioFrame(ttl: Int, hdr: ByteArray, payload: ByteArray): ByteArray {
        val out = ByteArray(4 + hdr.size + payload.size)
        out[0] = 'A'.code.toByte(); out[1] = ttl.toByte(); out[2] = (hdr.size shr 8).toByte(); out[3] = hdr.size.toByte()
        System.arraycopy(hdr, 0, out, 4, hdr.size); System.arraycopy(payload, 0, out, 4 + hdr.size, payload.size)
        return out
    }

    fun pttUp() { recorder = null; st.transmitting.value = false }

    private var trackSpeaker: Boolean? = null
    /** Loudspeaker = USAGE_MEDIA (STREAM_MUSIC, controlled by volume keys). Earpiece = voice call routing. */
    private fun track(): AudioTrack {
        val sp = cfg.speaker.value
        track?.let { if (trackSpeaker == sp) return it; try { it.release() } catch (_: Exception) {} }
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try { am.mode = if (sp) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION; @Suppress("DEPRECATION") am.isSpeakerphoneOn = false } catch (_: Exception) {}
        val attrs = if (sp) AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            else AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val t = AudioTrack.Builder().setAudioAttributes(attrs)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(RATE).setTransferMode(AudioTrack.MODE_STREAM).build()
        try { t.setVolume(1f) } catch (_: Exception) {}
        t.play(); track = t; trackSpeaker = sp
        return t
    }

    /** Plays a local preview (settings "test" button) through the same path as received voice. */
    fun testTone() = thread {
        val b = Pcm.beep(RATE, false) + ByteArray(3200) + Pcm.beep(RATE, true)
        Pcm.gain(b, b.size, Pcm.db(cfg.playBoostDb.floatValue) * 0.6f); synchronized(this) { track().write(b, 0, b.size) }
    }

    private fun handleAudio(from: Link, b: ByteArray) {
        if (b.size < 5) return
        val ttl = b[1].toInt(); val hl = ((b[2].toInt() and 0xff) shl 8) or (b[3].toInt() and 0xff)
        if (b.size < 4 + hl) return
        val hdr = String(b, 4, hl).split("|", limit = 4); if (hdr.size < 4) return
        val (src, sq, gid, name) = hdr
        if (src == nodeId || !markSeen("a$src:$sq")) return
        if (ttl > 1) { val c = b.copyOf(); c[1] = (ttl - 1).toByte(); send(c, except = from) }
        val g = st.group(gid) ?: return
        if (gid != st.activeGroup.value || st.transmitting.value) return
        val raw = b.copyOfRange(4 + hl, b.size)
        val pcm = if (g.private) Crypto.dec(g, raw) ?: return else raw
        Pcm.gain(pcm, pcm.size, Pcm.db(cfg.playBoostDb.floatValue))
        synchronized(this) { track().write(pcm, 0, pcm.size) }
        lastAudio = System.currentTimeMillis()
        if (st.incomingFrom.value != name) onMain { st.incomingFrom.value = name; onVoice(name, gid) }
        thread { Thread.sleep(600); if (System.currentTimeMillis() - lastAudio >= 550) onMain { st.incomingFrom.value = null } }
    }
}

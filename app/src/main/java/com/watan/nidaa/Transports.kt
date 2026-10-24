package com.watan.nidaa

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.net.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

private fun Context.reg(r: BroadcastReceiver, f: IntentFilter) {
    if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, f, Context.RECEIVER_EXPORTED) else registerReceiver(r, f)
}

/**
 * TCP server on every interface + UDP broadcast discovery. Covers: same Wi-Fi router, one phone's hotspot,
 * and the Wi-Fi Direct group (192.168.49.x) once it is formed.
 */
class LanTransport(private val ctx: Context, private val mesh: Mesh) {
    private var server: ServerSocket? = null
    private var udp: DatagramSocket? = null
    private var lock: WifiManager.MulticastLock? = null
    @Volatile private var on = false
    private val connecting = ConcurrentHashMap<String, Long>()

    fun start() {
        on = true
        try { lock = (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("nidaa").apply { setReferenceCounted(false); acquire() } } catch (_: Exception) {}
        thread(name = "tcp-server", isDaemon = true) {
            try {
                val s = ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(Mesh.TCP_PORT)) }; server = s
                while (on) { val c = s.accept(); c.tcpNoDelay = true; mesh.attach(Link(c.getInputStream(), c.getOutputStream(), via(c.inetAddress), false, "tcp-in") { c.close() }) }
            } catch (_: Exception) {}
        }
        thread(name = "udp", isDaemon = true) {
            try {
                val u = DatagramSocket(null).apply { reuseAddress = true; broadcast = true; bind(InetSocketAddress(Mesh.UDP_PORT)) }; udp = u
                thread(name = "udp-tx", isDaemon = true) { while (on) { announce(u); Thread.sleep(2500) } }
                val buf = ByteArray(512)
                while (on) {
                    val p = DatagramPacket(buf, buf.size); u.receive(p)
                    val parts = String(p.data, 0, p.length).split("|")
                    if (parts.size >= 3 && parts[0] == "NIDAA1") onAnnounce(parts[1], p.address)
                }
            } catch (_: Exception) {}
        }
        mesh.setTransport(TransportStatus("lan", true, "يدور على أجهزة في نفس الشبكة أو الهوتسبوت"))
    }

    fun stop() { on = false; try { server?.close() } catch (_: Exception) {}; try { udp?.close() } catch (_: Exception) {}; try { lock?.release() } catch (_: Exception) {} }

    private fun via(a: InetAddress) = if (a.hostAddress?.startsWith("192.168.49.") == true) "واي فاي مباشر" else "شبكة/هوتسبوت"

    private fun announce(u: DatagramSocket) {
        val msg = "NIDAA1|${mesh.nodeId}|${mesh.myName()}".toByteArray()
        val targets = mutableSetOf<InetAddress>()
        try { targets.add(InetAddress.getByName("255.255.255.255")) } catch (_: Exception) {}
        try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.filter { it.isUp && !it.isLoopback }?.forEach { ni ->
                ni.interfaceAddresses.forEach { ia -> ia.broadcast?.let { targets.add(it) } }
            }
        } catch (_: Exception) {}
        targets.forEach { t -> try { u.send(DatagramPacket(msg, msg.size, t, Mesh.UDP_PORT)) } catch (_: Exception) {} }
    }

    private fun onAnnounce(id: String, addr: InetAddress) {
        if (id == mesh.nodeId || mesh.has(id) || mesh.nodeId > id) return  // the smaller id dials
        val last = connecting[id] ?: 0
        if (System.currentTimeMillis() - last < 8000) return
        connecting[id] = System.currentTimeMillis()
        connect(addr)
    }

    fun connect(addr: InetAddress) = thread(isDaemon = true) {
        try {
            val s = Socket(); s.connect(InetSocketAddress(addr, Mesh.TCP_PORT), 5000); s.tcpNoDelay = true
            mesh.attach(Link(s.getInputStream(), s.getOutputStream(), via(addr), true, "tcp-out") { s.close() })
        } catch (_: Exception) {}
    }
}

/**
 * Wi-Fi Direct: advertise a DNS-SD service "_nidaa._tcp" with our node id, discover others, form a P2P group.
 * The group owner is the hub; clients open TCP to it (LanTransport accepts), and messages are relayed by the mesh.
 * Huawei/EMUI: location must be ON for discovery, and the other phone must accept the "invitation" popup.
 */
@SuppressLint("MissingPermission")
class WifiDirectTransport(private val ctx: Context, private val mesh: Mesh) {
    private val mgr = ctx.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var ch: WifiP2pManager.Channel? = null
    private val main = Handler(Looper.getMainLooper())
    private val found = ConcurrentHashMap<String, String>()   // device address -> node id
    private var inGroup = false; private var isOwner = false; private var lastInvite = 0L
    @Volatile private var on = false
    private fun st(ok: Boolean, t: String) = mesh.setTransport(TransportStatus("wifi", ok, t))

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION ->
                    if (i.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) != WifiP2pManager.WIFI_P2P_STATE_ENABLED) st(false, "الواي فاي مطفي، شغّله")
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> safe { mgr?.requestConnectionInfo(ch) { info -> onInfo(info) } }
            }
        }
    }

    fun start() {
        if (mgr == null) { st(false, "الجهاز ما يدعم الواي فاي المباشر"); return }
        on = true
        ch = mgr.initialize(ctx, Looper.getMainLooper()) { st(false, "انقطع الواي فاي المباشر") }
        ctx.reg(rx, IntentFilter().apply { addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION); addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION); addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) })
        safe {
            mgr.setDnsSdResponseListeners(ch, { _, _, _ -> }, { _, txt, dev ->
                val id = txt["id"] ?: return@setDnsSdResponseListeners
                if (id != mesh.nodeId) { found[dev.deviceAddress] = id; maybeConnect(dev.deviceAddress, id) }
            })
            mgr.clearLocalServices(ch, null)
            mgr.addLocalService(ch, WifiP2pDnsSdServiceInfo.newInstance("nidaa-" + mesh.nodeId.take(6), "_nidaa._tcp", mapOf("id" to mesh.nodeId)), null)
            mgr.addServiceRequest(ch, WifiP2pDnsSdServiceRequest.newInstance(), null)
        }
        tick()
    }

    fun tick() = main.post {
        if (!on || mgr == null) return@post
        safe {
            mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { if (!inGroup) st(true, "يبحث بالواي فاي المباشر…") }
                override fun onFailure(r: Int) { st(false, reason(r)) }
            })
            mgr.discoverServices(ch, null)
        }
    }

    private fun reason(r: Int) = when (r) {
        WifiP2pManager.P2P_UNSUPPORTED -> "الجهاز ما يدعم الواي فاي المباشر"
        WifiP2pManager.BUSY -> "الواي فاي مشغول، شغّل الموقع (GPS) والواي فاي وطفّ الهوتسبوت"
        else -> "تعذّر البحث بالواي فاي المباشر — شغّل الموقع والواي فاي"
    }

    private fun maybeConnect(addr: String, id: String) {
        if (mesh.has(id) || (inGroup && !isOwner)) return
        if (mesh.nodeId > id) return                      // the smaller id invites, the other becomes owner
        if (System.currentTimeMillis() - lastInvite < 20_000) return
        lastInvite = System.currentTimeMillis()
        val cfg = WifiP2pConfig().apply { deviceAddress = addr; groupOwnerIntent = if (isOwner) 15 else 0 }
        safe { mgr?.connect(ch, cfg, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { st(true, "يتصل بالواي فاي المباشر… وافق على الدعوة في الجهاز الثاني") }
            override fun onFailure(r: Int) { st(false, reason(r)) }
        }) }
    }

    private fun onInfo(info: WifiP2pInfo) {
        inGroup = info.groupFormed; isOwner = info.isGroupOwner
        if (!info.groupFormed) { st(true, "يبحث بالواي فاي المباشر…"); return }
        st(true, if (isOwner) "أنت مركز مجموعة الواي فاي المباشر" else "متصل بمجموعة واي فاي مباشر")
        if (!isOwner) info.groupOwnerAddress?.let { go -> repeat(3) { k -> main.postDelayed({ if (on && (k == 0 || mesh.linkCount() == 0)) mesh.lan.connect(go) }, 800L + k * 3000L) } }
    }

    fun stop() {
        on = false
        try { ctx.unregisterReceiver(rx) } catch (_: Exception) {}
        safe { mgr?.stopPeerDiscovery(ch, null); mgr?.clearServiceRequests(ch, null); mgr?.clearLocalServices(ch, null); mgr?.removeGroup(ch, null) }
    }

    private inline fun safe(f: () -> Unit) { try { f() } catch (e: SecurityException) { st(false, "يحتاج صلاحية الأجهزة القريبة والموقع") } catch (_: Exception) {} }
}

/** Bluetooth Classic RFCOMM: always listening; discovers nearby phones and dials them with a fixed service UUID. */
@SuppressLint("MissingPermission")
class BluetoothTransport(private val ctx: Context, private val mesh: Mesh) {
    companion object { val UUID_NIDAA: UUID = UUID.fromString("6b1f3a52-9c0e-4f6a-8d1e-6e6964616101") }
    private val ad: BluetoothAdapter? = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var server: BluetoothServerSocket? = null
    @Volatile private var on = false; @Volatile private var dialing = false
    private val candidates = ConcurrentHashMap<String, BluetoothDevice>()
    private val failed = ConcurrentHashMap<String, Long>()
    private val linked = ConcurrentHashMap<String, Link>()
    private fun st(ok: Boolean, t: String) = mesh.setTransport(TransportStatus("bt", ok, t))

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val d: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) else @Suppress("DEPRECATION") i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val cls = try { d?.bluetoothClass?.majorDeviceClass } catch (_: Exception) { null }
                    // phones / tablets / computers / unknown only (skip headsets, cars, watches)
                    if (d != null && (cls == null || cls == android.bluetooth.BluetoothClass.Device.Major.PHONE || cls == android.bluetooth.BluetoothClass.Device.Major.COMPUTER || cls == android.bluetooth.BluetoothClass.Device.Major.UNCATEGORIZED || cls == android.bluetooth.BluetoothClass.Device.Major.MISC)) candidates[d.address] = d
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> dialAll()
                BluetoothAdapter.ACTION_STATE_CHANGED -> if (ad?.isEnabled == true) { listen(); st(true, "البلوتوث شغّال") } else st(false, "البلوتوث مطفي")
            }
        }
    }

    fun start() {
        if (ad == null) { st(false, "ما فيه بلوتوث"); return }
        on = true
        ctx.reg(rx, IntentFilter().apply { addAction(BluetoothDevice.ACTION_FOUND); addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); addAction(BluetoothAdapter.ACTION_STATE_CHANGED) })
        if (!safeBool { ad.isEnabled }) { st(false, "البلوتوث مطفي — شغّله عشان يصير احتياطي"); return }
        listen(); tick()
    }

    private fun listen() {
        if (server != null || ad == null) return
        thread(name = "bt-server", isDaemon = true) {
            try {
                val s = ad.listenUsingInsecureRfcommWithServiceRecord("Nidaa", UUID_NIDAA); server = s
                while (on) { val c = s.accept(); mesh.attach(Link(c.inputStream, c.outputStream, "بلوتوث", false, "bt-in") { c.close() }) }
            } catch (e: SecurityException) { st(false, "يحتاج صلاحية الأجهزة القريبة") } catch (_: Exception) {}
            server = null
        }
    }

    fun tick() {
        if (!on || ad == null || dialing) return
        try {
            if (!ad.isEnabled) { st(false, "البلوتوث مطفي"); return }
            ad.bondedDevices?.forEach { candidates[it.address] = it }
            if (!ad.isDiscovering) ad.startDiscovery()
            st(true, if (mesh.linkCount() > 0) "البلوتوث شغّال" else "يبحث بالبلوتوث…")
        } catch (e: SecurityException) { st(false, "يحتاج صلاحية الأجهزة القريبة") }
    }

    private fun dialAll() {
        if (!on || dialing || ad == null) return
        dialing = true
        thread(name = "bt-dial", isDaemon = true) {
            try {
                val now = System.currentTimeMillis()
                candidates.values.filter { d -> linked[d.address]?.closed != false && now - (failed[d.address] ?: 0) > 120_000 }.take(8).forEach { d ->
                    if (!on) return@forEach
                    try {
                        val s = d.createInsecureRfcommSocketToServiceRecord(UUID_NIDAA)
                        s.connect()
                        val l = Link(s.inputStream, s.outputStream, "بلوتوث", true, "bt-out") { s.close() }
                        linked[d.address] = l; mesh.attach(l)
                    } catch (_: Exception) { failed[d.address] = System.currentTimeMillis() }
                }
                candidates.clear()
            } finally { dialing = false }
        }
    }

    fun stop() {
        on = false
        try { ctx.unregisterReceiver(rx) } catch (_: Exception) {}
        try { ad?.cancelDiscovery() } catch (_: Exception) {}
        try { server?.close() } catch (_: Exception) {}; server = null
    }

    private inline fun safeBool(f: () -> Boolean) = try { f() } catch (_: Exception) { false }
}

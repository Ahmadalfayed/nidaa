package com.watan.nidaa

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Keeps the mesh (relay, chat, SOS, walkie-talkie receive) running with the screen off. */
class MeshService : Service() {
    companion object {
        const val STOP = "com.watan.nidaa.STOP"
        fun start(c: Context) = ContextCompat.startForegroundService(c, Intent(c, MeshService::class.java))
        fun stop(c: Context) = c.startService(Intent(c, MeshService::class.java).setAction(STOP))
    }
    private var wake: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null

    override fun onBind(i: Intent?): IBinder? = null

    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun notification() = NotificationCompat.Builder(this, Hub.CH_SERVICE).setSmallIcon(R.drawable.ic_notif)
        .setContentTitle("نِداء شغّال")
        .setContentText(if (Hub.st.peers.isEmpty()) "يبحث عن أجهزة قريبة… يستقبل الرسائل والاستغاثة والصوت" else "متصل بـ ${Hub.st.peers.size} جهاز · يستقبل الرسائل والصوت")
        .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(Hub.openAppIntent())
        .addAction(0, "إيقاف", PendingIntent.getService(this, 1, Intent(this, MeshService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE))
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()

    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Hub.init(this)
        if (intent?.action == STOP) { Hub.mesh.stop(); release(); ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        var type = 0
        if (Build.VERSION.SDK_INT >= 29) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            if (has(Manifest.permission.ACCESS_FINE_LOCATION)) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        try { ServiceCompat.startForeground(this, 1, notification(), type) }
        catch (e: Exception) { try { ServiceCompat.startForeground(this, 1, notification(), if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0) } catch (_: Exception) { stopSelf(); return START_NOT_STICKY } }
        if (wake == null) wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nidaa:mesh").apply { setReferenceCounted(false); acquire() }
        if (wifi == null) wifi = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).createWifiLock(
            @Suppress("DEPRECATION") WifiManager.WIFI_MODE_FULL_HIGH_PERF, "nidaa:wifi").apply { setReferenceCounted(false); acquire() }
        Hub.mesh.start()
        Hub.mesh.onPeersChanged = { refresh() }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun refresh() { try { androidx.core.app.NotificationManagerCompat.from(this).notify(1, notification()) } catch (_: Exception) {} }

    private fun release() { try { wake?.release() } catch (_: Exception) {}; try { wifi?.release() } catch (_: Exception) {}; wake = null; wifi = null }

    override fun onDestroy() { release(); super.onDestroy() }
}

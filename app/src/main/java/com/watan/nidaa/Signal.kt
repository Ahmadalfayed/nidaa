package com.watan.nidaa

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.telephony.TelephonyManager

class SignalMonitor(private val ctx: Context, private val st: AppState, private val onLog: (SignalReading) -> Unit = {}) {
    private val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val listener = LocationListener { l: Location -> st.myLat.value = l.latitude; st.myLon.value = l.longitude }

    @SuppressLint("MissingPermission")
    fun startLocation() {
        try {
            (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))?.let(listener::onLocationChanged)
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER))
                if (lm.isProviderEnabled(p)) lm.requestLocationUpdates(p, 5000L, 5f, listener, Looper.getMainLooper())
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun sample() {
        var dbm: Int? = null; var level = 0; var type = "—"
        try {
            val ss = tm.signalStrength
            val c = ss?.cellSignalStrengths?.firstOrNull()
            level = c?.level ?: ss?.level ?: 0
            dbm = c?.dbm?.takeIf { it in -150..-20 }
        } catch (_: Exception) {}
        try { type = netName(tm.dataNetworkType) } catch (_: SecurityException) { type = "؟" }
        val r = SignalReading(dbm, level, type, st.myLat.value, st.myLon.value, System.currentTimeMillis())
        st.signal.value = r
        if (st.logging.value) { st.signalLog.add(0, r); onLog(r); if (st.signalLog.size > 500) st.signalLog.removeAt(st.signalLog.size - 1) }
    }

    private fun netName(t: Int) = when (t) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "لا شبكة"
        else -> "أخرى"
    }
}

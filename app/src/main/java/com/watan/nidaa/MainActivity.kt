package com.watan.nidaa

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID

class MainActivity : ComponentActivity(), Actions {
    private val st get() = Hub.st
    private val main = Hub.main
    private val mesh get() = Hub.mesh
    private val signal get() = Hub.signal
    private val prefs by lazy { getSharedPreferences("nidaa", MODE_PRIVATE) }
    private val tick = object : Runnable { override fun run() { signal.sample(); checkRadios(); main.postDelayed(this, 2000) } }

    private val perms: Array<String> get() = buildList {
        add(Manifest.permission.RECORD_AUDIO); add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 31) { add(Manifest.permission.BLUETOOTH_SCAN); add(Manifest.permission.BLUETOOTH_ADVERTISE); add(Manifest.permission.BLUETOOTH_CONNECT) }
        if (Build.VERSION.SDK_INT >= 33) { add(Manifest.permission.NEARBY_WIFI_DEVICES); add(Manifest.permission.POST_NOTIFICATIONS) }
    }.toTypedArray()

    private var pendingStart = false

    private val askPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        signal.startLocation()
        if (pendingStart) { pendingStart = false
            val missing = res.filter { !it.value && it.key != Manifest.permission.READ_PHONE_STATE && it.key != Manifest.permission.RECORD_AUDIO && it.key != Manifest.permission.POST_NOTIFICATIONS }
            if (missing.isNotEmpty()) toast("بعض الصلاحيات مرفوضة (الموقع أو الأجهزة القريبة). بيحاول يشتغل، وإذا ما لقى أحد افتح «الاتصال» ← نصائح")
            Hub.bgRunning = st.settings.background.value
            if (Hub.bgRunning) MeshService.start(this) else mesh.start()
            if (!prefs.getBoolean("askedBattery", false)) { prefs.edit().putBoolean("askedBattery", true).apply(); if ("battery" in st.warnings || !isIgnoringBattery()) openSettings("battery") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Hub.init(this)
        applySettings()
        handleIntent(intent)
        if (st.settings.autoStart.value && !st.meshOn.value) pendingStart = true
        askPerms.launch(perms)
        setContent { NidaaApp(st, this) }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }
    private fun handleIntent(i: Intent?) { i?.getStringExtra("group")?.let { if (st.group(it) != null) openChat(it) } }
    override fun onResume() { super.onResume(); Hub.uiVisible = true; main.post(tick) }
    override fun onPause() { super.onPause(); Hub.uiVisible = false; main.removeCallbacks(tick) }
    private fun isIgnoringBattery() = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

    /** Huawei/EMUI: Wi-Fi Direct discovery silently finds nothing unless Location is ON. */
    @SuppressLint("MissingPermission")
    private fun checkRadios() {
        val w = mutableListOf<String>()
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val locOn = if (Build.VERSION.SDK_INT >= 28) lm.isLocationEnabled else lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!locOn) w.add("location")
        if (!(applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).isWifiEnabled) w.add("wifi")
        val bt = try { BluetoothAdapter.getDefaultAdapter()?.isEnabled } catch (_: Exception) { null }
        if (bt == false) w.add("bt")
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) w.add("battery")
        if (w != st.warnings.toList()) { st.warnings.clear(); st.warnings.addAll(w) }
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    private fun saveGroups() = GroupStore.save(prefs, st.groups)

    override fun toggleMesh() { if (st.meshOn.value) { if (Hub.bgRunning) MeshService.stop(this) else mesh.stop() } else { pendingStart = true; askPerms.launch(perms) } }
    override fun selectGroup(id: String) { st.activeGroup.value = id; prefs.edit().putString("active", id).apply() }
    override fun openChat(id: String?) { st.openChat.value = id; if (id != null) selectGroup(id) }
    override fun createGroup(name: String, private: Boolean, pin: String): Group? {
        val n = name.trim().replace("|", " ").take(30); if (n.isEmpty()) return null
        val g = Group((if (private) "p-" else "c-") + GroupStore.newCode(), n, private, GroupStore.arabicDigits(pin.trim()))
        st.groups.add(g); saveGroups(); selectGroup(g.id); return g
    }
    override fun joinGroup(code: String, pin: String): String? {
        val c = GroupStore.normCode(code); if (c.length != 6) return "الرمز لازم يكون ٦ حروف/أرقام"
        val p = GroupStore.arabicDigits(pin.trim()); if (p.length < 4) return "الرقم السري ٤ أرقام على الأقل"
        val id = "p-$c"
        if (st.group(id) != null) return "أنت أصلاً في هالمجموعة"
        st.groups.add(Group(id, "مجموعة $c", true, p)); saveGroups(); selectGroup(id); return null
    }
    override fun joinChannel(g: Group) { if (st.group(g.id) == null) st.groups.add(g); st.nearbyChannels.removeAll { it.id == g.id }; saveGroups(); selectGroup(g.id) }
    override fun leaveGroup(id: String) {
        if (id == Group.GENERAL) return
        st.groups.removeAll { it.id == id }; st.messages.removeAll { it.group == id }; Hub.db.clearGroup(id); saveGroups()
        if (st.activeGroup.value == id) selectGroup(Group.GENERAL); if (st.openChat.value == id) st.openChat.value = null
    }
    override fun pttDown() { if (!st.meshOn.value) { toast("شغّل «نِداء» أول من الزر فوق"); return }; mesh.pttDown() }
    override fun pttUp() = mesh.pttUp()
    override fun sendText(t: String) = mesh.sendText(t)
    override fun shareLocation() { mesh.sendLocation(false); toast(if (st.peers.isEmpty()) "ما فيه أجهزة متصلة للحين" else "انرسل موقعك") }
    override fun sendSos() { mesh.sendLocation(true); toast(if (st.peers.isEmpty()) "ما فيه أجهزة متصلة! أعد الإرسال لما يظهر جهاز" else "انرسلت الاستغاثة لـ ${st.peers.size} جهاز") }
    override fun toggleLogging() { st.logging.value = !st.logging.value }
    override fun clearLog() { st.signalLog.clear(); Hub.db.clearSignal() }
    override fun clearChat(id: String) { st.messages.removeAll { it.group == id }; Hub.db.clearGroup(id); toast("انمسحت المحادثة") }
    override fun clearAlerts() { st.alerts.clear(); Hub.db.clearAlerts() }
    override fun clearAllHistory() { st.messages.clear(); st.alerts.clear(); st.signalLog.clear(); Hub.db.clearAll(); toast("انمسح كل السجل من جوالك") }
    override fun openMap(lat: Double, lon: Double) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))) } catch (_: Exception) { toast("ما فيه تطبيق خرائط") }
    }
    override fun rename(n: String) {
        val v = n.trim().replace("|", " ").ifEmpty { return }
        st.myName.value = v; prefs.edit().putString("name", v).apply(); toast("انحفظ الاسم، أعد التشغيل عشان يتحدث عند الباقين")
    }
    @SuppressLint("BatteryLife")
    override fun openSettings(what: String) {
        val i = when (what) {
            "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bt" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "battery" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            "hotspot" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            "huawei" -> Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            "notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        try { startActivity(i) } catch (_: Exception) { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {} }
    }
    override fun makeDiscoverable() {
        try { startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)) } catch (_: Exception) { toast("تعذّر، شغّل البلوتوث") }
    }
    override fun copy(text: String) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("nidaa", text)); toast("انسخ") }
    override fun showAbout(v: Boolean) { st.showAbout.value = v }
    override fun showSettings(v: Boolean) { st.showSettings.value = v }
    override fun testBeep() = mesh.testTone().let { }
    override fun settingsChanged() {
        st.settings.save(prefs); applySettings()
    }
    private fun applySettings() {
        if (st.settings.keepScreenOn.value) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = if (st.settings.speaker.value) android.media.AudioManager.STREAM_MUSIC else android.media.AudioManager.STREAM_VOICE_CALL
    }
    override fun onDestroy() { super.onDestroy(); if (isFinishing && st.meshOn.value && !Hub.bgRunning) mesh.stop() }
}

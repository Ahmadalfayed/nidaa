package com.watan.nidaa

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.UUID

/** Process-wide singleton: the mesh outlives the Activity and is kept alive by [MeshService]. */
@SuppressLint("StaticFieldLeak")
object Hub {
    const val CH_SERVICE = "svc"; const val CH_MSG = "msg"; const val CH_SOS = "sos"; const val CH_VOICE = "voice"
    lateinit var app: Context; lateinit var st: AppState; lateinit var mesh: Mesh; lateinit var db: Db; lateinit var signal: SignalMonitor
    val main = Handler(Looper.getMainLooper())
    @Volatile var uiVisible = false
    @Volatile var bgRunning = false
    private var ready = false
    private var notifId = 100

    fun init(c: Context) {
        if (ready) return; ready = true
        app = c.applicationContext
        val prefs = app.getSharedPreferences("nidaa", Context.MODE_PRIVATE)
        st = AppState()
        st.nodeId.value = prefs.getString("node", null) ?: UUID.randomUUID().toString().replace("-", "").take(10).also { prefs.edit().putString("node", it).apply() }
        st.myName.value = prefs.getString("name", null) ?: ("جوال-" + (1000..9999).random()).also { prefs.edit().putString("name", it).apply() }
        st.groups.addAll(GroupStore.load(prefs))
        st.settings.load(prefs)
        st.activeGroup.value = (st.settings.defaultGroup.value.takeIf { it != Group.GENERAL && st.group(it) != null }
            ?: prefs.getString("active", Group.GENERAL)!!).let { if (st.group(it) != null) it else Group.GENERAL }
        db = Db(app); try { db.load(st) } catch (_: Exception) {}
        mesh = Mesh(app, st) { f -> main.post(f) }
        mesh.onGroupsChanged = { GroupStore.save(prefs, st.groups) }
        mesh.onMessage = { m -> db.add(m); if (!m.mine) notifyMsg(m) }
        mesh.onAlert = { a -> db.add(a); notifySos(a) }
        mesh.onVoice = { name, gid -> notifyVoice(name, gid) }
        signal = SignalMonitor(app, st) { r -> db.add(r) }
        channels()
    }

    private fun channels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "تشغيل نِداء بالخلفية", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_MSG, "الرسائل", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_SOS, "الاستغاثة", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true); vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800) })
        nm.createNotificationChannel(NotificationChannel(CH_VOICE, "الصوت (لاسلكي)", NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null) })
    }

    fun openAppIntent(gid: String? = null): PendingIntent = PendingIntent.getActivity(app, gid?.hashCode() ?: 0,
        Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).apply { if (gid != null) putExtra("group", gid) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun canNotify() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun post(id: Int, n: android.app.Notification) { if (canNotify()) try { NotificationManagerCompat.from(app).notify(id, n) } catch (_: Exception) {} }

    private fun notifyMsg(m: ChatMsg) {
        if (!st.settings.notifyMsg.value) return
        if (uiVisible && st.openChat.value == m.group) return
        val g = st.group(m.group) ?: return
        post(1000 + (m.group.hashCode() and 0xfff), NotificationCompat.Builder(app, CH_MSG).setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(g.name).setContentText("${m.sender}: ${m.body}").setStyle(NotificationCompat.BigTextStyle().bigText("${m.sender}: ${m.body}"))
            .setAutoCancel(true).setContentIntent(openAppIntent(m.group)).setCategory(NotificationCompat.CATEGORY_MESSAGE).build())
    }

    private fun notifySos(a: SosAlert) {
        if (!st.settings.notifySos.value) return
        post(notifId++, NotificationCompat.Builder(app, if (a.isLocation) CH_MSG else CH_SOS).setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (a.isLocation) "${a.sender} شارك موقعه" else "🆘 استغاثة من ${a.sender}")
            .setContentText(a.lat?.let { "الموقع: %.5f, %.5f".format(java.util.Locale.US, it, a.lon) } ?: "بدون موقع")
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM).setAutoCancel(true).setContentIntent(openAppIntent()).build())
    }

    private var lastVoice = 0L
    private fun notifyVoice(name: String, gid: String) {
        if (!st.settings.notifyVoice.value || uiVisible || System.currentTimeMillis() - lastVoice < 15_000) return
        lastVoice = System.currentTimeMillis()
        post(900, NotificationCompat.Builder(app, CH_VOICE).setSmallIcon(R.drawable.ic_notif).setContentTitle("🎙️ $name يتكلم")
            .setContentText(st.group(gid)?.name ?: "").setTimeoutAfter(20_000).setAutoCancel(true).setContentIntent(openAppIntent(gid)).build())
    }
}

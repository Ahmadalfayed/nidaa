package com.watan.nidaa

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.Executors

/** Local-only history (never leaves the device): chat per group, SOS/location alerts, signal log. */
class Db(ctx: Context) : SQLiteOpenHelper(ctx, "nidaa.db", null, 1) {
    private val io = Executors.newSingleThreadExecutor()
    override fun onCreate(d: SQLiteDatabase) {
        d.execSQL("CREATE TABLE msg(id TEXT PRIMARY KEY, sender TEXT, body TEXT, time INTEGER, mine INTEGER, hops INTEGER, grp TEXT)")
        d.execSQL("CREATE INDEX msg_grp ON msg(grp, time)")
        d.execSQL("CREATE TABLE alert(id TEXT PRIMARY KEY, sender TEXT, lat REAL, lon REAL, time INTEGER, note TEXT, isloc INTEGER)")
        d.execSQL("CREATE TABLE sig(time INTEGER PRIMARY KEY, dbm INTEGER, level INTEGER, type TEXT, lat REAL, lon REAL)")
    }
    override fun onUpgrade(d: SQLiteDatabase, o: Int, n: Int) {}

    private fun bg(f: SQLiteDatabase.() -> Unit) = io.execute { try { writableDatabase.f() } catch (_: Exception) {} }

    fun add(m: ChatMsg) = bg { insertWithOnConflict("msg", null, ContentValues().apply {
        put("id", m.id); put("sender", m.sender); put("body", m.body); put("time", m.time); put("mine", if (m.mine) 1 else 0); put("hops", m.hops); put("grp", m.group) }, SQLiteDatabase.CONFLICT_IGNORE) }
    fun add(a: SosAlert) = bg { insertWithOnConflict("alert", null, ContentValues().apply {
        put("id", a.id); put("sender", a.sender); put("lat", a.lat); put("lon", a.lon); put("time", a.time); put("note", a.note); put("isloc", if (a.isLocation) 1 else 0) }, SQLiteDatabase.CONFLICT_IGNORE) }
    fun add(r: SignalReading) = bg { insertWithOnConflict("sig", null, ContentValues().apply {
        put("time", r.time); put("dbm", r.dbm); put("level", r.level); put("type", r.type); put("lat", r.lat); put("lon", r.lon) }, SQLiteDatabase.CONFLICT_REPLACE)
        execSQL("DELETE FROM sig WHERE time NOT IN (SELECT time FROM sig ORDER BY time DESC LIMIT 2000)") }

    fun clearGroup(gid: String) = bg { delete("msg", "grp=?", arrayOf(gid)) }
    fun clearAlerts() = bg { delete("alert", null, null) }
    fun clearSignal() = bg { delete("sig", null, null) }
    fun clearAll() = bg { delete("msg", null, null); delete("alert", null, null); delete("sig", null, null) }

    /** Loads history synchronously (called once at startup). Last 3000 messages (no window functions: Android 8-10 SQLite). */
    fun load(st: AppState) {
        val d = readableDatabase
        d.rawQuery("SELECT id,sender,body,time,mine,hops,grp FROM (SELECT * FROM msg ORDER BY time DESC LIMIT 3000) ORDER BY time", null).use { c ->
            while (c.moveToNext()) st.messages.add(ChatMsg(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getInt(4) == 1, c.getInt(5), c.getString(6)))
        }
        d.rawQuery("SELECT id,sender,lat,lon,time,note,isloc FROM alert ORDER BY time DESC LIMIT 200", null).use { c ->
            while (c.moveToNext()) st.alerts.add(SosAlert(c.getString(0), c.getString(1), if (c.isNull(2)) null else c.getDouble(2), if (c.isNull(3)) null else c.getDouble(3), c.getLong(4), c.getString(5), c.getInt(6) == 1))
        }
        d.rawQuery("SELECT time,dbm,level,type,lat,lon FROM sig ORDER BY time DESC LIMIT 500", null).use { c ->
            while (c.moveToNext()) st.signalLog.add(SignalReading(if (c.isNull(1)) null else c.getInt(1), c.getInt(2), c.getString(3), if (c.isNull(4)) null else c.getDouble(4), if (c.isNull(5)) null else c.getDouble(5), c.getLong(0)))
        }
    }
}

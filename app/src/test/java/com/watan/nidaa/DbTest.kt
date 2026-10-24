package com.watan.nidaa
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])
class DbTest {
    @Test fun persist() {
        val db = Db(ApplicationProvider.getApplicationContext())
        db.add(ChatMsg("1", "نورة", "هلا", 1000, false, 2, "p-X")); db.add(ChatMsg("2", "أنا", "أهلين", 2000, true, 0, "c-general"))
        db.add(SosAlert("s", "نورة", 29.1, 47.6, 3000, "استغاثة", false)); db.add(SignalReading(-100, 2, "4G", null, null, 4000))
        Thread.sleep(500)
        val st = AppState(); db.load(st)
        assertEquals(2, st.messages.size); assertEquals("هلا", st.messages[0].body); assertEquals(1, st.alerts.size); assertEquals(1, st.signalLog.size); assertNull(st.signalLog[0].lat)
        db.clearGroup("p-X"); db.clearAlerts(); Thread.sleep(300)
        val s2 = AppState(); db.load(s2); assertEquals(1, s2.messages.size); assertEquals(0, s2.alerts.size)
    }
}

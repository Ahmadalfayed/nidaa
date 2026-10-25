package com.watan.nidaa

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ar-w393dp-h852dp-xxhdpi")
class Shots {
    @get:Rule val rule = createComposeRule()

    private fun sample() = Samples.state()


    private fun shot(tab: Tab, name: String, st: AppState = sample()) {
        rule.setContent { NidaaApp(st, object : Actions {}, tab) }
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    @Test fun talk() = shot(Tab.Talk, "01-talk")
    @Test fun talkTx() = shot(Tab.Talk, "02-talk-transmitting", sample().apply { transmitting.value = false; incomingFrom.value = "أبو سالم" })
    @Test fun groups() = shot(Tab.Groups, "03-groups")
    @Test fun groupChat() = shot(Tab.Talk, "08-group-chat", sample().apply { openChat.value = "p-K7Q2MF" })
    @Test fun connection() = shot(Tab.Link, "09-connection")
    @Test fun connectionEmpty() = shot(Tab.Link, "10-connection-searching", AppState().apply { myName.value = "جوال-1234"; nodeId.value = "f00dbeef12"; meshOn.value = true
        transports.addAll(listOf(TransportStatus("wifi", false, "الواي فاي مشغول، شغّل الموقع (GPS) والواي فاي وطفّ الهوتسبوت"), TransportStatus("lan", true, "يدور على أجهزة في نفس الشبكة أو الهوتسبوت"), TransportStatus("bt", false, "البلوتوث مطفي")))
        warnings.addAll(listOf("location", "bt", "battery")) })
    @Test fun createDialog() { rule.setContent { NidaaTheme { Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp).clip(RoundedCornerShape(28.dp)).background(Card).padding(24.dp)) {
            Text("إنشاء مجموعة أو قناة", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(16.dp))
            CreateGroupFields("ربع البر", {}, true, {}, "1234", {}); Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({}) { Text("إلغاء") }; Button({}) { Text("إنشاء") } }
        } } } }; rule.onRoot().captureRoboImage("../screenshots/11-create-group.png") }
    @Test fun createdDialog() { rule.setContent { NidaaTheme { CreatedDialog(Group("p-K7Q2MF", "ربع البر", true, "1234"), object : Actions {}) {} } }; com.github.takahirom.roborazzi.captureScreenRoboImage("../screenshots/12-group-code.png") }
    @Test fun settings() = shot(Tab.Talk, "14-settings", sample().apply { showSettings.value = true })
    @Test @Config(qualifiers = "ar-w393dp-h2200dp-xxhdpi") fun settingsFull() = shot(Tab.Talk, "15-settings-full", sample().apply { showSettings.value = true })
    @Test fun talkWarn() = shot(Tab.Talk, "13-talk-warning", sample().apply { warnings.add("location") })
    @Test fun signal() = shot(Tab.Signal, "04-signal")
    @Test fun sos() = shot(Tab.Sos, "05-sos")
    @Test fun about() = shot(Tab.Talk, "06-about", sample().apply { showAbout.value = true })
    @Test fun empty() = shot(Tab.Talk, "07-first-run", AppState().apply { myName.value = "جوال-1234" })
}

object Samples {
    fun state() = AppState().apply {
        val now = System.currentTimeMillis()
        myName.value = "وطن"; nodeId.value = "a1b2c3d4e5"; meshOn.value = true; status.value = "متصل بـ 3 جهاز"
        peers.addAll(listOf(Peer("a", "أبو سالم", "واي فاي مباشر"), Peer("b", "نورة", "شبكة/هوتسبوت"), Peer("c", "جوال-4821", "بلوتوث")))
        groups.addAll(listOf(Group("p-K7Q2MF", "ربع البر", true, "1234"), Group("c-R4TX9A", "رحلة الصمان", false)))
        activeGroup.value = "p-K7Q2MF"
        nearbyChannels.add(Group("c-ZZ88QQ", "مخيم الوفرة", false))
        meshNodes["d"] = MeshNode("d", "أبو فهد", 3, now)
        transports.addAll(listOf(TransportStatus("wifi", true, "أنت مركز مجموعة الواي فاي المباشر"), TransportStatus("lan", true, "يدور على أجهزة في نفس الشبكة أو الهوتسبوت"), TransportStatus("bt", true, "البلوتوث شغّال")))
        warnings.add("battery")
        messages.addAll(listOf(
            ChatMsg("1", "أبو سالم", "وصلنا عند الطعس الكبير، وينكم؟", now - 600000, false, 1, "p-K7Q2MF"),
            ChatMsg("2", "وطن", "جايين، بعدنا ورا التل بخمس دقايق", now - 540000, true, 0, "p-K7Q2MF"),
            ChatMsg("3", "نورة", "السيارة الثانية غرزت، نبي حبل 🙏", now - 300000, false, 3, "p-K7Q2MF"),
            ChatMsg("4", "وطن", "أبشروا، جايينكم الحين", now - 240000, true, 0, "p-K7Q2MF")))
        relayedCount.value = 12
        myLat.value = 29.12345; myLon.value = 47.65432
        signal.value = SignalReading(-104, 2, "4G", 29.1234, 47.6543, now)
        signalLog.addAll(listOf(signal.value!!, SignalReading(-108, 1, "4G", 29.1230, 47.6540, now - 4000), SignalReading(-97, 3, "4G", 29.1301, 47.6611, now - 60000), SignalReading(null, 0, "لا شبكة", 29.12, 47.65, now - 90000)))
        logging.value = true
        alerts.addAll(listOf(SosAlert("s", "نورة", 29.1402, 47.6702, now - 120000, "استغاثة! أحتاج مساعدة", false), SosAlert("l", "أبو سالم", 29.1388, 47.6690, now - 400000, "موقعي", true)))
    }}

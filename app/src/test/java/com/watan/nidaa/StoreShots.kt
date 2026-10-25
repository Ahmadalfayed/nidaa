package com.watan.nidaa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Google Play phone screenshots: 1080x1920 (9:16). */
@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ar-w360dp-h640dp-xxhdpi")
class StoreShots {
    @get:Rule val rule = createComposeRule()
    private fun shot(tab: Tab, name: String, st: AppState = Samples.state()) {
        rule.setContent { NidaaApp(st, object : Actions {}, tab) }
        rule.onRoot().captureRoboImage("../store/screenshots/phone/$name.png")
    }
    @Test fun s1() = shot(Tab.Talk, "1-walkie-talkie")
    @Test fun s2() = shot(Tab.Groups, "2-groups")
    @Test fun s3() = shot(Tab.Talk, "3-group-chat", Samples.state().apply { openChat.value = "p-K7Q2MF" })
    @Test fun s4() = shot(Tab.Link, "4-connection")
    @Test fun s5() = shot(Tab.Sos, "5-sos")
    @Test fun s6() = shot(Tab.Signal, "6-signal")
    @Test fun s7() = shot(Tab.Talk, "7-settings", Samples.state().apply { showSettings.value = true })
}

@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ar-w1024dp-h500dp-mdpi")
class FeatureGraphic {
    @get:Rule val rule = createComposeRule()
    @Test fun feature() {
        rule.setContent { NidaaTheme {
            Box(Modifier.size(1024.dp, 500.dp).background(Brush.linearGradient(listOf(Color(0xFF2A2118), Night, Color(0xFF3A2614))))) {
                Row(Modifier.fillMaxSize().padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Logo(300); Spacer(Modifier.width(48.dp))
                    Column {
                        Text("نِداء", fontFamily = Cairo, fontWeight = FontWeight.ExtraBold, fontSize = 110.sp, color = Gold)
                        Text("لاسلكي ومحادثة بدون إنترنت", fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color(0xFFF3EADF))
                        Text("مجموعات مشفّرة · استغاثة بالموقع · بدون شبكة", fontFamily = Cairo, fontSize = 22.sp, color = Muted)
                    }
                }
            }
        } }
        rule.onRoot().captureRoboImage("../store/graphics/feature-graphic-1024x500.png")
    }
}

@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w512dp-h512dp-mdpi")
class StoreIcon {
    @get:Rule val rule = createComposeRule()
    @Test fun icon() {
        rule.setContent { Box(Modifier.size(512.dp).background(Brush.verticalGradient(listOf(Color(0xFF2A2118), Night))), contentAlignment = Alignment.Center) { Logo(512) } }
        rule.onRoot().captureRoboImage("../store/graphics/icon-512.png")
    }
}

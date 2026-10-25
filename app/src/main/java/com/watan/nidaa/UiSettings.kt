package com.watan.nidaa

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
private fun Header(t: String) = Text(t, style = MaterialTheme.typography.titleMedium, color = Gold, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String?, v: MutableState<Boolean>, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { v.value = !v.value; onChange() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Sand, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(v.value, { v.value = it; onChange() }, colors = SwitchDefaults.colors(checkedTrackColor = Sand, checkedThumbColor = Night))
    }
}

@Composable
private fun SliderRow(icon: ImageVector, title: String, sub: String, v: MutableFloatState, range: ClosedFloatingPointRange<Float>, onChange: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Sand, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(ltr("+" + String.format(Locale.US, "%.0f", v.floatValue) + " dB"), color = Gold, style = MaterialTheme.typography.titleSmall)
        }
        Slider(v.floatValue, { v.floatValue = Math.round(it).toFloat() }, valueRange = range, steps = (range.endInclusive - range.start).toInt() / 3 - 1,
            onValueChangeFinished = onChange, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Sand, inactiveTrackColor = Card2))
        Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SettingsScreen(st: AppState, a: Actions) {
    val s = st.settings; val ch = { a.settingsChanged() }
    var name by remember { mutableStateOf(st.myName.value) }
    var confirmClear by remember { mutableStateOf(false) }
    var groupMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Header("اسمك")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                OutlinedTextField(name, { name = it.take(20) }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.width(8.dp)); Button(onClick = { a.rename(name) }) { Text("حفظ") }
            }
        }
        SectionCard {
            Header("الصوت")
            SliderRow(Icons.Filled.VolumeUp, "تعلية صوت المستقبَل", "يكبّر صوت اللي يكلمك. إذا صار فيه تشويش نزّله شوي. أزرار الصوت في الجوال تتحكم فيه بعد.", s.playBoostDb, 0f..24f, ch)
            SliderRow(Icons.Filled.Mic, "تقوية الميكروفون", "يرفع صوتك تلقائياً لما يكون واطي، لين هالحد.", s.micGainDb, 0f..18f, ch)
            ToggleRow(Icons.Filled.Speaker, "السماعة الخارجية", if (s.speaker.value) "الصوت يطلع من السماعة الكبيرة (أعلى)" else "الصوت يطلع من سماعة الأذن مثل المكالمة", s.speaker, ch)
            ToggleRow(Icons.Filled.NotificationsActive, "نغمة بداية ونهاية الكلام", "تنبيه قصير «روجر» لما تبدأ وتخلص", s.rogerBeep, ch)
            ToggleRow(Icons.Filled.GraphicEq, "تقليل الضوضاء", "يخفف صوت الهوا والسيارة من الميكروفون", s.noiseSuppress, ch)
            OutlinedButton(onClick = a::testBeep, modifier = Modifier.padding(top = 4.dp)) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("جرّب الصوت") }
        }
        SectionCard {
            Header("الإشعارات")
            ToggleRow(Icons.Filled.Forum, "الرسائل", null, s.notifyMsg, ch)
            ToggleRow(Icons.Filled.Sos, "الاستغاثة", "ننصحك تخليها شغالة", s.notifySos, ch)
            ToggleRow(Icons.Filled.RecordVoiceOver, "أحد يتكلم", null, s.notifyVoice, ch)
        }
        SectionCard {
            Header("التشغيل")
            ToggleRow(Icons.Filled.Bedtime, "يشتغل والشاشة مطفية", "إشعار ثابت ويظل يستقبل ويمرّر (يتطبق من التشغيل الجاي)", s.background, ch)
            ToggleRow(Icons.Filled.PlayCircle, "يبدأ البحث أول ما أفتح التطبيق", null, s.autoStart, ch)
            ToggleRow(Icons.Filled.LightMode, "الشاشة تظل شغالة والتطبيق مفتوح", null, s.keepScreenOn, ch)
            Box {
                Row(Modifier.fillMaxWidth().clickable { groupMenu = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Campaign, null, tint = Sand, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("القناة الافتراضية", style = MaterialTheme.typography.bodyLarge)
                        Text("تفتح عليها كل مرة", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(st.group(s.defaultGroup.value)?.name ?: "العامة", color = Gold); Icon(Icons.Filled.ArrowDropDown, null, tint = Gold)
                }
                DropdownMenu(groupMenu, { groupMenu = false }, containerColor = Card2) {
                    st.groups.forEach { g -> DropdownMenuItem(text = { Text(g.name) }, onClick = { s.defaultGroup.value = g.id; groupMenu = false; ch() }) }
                }
            }
        }
        SectionCard {
            Header("هواوي: عشان ما يتوقف")
            Text("الإعدادات ← البطارية ← تشغيل التطبيقات ← نِداء ← طفّ «إدارة تلقائية» وفعّل: التشغيل التلقائي، التشغيل الثانوي، التشغيل في الخلفية.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                OutlinedButton(onClick = { a.openSettings("huawei") }) { Text("تشغيل التطبيقات") }
                OutlinedButton(onClick = { a.openSettings("battery") }) { Text("البطارية") }
                OutlinedButton(onClick = { a.openSettings("notifications") }) { Text("إشعارات النظام") }
                OutlinedButton(onClick = { a.openSettings("app") }) { Text("الأذونات") }
            }
        }
        SectionCard {
            Header("السجل")
            Text("المحادثات والاستغاثات وسجل الإشارة محفوظة في جوالك بس.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) {
                Icon(Icons.Filled.DeleteSweep, null); Spacer(Modifier.width(6.dp)); Text("مسح كل السجل") }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card).clickable { a.showSettings(false); a.showAbout(true) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, tint = Sand); Spacer(Modifier.width(12.dp))
            Text("حول «نِداء» والحقوق", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); Icon(Icons.Filled.ChevronLeft, null, tint = Muted)
        }
        Text("الإصدار 1.3.0 · © 2026 وطن النهار — جميع الحقوق محفوظة", color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, containerColor = Card, title = { Text("تمسح كل السجل؟") },
        text = { Text("بتنمسح كل المحادثات والاستغاثات وسجل الإشارة من هالجوال. المجموعات تبقى.") },
        confirmButton = { Button(onClick = { confirmClear = false; a.clearAllHistory() }, colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)) { Text("امسح") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } })
}

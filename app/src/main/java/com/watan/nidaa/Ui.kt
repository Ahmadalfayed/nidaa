package com.watan.nidaa

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

val Sand = Color(0xFFE0A458); val Gold = Color(0xFFF2C879); val Dune = Color(0xFFC8873E)
val Night = Color(0xFF14110E); val Card = Color(0xFF221D17); val Card2 = Color(0xFF2C251D)
val Muted = Color(0xFFA89A88); val Danger = Color(0xFFE5484D); val Ok = Color(0xFF4CC38A)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun cairo(w: Int) = Font(R.font.cairo, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
val Cairo = FontFamily(cairo(400), cairo(600), cairo(700), cairo(800))

@Composable
fun NidaaTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val t = Typography(
        displaySmall = base.displaySmall.copy(fontFamily = Cairo), headlineSmall = base.headlineSmall.copy(fontFamily = Cairo, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = Cairo, fontWeight = FontWeight.Bold), titleMedium = base.titleMedium.copy(fontFamily = Cairo, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = Cairo), bodyLarge = base.bodyLarge.copy(fontFamily = Cairo), bodyMedium = base.bodyMedium.copy(fontFamily = Cairo),
        bodySmall = base.bodySmall.copy(fontFamily = Cairo), labelLarge = base.labelLarge.copy(fontFamily = Cairo, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = Cairo), labelSmall = base.labelSmall.copy(fontFamily = Cairo),
    )
    MaterialTheme(colorScheme = darkColorScheme(primary = Sand, onPrimary = Night, secondary = Gold, background = Night, surface = Card,
        onSurface = Color(0xFFF3EADF), onBackground = Color(0xFFF3EADF), surfaceVariant = Card2, onSurfaceVariant = Muted, error = Danger), typography = t) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    Talk("لاسلكي", Icons.Filled.SettingsInputAntenna), Groups("المجموعات", Icons.Filled.Forum), Link("الاتصال", Icons.Filled.Hub),
    Signal("الإشارة", Icons.Filled.SignalCellularAlt), Sos("طوارئ", Icons.Filled.Sos)
}

@Composable
fun NidaaApp(st: AppState, a: Actions, start: Tab = Tab.Talk) {
    var tab by rememberSaveable { mutableStateOf(start) }
    NidaaTheme {
        val chat = st.openChat.value?.let { st.group(it) }
        if (chat != null) { GroupChatScreen(st, a, chat); return@NidaaTheme }
        if (st.showSettings.value) { Column(Modifier.fillMaxSize().background(Night).statusBarsPadding().navigationBarsPadding()) {
            BackBar("الإعدادات") { a.showSettings(false) }; SettingsScreen(st, a) }; return@NidaaTheme }
        if (st.showAbout.value) { Column(Modifier.fillMaxSize().background(Night).statusBarsPadding().navigationBarsPadding()) {
            BackBar("حول «نِداء»") { a.showAbout(false) }; AboutScreen(st, a) }; return@NidaaTheme }
        Scaffold(containerColor = Night, topBar = { TopBar(st, a) { tab = Tab.Link } }, bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E1A15)) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(selected = tab == t, onClick = { tab = t }, icon = { Icon(t.icon, null) },
                        label = { Text(t.label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Night, indicatorColor = Sand,
                            selectedTextColor = Sand, unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    Tab.Talk -> TalkScreen(st, a) { tab = it }; Tab.Groups -> GroupsScreen(st, a); Tab.Link -> ConnectionScreen(st, a)
                    Tab.Signal -> SignalScreen(st, a); Tab.Sos -> SosScreen(st, a)
                }
            }
        }
    }
}

@Composable
fun Logo(size: Int = 40) {
    Canvas(Modifier.size(size.dp)) {
        val w = this.size.width; val sw = w * 0.07f
        drawCircle(Brush.verticalGradient(listOf(Color(0xFF2A2118), Night)))
        drawArc(Dune, 200f, 140f, false, Offset(w * .18f, w * .2f), androidx.compose.ui.geometry.Size(w * .64f, w * .64f), style = Stroke(sw, cap = StrokeCap.Round))
        drawArc(Sand, 215f, 110f, false, Offset(w * .3f, w * .32f), androidx.compose.ui.geometry.Size(w * .4f, w * .4f), style = Stroke(sw, cap = StrokeCap.Round))
        drawCircle(Gold, w * .07f, Offset(w / 2, w * .52f))
        val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * .15f, w * .8f); cubicTo(w * .35f, w * .64f, w * .5f, w * .66f, w * .6f, w * .73f); cubicTo(w * .7f, w * .8f, w * .78f, w * .74f, w * .88f, w * .68f); lineTo(w * .88f, w * .85f); lineTo(w * .15f, w * .85f); close() }
        drawPath(p, Dune)
    }
}

@Composable
fun TopBar(st: AppState, a: Actions, onStatus: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(Night).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo(40); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).clickable(onClick = onStatus)) {
            Text("نِداء", style = MaterialTheme.typography.titleLarge, color = Gold)
            Text(st.status.value.ifEmpty { "تواصل بدون إنترنت" }, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
        }
        IconButton(onClick = { a.showSettings(true) }) { Icon(Icons.Filled.Settings, "الإعدادات", tint = Sand) }
        val on = st.meshOn.value
        FilledTonalButton(onClick = a::toggleMesh, colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (on) Ok.copy(.18f) else Card2, contentColor = if (on) Ok else Sand)) {
            Icon(if (on) Icons.Filled.WifiTethering else Icons.Filled.WifiTetheringOff, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(if (on) "شغّال" else "تشغيل")
        }
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card).padding(16.dp), content = content)

@Composable
fun PeersRow(st: AppState, onClick: () -> Unit = {}) {
    SectionCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Groups, null, tint = Sand); Spacer(Modifier.width(8.dp))
            Text("الأجهزة القريبة", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${st.peers.size}", color = Gold, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
        if (st.peers.isEmpty()) Text(if (st.meshOn.value) "يبحث… قرّب الجوالات من بعض (حتى ١٠٠ متر)" else "اضغط «تشغيل» فوق عشان تبدأ تلقط الأجهزة", color = Muted, style = MaterialTheme.typography.bodyMedium)
        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            st.peers.forEach { p -> AssistChip(onClick = onClick, label = { Text(p.name + if (p.via.isNotEmpty()) " · ${p.via}" else "") }, leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(Ok)) }) }
        }
    }
}

@Composable
fun TalkScreen(st: AppState, a: Actions, go: (Tab) -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (st.meshOn.value && st.warnings.any { it != "battery" }) WarningCard(st, a) { go(Tab.Link) }
        PeersRow(st) { go(Tab.Link) }
        SectionCard {
            Text("تتكلم في", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            val gs = st.groups; val i = gs.indexOfFirst { it.id == st.activeGroup.value }.coerceAtLeast(0); val g = gs[i]
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { a.selectGroup(gs[(i - 1 + gs.size) % gs.size].id) }) { Icon(Icons.Filled.ChevronRight, null, tint = Sand) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (g.private) Icons.Filled.Lock else Icons.Filled.Campaign, null, tint = Gold, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(8.dp))
                    Text(g.name, fontFamily = Cairo, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Gold, maxLines = 1)
                }
                IconButton(onClick = { a.selectGroup(gs[(i + 1) % gs.size].id) }) { Icon(Icons.Filled.ChevronLeft, null, tint = Sand) }
            }
            Text(if (g.private) "مجموعة خاصة مشفّرة — بس أعضاؤها يسمعونك" else "قناة عامة — كل من دخلها يسمعك", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            TextButton(onClick = { go(Tab.Groups) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Icon(Icons.Filled.GroupAdd, null); Spacer(Modifier.width(6.dp)); Text("إنشاء مجموعة أو قناة") }
        }
        val tx = st.transmitting.value; val rx = st.incomingFrom.value
        val pulse by rememberInfiniteTransition(label = "p").animateFloat(1f, 1.08f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).scale(if (tx) pulse else 1f).clip(CircleShape)
                .background(Brush.radialGradient(if (tx) listOf(Danger, Color(0xFF7A1F22)) else listOf(Sand, Dune)))
                .border(6.dp, (if (tx) Danger else Gold).copy(.35f), CircleShape)
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(); a.pttDown(); waitForUpOrCancellation(); a.pttUp() } },
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Mic, null, Modifier.size(64.dp), tint = if (tx) Color.White else Night)
                    Text(if (tx) "يرسل…" else "اضغط وتكلّم", color = if (tx) Color.White else Night, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VolumeUp, null, tint = if (rx != null) Ok else Muted); Spacer(Modifier.width(8.dp))
                Text(if (rx != null) "يتكلم الحين: $rx" else "ما فيه أحد يتكلم", color = if (rx != null) Ok else Muted)
            }
        }
    }
}

private object hm { private val f = SimpleDateFormat("h:mm", Locale.US)
    fun format(d: Date): String { val c = Calendar.getInstance().apply { time = d }; return "\u2066" + f.format(d) + "\u2069 " + if (c.get(Calendar.HOUR_OF_DAY) < 12) "ص" else "م" } }
fun ltr(s: String) = "\u2066$s\u2069"

@Composable
fun ChatBody(st: AppState, a: Actions, g: Group, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val list = st.messages.filter { it.group == g.id }
    val ls = rememberLazyListState()
    LaunchedEffect(list.size) { if (list.isNotEmpty()) ls.animateScrollToItem(list.size - 1) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (g.private) "🔒 مشفّرة بالرقم السري" else "📢 قناة عامة", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Hub, null, tint = Muted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
            Text("مرّرت ${st.relayedCount.value} رسالة لغيرك", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = ls, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) item { Text("الرسائل تنتقل من جوال لجوال لين توصل، حتى لو الشخص بعيد عنك ما دام فيه جوالات بينكم.", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            items(list, key = { it.id }) { m -> Bubble(m) }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF1E1A15)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("اكتب رسالة…") }, shape = RoundedCornerShape(24.dp), maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sand, unfocusedBorderColor = Card2))
            Spacer(Modifier.width(6.dp))
            val tx = st.transmitting.value
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (tx) Danger else Card2)
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(); a.pttDown(); waitForUpOrCancellation(); a.pttUp() } }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, "اضغط وتكلّم", tint = if (tx) Color.White else Sand)
            }
            Spacer(Modifier.width(6.dp))
            FilledIconButton(onClick = { if (text.isNotBlank()) { a.sendText(text.trim()); text = "" } }, Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
            }
        }
    }
}

@Composable
fun Bubble(m: ChatMsg) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.Start else Arrangement.End) {
        Column(Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp)).background(if (m.mine) Sand else Card2).padding(horizontal = 14.dp, vertical = 8.dp)) {
            if (!m.mine) Text(m.sender, color = Gold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(m.body, color = if (m.mine) Night else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(hm.format(Date(m.time)) + if (!m.mine && m.hops > 1) " · عبر ${m.hops - 1} جوال" else "",
                color = if (m.mine) Night.copy(.6f) else Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun hintFor(r: SignalReading?, log: List<SignalReading>): String {
    if (r == null) return "جارِ القراءة…"
    if (r.dbm == null) return "ما فيه شبكة هني. جرّب تطلع مكان مرتفع أو مفتوح بعيد عن الجبال والسيارة."
    val prev = log.drop(1).take(5).mapNotNull { it.dbm }.average().takeIf { !it.isNaN() }
    val trend = when { prev == null -> ""; r.dbm > prev + 3 -> " الإشارة تتحسن، كمّل بنفس الاتجاه ⬆️"; r.dbm < prev - 3 -> " الإشارة تضعف، ارجع للمكان اللي قبل ⬇️"; else -> "" }
    return when {
        r.dbm >= -85 -> "إشارة ممتازة."; r.dbm >= -100 -> "إشارة جيدة."; r.dbm >= -110 -> "إشارة ضعيفة، المكالمات ممكن تتقطع."
        else -> "إشارة ضعيفة جداً، جرّب ترفع الجوال أو تطلع على مرتفع."
    } + trend
}

@Composable
fun SignalScreen(st: AppState, a: Actions) {
    val r = st.signal.value
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard {
            Row(verticalAlignment = Alignment.Bottom) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    (1..4).forEach { i -> Box(Modifier.width(18.dp).height((18 * i + 8).dp).clip(RoundedCornerShape(5.dp)).background(if ((r?.level ?: 0) >= i) Gold else Card2)) }
                } }
                Column(horizontalAlignment = Alignment.End) {
                    Text(r?.dbm?.let { "$it" } ?: "—", fontFamily = Cairo, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Gold, style = TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr))
                    Text("dBm · ${r?.type ?: "—"}", color = Muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(hintFor(r, st.signalLog), style = MaterialTheme.typography.bodyLarge)
        }
        val best = st.signalLog.filter { it.dbm != null && it.lat != null }.maxByOrNull { it.dbm!! }
        if (best != null) SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = Gold); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("أفضل مكان سجّلته: " + ltr("${best.dbm} dBm"), style = MaterialTheme.typography.titleMedium)
                    Text("%.5f, %.5f".format(Locale.US, best.lat, best.lon), color = Muted, style = TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr, fontFamily = Cairo))
                }
                TextButton(onClick = { a.openMap(best.lat!!, best.lon!!) }) { Text("خريطة") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = a::toggleLogging, Modifier.weight(1f).height(52.dp), colors = if (st.logging.value) ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White) else ButtonDefaults.buttonColors()) {
                Icon(if (st.logging.value) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, null); Spacer(Modifier.width(6.dp))
                Text(if (st.logging.value) "إيقاف التسجيل" else "سجّل القراءات مع الموقع")
            }
            OutlinedButton(onClick = a::clearLog, Modifier.height(52.dp)) { Text("مسح") }
        }
        if (st.signalLog.isNotEmpty()) SectionCard {
            Text("السجل (${st.signalLog.size})", style = MaterialTheme.typography.titleMedium)
            st.signalLog.take(30).forEach { x ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(hm.format(Date(x.time)), color = Muted, modifier = Modifier.width(80.dp))
                    Text(x.dbm?.let { ltr("$it dBm") } ?: "لا شبكة", modifier = Modifier.weight(1f), color = if ((x.dbm ?: -200) >= -100) Ok else Sand)
                    Text(x.type, color = Muted)
                }
            }
        }
        Text("ملاحظة: التطبيق يقيس الإشارة ويدلك على أفضل مكان، لكنه ما يقدر يقوّيها. قوة الإشارة تعتمد على قطعة الاستقبال في جوالك وعلى أبراج الشركة.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SosScreen(st: AppState, a: Actions) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Danger, Color(0xFF8E2328)))).clickable { confirm = true }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Sos, null, Modifier.size(64.dp), tint = Color.White)
                Text("زر الاستغاثة", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("يرسل موقعك لكل الأجهزة القريبة", color = Color.White.copy(.85f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MyLocation, null, tint = Sand); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("موقعي", style = MaterialTheme.typography.titleMedium)
                    Text(if (st.myLat.value != null) ltr("%.5f, %.5f".format(Locale.US, st.myLat.value, st.myLon.value)) else "جارِ تحديد الموقع بالـ GPS…", color = Muted,
                        style = TextStyle(fontFamily = Cairo, textDirection = androidx.compose.ui.text.style.TextDirection.Content))
                }
                Button(onClick = a::shareLocation) { Text("شارك موقعي") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("التنبيهات الواصلة", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (st.alerts.isNotEmpty()) TextButton(onClick = a::clearAlerts) { Text("مسح") }
        }
        if (st.alerts.isEmpty()) Text("ما وصلك شي للحين.", color = Muted)
        st.alerts.forEach { al ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (al.isLocation) Card else Danger.copy(.16f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (al.isLocation) Icons.Filled.Place else Icons.Filled.Warning, null, tint = if (al.isLocation) Sand else Danger); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${al.sender} · ${al.note}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(hm.format(Date(al.time)) + (al.lat?.let { " · " + ltr("%.4f, %.4f".format(Locale.US, it, al.lon)) } ?: " · بدون موقع"), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                if (al.lat != null && al.lon != null) TextButton(onClick = { a.openMap(al.lat, al.lon) }) { Text("خريطة") }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = Card,
        title = { Text("إرسال استغاثة؟") }, text = { Text("بيوصل موقعك وتنبيه استغاثة لكل الجوالات القريبة اللي فيها «نِداء»، وينتقل من جوال لجوال.") },
        confirmButton = { Button(onClick = { a.sendSos(); confirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)) { Text("أرسل") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("إلغاء") } })
}

@Composable
fun AboutScreen(st: AppState, a: Actions) {
    var name by remember { mutableStateOf(st.myName.value) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(88); Text("نِداء", style = MaterialTheme.typography.headlineSmall, color = Gold); Text("الإصدار 1.3.0", color = Muted)
        }
        Button(onClick = { a.showAbout(false); a.showSettings(true) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(8.dp)); Text("الإعدادات") }
        SectionCard {
            Text("اسمك اللي يشوفونه الباقين", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(name, { name = it.take(20) }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.width(8.dp)); Button(onClick = { a.rename(name) }) { Text("حفظ") }
            }
        }
        SectionCard {
            Text("شلون يشتغل؟", style = MaterialTheme.typography.titleMedium)
            Text("يوصل الجوالات ببعض مباشرة عن طريق الواي فاي المباشر والبلوتوث والهوتسبوت، بدون إنترنت ولا شبكة ولا خدمات Google، فيشتغل على هواوي وغيره. المدى بين جوالين تقريباً ٥٠–١٠٠ متر، والرسائل النصية والاستغاثة تنتقل من جوال لجوال فيزيد المدى كل ما زاد عدد الجوالات. الصوت يوصل للجوالات المتصلة فيك مباشرة.", color = Muted)
        }
        SectionCard {
            Text("بصراحة، شنو ما يقدر يسويه", style = MaterialTheme.typography.titleMedium, color = Sand)
            Text("• ما يقوّي إشارة شركة الاتصالات، بس يقيسها ويدلك على أفضل مكان.\n• ما يتصل بأجهزة اللاسلكي (الووكي توكي) العادية، لأنها تشتغل على موجات راديو الجوال ما يستقبلها.\n• يشتغل بس مع جوالات فيها «نِداء».", color = Muted)
        }
        SectionCard {
            Text("تبي مدى أبعد؟", style = MaterialTheme.typography.titleMedium)
            Text("فيه أجهزة صغيرة اسمها LoRa توصلها بالجوال بالبلوتوث وتشتغل مع تطبيق Meshtastic المجاني، مداها كيلومترات في البر. هذي قطعة إضافية تنشرى منفصلة.", color = Muted)
        }
        SectionCard {
            Text("يشتغل والشاشة مطفية", style = MaterialTheme.typography.titleMedium)
            Text("لما تضغط «تشغيل» يطلع إشعار ثابت «نِداء شغّال»، ويظل يستقبل الرسائل والاستغاثة والصوت ويمرّرها حتى لو قفلت الشاشة. توقفه من زر «إيقاف» في الإشعار أو من فوق.", color = Muted)
            Spacer(Modifier.height(6.dp))
            Text("هواوي: الإعدادات ← البطارية ← تشغيل التطبيقات ← نِداء ← طفّ «إدارة تلقائية» وفعّل: التشغيل التلقائي، التشغيل الثانوي، التشغيل في الخلفية.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp)) {
                OutlinedButton(onClick = { a.openSettings("huawei") }) { Text("تشغيل التطبيقات (هواوي)") }
                OutlinedButton(onClick = { a.openSettings("battery") }) { Text("البطارية") }
                OutlinedButton(onClick = { a.openSettings("notifications") }) { Text("الإشعارات") }
            }
        }
        var confirmClear by remember { mutableStateOf(false) }
        SectionCard {
            Text("السجل والخصوصية", style = MaterialTheme.typography.titleMedium)
            Text("المحادثات والاستغاثات وسجل الإشارة محفوظة في جوالك بس، وما تنرسل لأي سيرفر.", color = Muted)
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Icon(Icons.Filled.DeleteSweep, null); Spacer(Modifier.width(6.dp)); Text("مسح كل السجل") }
        }
        if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, containerColor = Card, title = { Text("تمسح كل السجل؟") },
            text = { Text("بتنمسح كل المحادثات والاستغاثات وسجل الإشارة من هالجوال. المجموعات تبقى.") },
            confirmButton = { Button(onClick = { confirmClear = false; a.clearAllHistory() }, colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)) { Text("امسح") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } })
        Text("© 2026 وطن النهار — جميع الحقوق محفوظة\n© 2026 Watan Al-Nahar — All rights reserved", color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

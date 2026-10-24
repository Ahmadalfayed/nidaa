package com.watan.nidaa

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date

@Composable
fun BackBar(title: String, sub: String? = null, actions: @Composable RowScope.() -> Unit = {}, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Night).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Sand) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Gold, maxLines = 1)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
        }
        actions()
    }
}

private val warnText = mapOf(
    "location" to "الموقع (GPS) مطفي — هواوي ما يلقى الأجهزة بالواي فاي المباشر إلا والموقع شغّال",
    "wifi" to "الواي فاي مطفي — شغّله (ما يحتاج تتصل بشبكة)",
    "bt" to "البلوتوث مطفي — شغّله عشان يصير طريقة احتياطية",
    "battery" to "توفير البطارية ممكن يوقف التطبيق — خلّه «بدون قيود»",
)

@Composable
fun WarningCard(st: AppState, a: Actions, onMore: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Danger.copy(.14f)).clickable(onClick = onMore).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        st.warnings.filter { it != "battery" }.forEach { w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Danger, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(warnText[w] ?: w, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { a.openSettings(w) }) { Text("شغّل") }
            }
        }
    }
}

// ------------------------------- groups list -------------------------------
@Composable
fun GroupsScreen(st: AppState, a: Actions, initialDialog: String? = null, createdPreview: Group? = null) {
    var dialog by remember { mutableStateOf(initialDialog) }
    var created by remember { mutableStateOf(createdPreview) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { dialog = "create" }, Modifier.weight(1f).height(52.dp)) { Icon(Icons.Filled.GroupAdd, null); Spacer(Modifier.width(6.dp)); Text("إنشاء مجموعة / قناة") }
                OutlinedButton(onClick = { dialog = "join" }, Modifier.height(52.dp)) { Icon(Icons.Filled.Key, null); Spacer(Modifier.width(6.dp)); Text("انضمام برمز") }
            }
            Text("مجموعاتي (${st.groups.size})", style = MaterialTheme.typography.titleMedium)
            st.groups.forEach { g -> GroupRow(st, g) { a.openChat(g.id) } }
            if (st.nearbyChannels.isNotEmpty()) {
                Spacer(Modifier.height(6.dp)); Text("قنوات عامة قريبة منك", style = MaterialTheme.typography.titleMedium)
                st.nearbyChannels.forEach { g ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, null, tint = Sand); Spacer(Modifier.width(10.dp))
                        Text(g.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { a.joinChannel(g) }) { Text("انضم") }
                    }
                }
            }
            Text("• القناة العامة: أي أحد قريب يقدر يدخلها ويشوفها.\n• المجموعة الخاصة: تدخل برمز + رقم سري، والرسائل والصوت مشفّرة، وما يقراها غير أعضاؤها حتى لو مرّت على جوالات غيرهم.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
    when (dialog) {
        "create" -> CreateGroupDialog(onDismiss = { dialog = null }) { n, p, pin -> created = a.createGroup(n, p, pin); dialog = if (created != null) "created" else null }
        "join" -> JoinGroupDialog(a) { dialog = null }
        "created" -> created?.let { g -> CreatedDialog(g, a) { dialog = null } }
    }
}

@Composable
fun GroupRow(st: AppState, g: Group, onClick: () -> Unit) {
    val last = st.messages.lastOrNull { it.group == g.id }
    val active = st.activeGroup.value == g.id
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).border(if (active) 1.5.dp else 0.dp, if (active) Sand else Color.Transparent, RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (g.private) Dune.copy(.25f) else Sand.copy(.18f)), contentAlignment = Alignment.Center) {
            Icon(if (g.private) Icons.Filled.Lock else Icons.Filled.Campaign, null, tint = Gold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f, false))
                Spacer(Modifier.width(6.dp))
                Text(if (g.private) "خاصة" else "قناة", color = Night, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (g.private) Dune else Sand).padding(horizontal = 6.dp))
                if (active) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.SettingsInputAntenna, "الحالية", tint = Ok, modifier = Modifier.size(16.dp)) }
            }
            Text(last?.let { (if (it.mine) "أنت: " else it.sender + ": ") + it.body } ?: "ما فيه رسائل للحين", color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Icon(Icons.Filled.ChevronLeft, null, tint = Muted)
    }
}

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, initialName: String = "", initialPrivate: Boolean = true, initialPin: String = "", onCreate: (String, Boolean, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var private by remember { mutableStateOf(initialPrivate) }
    var pin by remember { mutableStateOf(initialPin) }
    val pinOk = !private || GroupStore.arabicDigits(pin).length >= 4
    AlertDialog(onDismissRequest = onDismiss, containerColor = Card,
        title = { Text("إنشاء مجموعة أو قناة") },
        text = { CreateGroupFields(name, { name = it }, private, { private = it }, pin, { pin = it }) },
        confirmButton = { Button(onClick = { onCreate(name, private, pin) }, enabled = name.isNotBlank() && pinOk) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}


@Composable
fun CreateGroupFields(name: String, setName: (String) -> Unit, private: Boolean, setPrivate: (Boolean) -> Unit, pin: String, setPin: (String) -> Unit) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { setName(it.take(30)) }, label = { Text("الاسم") }, placeholder = { Text("مثلاً: ربع البر") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip("مجموعة خاصة", Icons.Filled.Lock, private, Modifier.weight(1f)) { setPrivate(true) }
                    TypeChip("قناة عامة", Icons.Filled.Campaign, !private, Modifier.weight(1f)) { setPrivate(false) }
                }
                if (private) {
                    OutlinedTextField(pin, { setPin(it.filter { c -> c.isDigit() }.take(8)) }, label = { Text("رقم سري (٤ أرقام أو أكثر)") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Text("تعطي الرمز والرقم السري لربعك عشان يدخلون. الرسائل والصوت مشفّرة.", color = Muted, style = MaterialTheme.typography.bodySmall)
                } else Text("أي أحد قريب فيه «نِداء» يشوف القناة ويقدر يدخلها.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

@Composable
private fun TypeChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(if (on) Sand else Card2).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (on) Night else Muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
        Text(label, color = if (on) Night else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CreatedDialog(g: Group, a: Actions, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Card,
        title = { Text(if (g.private) "انشأت المجموعة ✅" else "انشأت القناة ✅") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(g.name, style = MaterialTheme.typography.titleLarge, color = Gold)
                if (g.private) {
                    Spacer(Modifier.height(8.dp)); Text("رمز الانضمام", color = Muted)
                    Text(ltr(g.code), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Cairo, letterSpacing = 4.sp, color = Gold)
                    Text("عطه لربعك مع الرقم السري", color = Muted, style = MaterialTheme.typography.bodySmall)
                } else Text("بتظهر لكل من حولك في «قنوات عامة قريبة».", color = Muted, textAlign = TextAlign.Center)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("تمام") } },
        dismissButton = { if (g.private) TextButton(onClick = { a.copy("انضم لمجموعتي «${g.name}» في نِداء — الرمز: ${g.code}") }) { Text("نسخ الرمز") } })
}

@Composable
fun JoinGroupDialog(a: Actions, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Card,
        title = { Text("انضمام لمجموعة خاصة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(code, { code = it.uppercase().take(8) }, label = { Text("رمز المجموعة (٦ خانات)") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pin, { pin = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("الرقم السري") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                err?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { Button(onClick = { err = a.joinGroup(code, pin); if (err == null) onDismiss() }) { Text("انضم") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

// ------------------------------- group chat -------------------------------
@Composable
fun GroupChatScreen(st: AppState, a: Actions, g: Group) {
    var menu by remember { mutableStateOf(false) }; var confirmLeave by remember { mutableStateOf(false) }; var info by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Night).statusBarsPadding().navigationBarsPadding().imePadding()) {
        BackBar(g.name, (if (g.private) "مجموعة خاصة · رمز ${ltr(g.code)}" else "قناة عامة") + " · ${st.peers.size} متصل",
            actions = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Muted) }
                DropdownMenu(menu, { menu = false }, containerColor = Card2) {
                    DropdownMenuItem(text = { Text("معلومات / الرمز") }, leadingIcon = { Icon(Icons.Filled.Info, null) }, onClick = { menu = false; info = true })
                    DropdownMenuItem(text = { Text("مسح المحادثة") }, leadingIcon = { Icon(Icons.Filled.DeleteSweep, null) }, onClick = { menu = false; a.clearChat(g.id) })
                    if (!g.builtIn) DropdownMenuItem(text = { Text("مغادرة", color = Danger) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Danger) }, onClick = { menu = false; confirmLeave = true })
                }
            }) { a.openChat(null) }
        if (st.incomingFrom.value != null) Row(Modifier.fillMaxWidth().background(Ok.copy(.15f)).padding(8.dp), horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.VolumeUp, null, tint = Ok); Spacer(Modifier.width(6.dp)); Text("يتكلم الحين: ${st.incomingFrom.value}", color = Ok)
        }
        ChatBody(st, a, g, Modifier.weight(1f))
    }
    if (info) CreatedDialog(g, a) { info = false }
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, containerColor = Card,
        title = { Text("تغادر «${g.name}»؟") }, text = { Text(if (g.private) "تقدر ترجع لها بالرمز والرقم السري." else "تقدر ترجع لها من «قنوات عامة قريبة».") },
        confirmButton = { Button(onClick = { confirmLeave = false; a.leaveGroup(g.id) }, colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)) { Text("غادر") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("إلغاء") } })
}

// ------------------------------- connection / peers -------------------------------
private val transportNames = mapOf("wifi" to ("الواي فاي المباشر" to Icons.Filled.WifiTethering), "lan" to ("الهوتسبوت / نفس الشبكة" to Icons.Filled.Router), "bt" to ("البلوتوث" to Icons.Filled.Bluetooth))

@Composable
fun ConnectionScreen(st: AppState, a: Actions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(if (st.peers.isNotEmpty()) Ok else if (st.meshOn.value) Gold else Muted)); Spacer(Modifier.width(10.dp))
                Text(when { !st.meshOn.value -> "متوقف — اضغط «تشغيل» فوق"; st.peers.isEmpty() -> "يبحث عن أجهزة قريبة…"; else -> "متصل بـ ${st.peers.size} جهاز مباشرة" },
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            listOf("wifi", "lan", "bt").forEach { k ->
                val t = st.transports.firstOrNull { it.name == k }; val (label, icon) = transportNames[k]!!
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if (t?.ok == true) Ok else if (t == null) Muted else Danger, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(t?.text ?: "—", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (st.warnings.isNotEmpty()) WarningCardAll(st, a)
        SectionCard {
            Text("الأجهزة القريبة (${st.peers.size})", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp))
            if (st.peers.isEmpty()) Text("ما فيه أحد متصل للحين.", color = Muted)
            st.peers.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Ok.copy(.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PhoneAndroid, null, tint = Ok, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Text(p.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(p.via, color = Sand, style = MaterialTheme.typography.labelMedium)
                }
            }
            val far = st.meshNodes.values.filter { n -> st.peers.none { it.id == n.id } && System.currentTimeMillis() - n.lastSeen < 60_000 }
            if (far.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Card2)
                Text("أبعد (توصلهم رسايلك عن طريق غيرهم)", color = Muted, style = MaterialTheme.typography.bodySmall)
                far.forEach { n -> Text("• ${n.name} — عبر ${n.hops - 1} جوال", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.TipsAndUpdates, null, tint = Gold); Spacer(Modifier.width(8.dp)); Text("إذا ما ظهر أحد", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(6.dp))
            Text(
                "١. شغّل الواي فاي والموقع (GPS) والبلوتوث في الجوالين، وما يحتاج إنترنت.\n" +
                "٢. افتح «نِداء» في الجوالين واضغط «تشغيل»، وخلّ الشاشة شغالة والتطبيق مفتوح.\n" +
                "٣. إذا طلع لك إشعار «دعوة اتصال واي فاي مباشر» اضغط «قبول».\n" +
                "٤. هواوي: الإعدادات ← التطبيقات ← نِداء ← الأذونات: اسمح بالموقع «دائماً/أثناء الاستخدام» والأجهزة القريبة.\n" +
                "٥. هواوي: البطارية ← تشغيل التطبيقات ← نِداء ← إدارة يدوية وفعّل كل الخيارات، عشان يشتغل والشاشة مطفية.\n" +
                "٦. أسهل طريقة مضمونة: جوال واحد يشغّل «نقطة اتصال/هوتسبوت»، والباقين يتصلون فيها بالواي فاي (بدون إنترنت عادي) — يتصلون خلال ثواني.",
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                OutlinedButton(onClick = { a.openSettings("app") }) { Text("أذونات التطبيق") }
                OutlinedButton(onClick = { a.openSettings("huawei") }) { Text("تشغيل التطبيقات (هواوي)") }
                OutlinedButton(onClick = { a.openSettings("battery") }) { Text("البطارية") }
                OutlinedButton(onClick = { a.makeDiscoverable() }) { Text("أظهر جوالي بالبلوتوث") }
                OutlinedButton(onClick = { a.openSettings("hotspot") }) { Text("الهوتسبوت") }
            }
        }
        Text("رقم جهازك: ${ltr(st.nodeId.value.take(6))}", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WarningCardAll(st: AppState, a: Actions) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Danger.copy(.14f)).padding(12.dp)) {
        st.warnings.forEach { w ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Danger, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(warnText[w] ?: w, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { a.openSettings(w) }) { Text(if (w == "battery") "اضبط" else "شغّل") }
            }
        }
    }
}

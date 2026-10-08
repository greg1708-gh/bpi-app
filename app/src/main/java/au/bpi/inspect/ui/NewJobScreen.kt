package au.bpi.inspect.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.Contact
import au.bpi.inspect.data.J
import au.bpi.inspect.data.Job
import au.bpi.inspect.data.Templates
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun NewJobScreen(nav: NavHostController, jobId: Long, presetClientId: Long, startNow: Boolean) {
    val app = BpiApp.instance
    val db = app.db
    val prefs = app.prefs
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by db.contacts().all().collectAsState(initial = emptyList())

    var loaded by remember { mutableStateOf(jobId == 0L) }
    var purpose by remember { mutableStateOf("Pre-purchase") }
    var scopeSel by remember { mutableStateOf("Combined") }
    var special by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf(if (presetClientId > 0) presetClientId else null as Long?) }
    var agentId by remember { mutableStateOf(null as Long?) }
    var day by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var start by remember { mutableIntStateOf(8 * 60 + 30) }
    var dur by remember { mutableStateOf(null as Int?) }
    var cal by remember { mutableStateOf(prefs.addToCalendar.value && !startNow) }
    var agreed by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf(null as Job?) }

    // A preset contact who isn't a client (an agent, say) goes in the referrer slot.
    LaunchedEffect(presetClientId, contacts.size) {
        if (presetClientId > 0) {
            val c = contacts.firstOrNull { it.id == presetClientId }
            if (c != null && c.type != "Client") { agentId = c.id; clientId = null }
        }
    }
    LaunchedEffect(jobId) {
        if (jobId > 0) db.jobs().get(jobId)?.let { j ->
            existing = j
            purpose = j.purpose; scopeSel = if (j.scope == "General") "Combined" else j.scope; special = j.specialScope
            address = j.address; clientId = j.clientId; agentId = j.agentId; day = j.dateEpochDay; start = j.startMin
            dur = j.durationMin; agreed = j.agreementAccepted; cal = false
            loaded = true
        }
    }

    val general = Templates.isGeneralPurpose(purpose)
    val finalScope = if (general) "General" else scopeSel
    val defaultDur = prefs.defaultDuration(purpose, scopeSel)
    val duration = dur ?: defaultDur
    val finish = start + duration

    // Double-booking check against other jobs that day, allowing the travel buffer.
    var clash by remember { mutableStateOf(null as Job?) }
    LaunchedEffect(day, start, duration) {
        val buf = prefs.travelBuffer.intValue
        clash = db.jobs().onDay(day, jobId).firstOrNull { o -> start < o.startMin + o.durationMin + buf && finish + buf > o.startMin }
    }

    val client = contacts.firstOrNull { it.id == clientId }
    val agent = contacts.firstOrNull { it.id == agentId }
    val canSave = address.isNotBlank()

    fun save(open: Boolean) {
        scope.launch {
            val base = existing ?: Job(
                address = "", dateEpochDay = day,
                areaOrderJson = J.list(prefs.areaOrder.value),
                roomsJson = J.list(Templates.defaultRooms)
            )
            val job = base.copy(
                address = address.trim(), purpose = purpose, scope = finalScope, specialScope = if (purpose == "Special purpose") special.trim() else "",
                clientId = clientId, agentId = agentId, dateEpochDay = day, startMin = start, durationMin = duration,
                agreementAccepted = agreed,
                status = if (open && base.status == "Booked") "In progress" else base.status
            )
            val rid = db.jobs().upsert(job)
            val id = if (rid > 0) rid else job.id
            if (cal) {
                Actions.addToCalendar(
                    ctx, job.copy(id = id),
                    client?.let { "${it.name} · ${it.phone}" } ?: "",
                    agent?.let { listOf(it.name, it.company, it.phone).filter { s -> s.isNotBlank() }.joinToString(" · ") } ?: ""
                )
            }
            if (open) nav.navigate("job/$id") { popUpTo("jobs") }
            else nav.popBackStack()
        }
    }

    Screen(
        title = when { jobId > 0 -> "Edit booking"; startNow -> "Start inspection now"; else -> "New inspection" },
        onBack = { nav.popBackStack() },
        bottom = {
            BottomBar {
                when {
                    startNow -> PrimaryButton("Start inspection", Modifier.weight(1f), enabled = canSave) { save(true) }
                    jobId > 0 -> PrimaryButton(if (cal) "Save & update calendar" else "Save changes", Modifier.weight(1f), enabled = canSave) { save(false) }
                    else -> PrimaryButton(if (cal) "Book & add to calendar" else "Save booking", Modifier.weight(1f), enabled = canSave) { save(false) }
                }
            }
        }
    ) {
        if (!loaded) { Text("Loading…", color = C.Muted); return@Screen }

        SectionLabel("Inspection purpose")
        ChoiceChips(Templates.purposes, { it == purpose }, { purpose = it; dur = null }) { C.AccentSoft to C.AccentInk }
        if (purpose == "Special purpose") {
            Field("Special purpose scope", special, { special = it }, lines = 3, placeholder = "What the client asked you to inspect or report on")
        }

        SectionLabel("Scope")
        if (general) {
            Column(Modifier.fillMaxWidth().background(C.Ink, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text(Templates.AS_GENERAL, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Inspection of buildings: general requirements. Set by the ${purpose.lowercase()} purpose.", color = Color(0xFFD6D8DD), fontSize = 13.sp)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Building" to Templates.AS_BUILDING,
                    "Timber pest" to Templates.AS_PEST,
                    "Combined" to Templates.AS_BUILDING + "\n" + Templates.AS_PEST
                ).forEach { (label, std) ->
                    val on = scopeSel == label
                    Column(
                        Modifier.weight(1f).heightIn(min = 76.dp)
                            .background(if (on) C.Ink else Color.White, RoundedCornerShape(12.dp))
                            .border(if (on) 2.dp else 1.dp, if (on) C.Ink else C.Field, RoundedCornerShape(12.dp))
                            .clickable { scopeSel = label; dur = null }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (on) Color.White else C.Ink)
                        Text(std, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = if (on) Color.White else C.Ink2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        SectionLabel("Property")
        Field("Address", address, { address = it }, placeholder = "Street, suburb, postcode")

        SectionLabel("Client & contacts")
        ContactPicker("Client (report goes to their email)", contacts.filter { it.type == "Client" }, client, { clientId = it }, nav, "Client")
        ContactPicker("Agent / conveyancer / referrer (optional)", contacts.filter { it.type != "Client" }, agent, { agentId = it }, nav, "Agent")

        SectionLabel("Booking")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                fmtDay(day), fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, C.Field, RoundedCornerShape(12.dp))
                    .clickable {
                        val d = LocalDate.ofEpochDay(day)
                        DatePickerDialog(ctx, { _, y, m, dd -> day = LocalDate.of(y, m + 1, dd).toEpochDay() }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                    }.padding(horizontal = 14.dp, vertical = 14.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val times = (6 * 60..18 * 60 step 15).map { fmtTime(it) }
            Dropdown("Start", times, fmtTime(start), { t -> start = t.substring(0, 2).toInt() * 60 + t.substring(3).toInt() }, Modifier.weight(1f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Finish", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(fmtTime(finish), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().background(C.LineSoft, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 16.dp))
            }
        }
        Text(
            "Duration " + if (dur != null) "(changed for this job)" else "(default for " + (if (general) "AS 4349.0 inspections" else scopeSel.lowercase()) + ")",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold
        )
        ChoiceChips(listOf(60, 90, 120, 150, 180, 240).map { fmtDur(it) }, { it == fmtDur(duration) }, { s ->
            dur = listOf(60, 90, 120, 150, 180, 240).first { fmtDur(it) == s }
        })
        clash?.let { o ->
            Notice("Overlaps ${o.address} (${fmtTime(o.startMin)}–${fmtTime(o.startMin + o.durationMin)}) once your ${prefs.travelBuffer.intValue} min travel buffer is allowed for.")
        }
        CardBox {
            ToggleRow(
                if (jobId > 0) "Update Google Calendar" else "Add to Google Calendar",
                "Opens Google Calendar with this booking filled in, ready to save",
                cal
            ) { cal = it }
            HorizontalDivider(color = C.LineSoft)
            Row(Modifier.fillMaxWidth().clickable { agreed = !agreed }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                Column {
                    Text("Pre-inspection agreement & T&Cs accepted", fontWeight = FontWeight.SemiBold)
                    Text("Scope, limitations and fee agreed before inspecting", color = C.Muted, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Pick a saved contact by searching, or jump to add a new one. */
@Composable
private fun ContactPicker(label: String, pool: List<Contact>, selected: Contact?, onPick: (Long?) -> Unit, nav: NavHostController, newType: String) {
    var q by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (selected != null) {
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).border(2.dp, C.Ink, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(selected.name, selected.type, 40)
                Column(Modifier.weight(1f)) {
                    Text(selected.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(listOf(selected.company, selected.phone, selected.email).filter { it.isNotBlank() }.joinToString(" · "), color = C.Muted, fontSize = 13.sp)
                }
                TextButton(onClick = { onPick(null) }) { Text("Change", color = C.Ink, fontWeight = FontWeight.Bold) }
            }
        } else {
            Field("", q, { q = it }, placeholder = "Search your contacts")
            CardBox {
                pool.filter { q.isBlank() || (it.name + " " + it.phone + " " + it.email + " " + it.company).contains(q, true) }.take(5).forEach { c ->
                    ListRow(c.name, listOf(c.type, c.company, c.phone).filter { it.isNotBlank() }.joinToString(" · "), trailing = {}) { onPick(c.id) }
                }
                Text(
                    "+ Add new contact", color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().clickable { nav.navigate("contactEdit/0?type=$newType") }.padding(14.dp)
                )
            }
        }
    }
}

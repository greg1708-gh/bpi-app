package au.bpi.inspect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.Contact
import au.bpi.inspect.data.Templates
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(nav: NavHostController) {
    val db = BpiApp.instance.db
    val all by db.contacts().all().collectAsState(initial = emptyList())
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val rows = all.filter { c ->
        (filter == "All" || c.type == filter) &&
            (q.isBlank() || listOf(c.name, c.phone, c.email, c.company, c.address).any { it.contains(q, ignoreCase = true) })
    }
    Screen(
        title = "Clients & contacts",
        actions = {
            TextButton(onClick = { nav.navigate("contactEdit/0") }) {
                Icon(Icons.Filled.Add, null, tint = C.Accent); Text("Add", color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        headerExtra = {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("", q, { q = it }, placeholder = "Search name, phone, email or address")
                Segmented(listOf("All", "Client", "Agent", "Conveyancer"), filter, { filter = it }) {
                    when (it) { "All" -> "All"; "Conveyancer" -> "Legal"; else -> it + "s" }
                }
            }
        },
        bottom = { HomeNav(nav, "Clients") }
    ) {
        Text("${rows.size} contact" + if (rows.size == 1) "" else "s", color = C.Muted, fontSize = 13.sp)
        if (rows.isEmpty()) EmptyState(if (all.isEmpty()) "No contacts yet. Tap Add to save your first client, agent or conveyancer." else "No matches.")
        else CardBox {
            rows.forEach { c ->
                ListRow(
                    title = c.name,
                    detail = listOf(c.company, c.phone, c.email).filter { it.isNotBlank() }.joinToString(" · "),
                    leading = { Avatar(c.name, c.type) },
                    trailing = { Badge(if (c.type == "Conveyancer") "Legal" else c.type, C.LineSoft, C.Ink2) }
                ) { nav.navigate("contact/${c.id}") }
            }
        }
    }
}

@Composable
fun ContactDetailScreen(nav: NavHostController, id: Long) {
    val db = BpiApp.instance.db
    val ctx = LocalContext.current
    val c by db.contacts().byId(id).collectAsState(initial = null)
    val jobs by db.jobs().forContact(id).collectAsState(initial = emptyList())
    val contact = c ?: run { Screen("Contact", onBack = { nav.popBackStack() }) { }; return }
    Screen(
        title = contact.name, subtitle = if (contact.type == "Conveyancer") "Conveyancer / solicitor" else contact.type,
        onBack = { nav.popBackStack() },
        actions = { TextButton(onClick = { nav.navigate("contactEdit/${contact.id}") }) { Text("Edit", fontWeight = FontWeight.Bold, color = C.Ink, fontSize = 16.sp) } },
        bottom = {
            BottomBar {
                PrimaryButton(
                    if (contact.type == "Client") "New inspection for ${contact.name}" else "New inspection referred by ${contact.name}",
                    Modifier.weight(1f), icon = Icons.Filled.Add
                ) { nav.navigate("newJob?clientId=${contact.id}") }
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("Call", Icons.Filled.Call, Modifier.weight(1f), contact.phone.isNotBlank()) { Actions.call(ctx, contact.phone) }
            QuickAction("Text", Icons.Filled.Sms, Modifier.weight(1f), contact.phone.isNotBlank()) { Actions.sms(ctx, contact.phone) }
            QuickAction("Email", Icons.Filled.Email, Modifier.weight(1f), contact.email.isNotBlank()) { Actions.email(ctx, contact.email) }
        }
        CardBox {
            listOf(
                "Phone" to contact.phone, "Email" to contact.email, "Company" to contact.company,
                "Postal address" to contact.address, "Notes" to contact.notes,
                "Copy reports" to if (contact.copyReports) "Yes, with client permission" else "No"
            ).filter { it.second.isNotBlank() }.forEach { (k, v) ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(k.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted, letterSpacing = 0.5.sp)
                    Text(v, fontSize = 16.sp)
                }
            }
        }
        SectionLabel("Inspections (${jobs.size})")
        if (jobs.isEmpty()) Text("No inspections yet.", color = C.Muted)
        else CardBox {
            jobs.forEach { j ->
                ListRow(j.address, "${j.purpose} · ${Templates.scopeLabel(j)} · ${fmtDay(j.dateEpochDay)} · ${j.status}") { nav.navigate("job/${j.id}") }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier.height(64.dp).background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (enabled) C.Ink else C.Field)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) C.Ink else C.Field)
    }
}

@Composable
fun ContactEditScreen(nav: NavHostController, id: Long, presetType: String) {
    val db = BpiApp.instance.db
    val scope = rememberCoroutineScope()
    val existing by (if (id > 0) db.contacts().byId(id) else flowOf(null)).collectAsState(initial = null)
    var loaded by remember { mutableStateOf(id == 0L) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(presetType) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf(false) }
    LaunchedEffect(existing) {
        val e = existing
        if (!loaded && e != null) {
            name = e.name; type = e.type; phone = e.phone; email = e.email; company = e.company; address = e.address; notes = e.notes; cc = e.copyReports
            loaded = true
        }
    }
    Screen(
        title = if (id > 0) "Edit contact" else "New contact", onBack = { nav.popBackStack() },
        bottom = {
            BottomBar {
                if (id > 0) SecondaryButton("Delete", Modifier.weight(1f)) {
                    scope.launch { existing?.let { db.contacts().delete(it) }; nav.popBackStack("clients", inclusive = false) }
                }
                PrimaryButton("Save contact", Modifier.weight(1.6f), enabled = name.isNotBlank()) {
                    scope.launch {
                        val c = Contact(id = id, name = name.trim(), type = type, phone = phone.trim(), email = email.trim(), company = company.trim(), address = address.trim(), notes = notes.trim(), copyReports = cc)
                        db.contacts().upsert(c)
                        nav.popBackStack()
                    }
                }
            }
        }
    ) {
        SectionLabel("Contact type")
        ChoiceChips(Templates.contactTypes, { it == type }, { type = it }) { C.Ink to Color.White }
        Field("Full name", name, { name = it }, placeholder = "First and last name")
        Field("Mobile / phone", phone, { phone = it }, keyboard = KeyboardType.Phone)
        Field("Email (reports are sent here)", email, { email = it }, keyboard = KeyboardType.Email)
        Field("Agency / firm (optional)", company, { company = it })
        Field("Postal address (optional)", address, { address = it })
        Field("Notes", notes, { notes = it }, lines = 3, placeholder = "e.g. prefers calls after 4pm")
        CardBox {
            Row(Modifier.fillMaxWidth().clickable { cc = !cc }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = cc, onCheckedChange = { cc = it })
                Column {
                    Text("Copy reports to this contact", fontWeight = FontWeight.SemiBold)
                    Text("Only with the client's permission", color = C.Muted, fontSize = 14.sp)
                }
            }
        }
    }
}

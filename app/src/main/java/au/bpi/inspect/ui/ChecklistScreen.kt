package au.bpi.inspect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.ItemResult
import au.bpi.inspect.data.J
import au.bpi.inspect.data.Job
import au.bpi.inspect.data.Templates
import kotlinx.coroutines.launch

private fun areaTitle(area: String) = when (area) {
    Templates.PEST_AREA -> "Timber pest activity & damage"
    Templates.COND_AREA -> "Conducive conditions"
    else -> area
}

/** Rating that a finding defaults to, from the item's rating. */
fun clsFromRating(area: String, rating: String): String = when {
    area == Templates.COND_AREA -> "Cond"
    area == Templates.PEST_AREA -> "Pest"
    rating in listOf("Major", "Minor", "Safety") -> rating
    else -> "Minor"
}

@Composable
fun ChecklistScreen(nav: NavHostController, jobId: Long, area: String, roomArg: String) {
    val db = BpiApp.instance.db
    val scope = rememberCoroutineScope()
    val jobState by db.jobs().byId(jobId).collectAsState(initial = null)
    val results by db.results().forJob(jobId).collectAsState(initial = emptyList())
    val findings by db.findings().forJob(jobId).collectAsState(initial = emptyList())
    var room by remember { mutableStateOf(roomArg) }
    var addRoom by remember { mutableStateOf(false) }
    val job = jobState ?: run { Screen("Loading…", onBack = { nav.popBackStack() }) { }; return }

    val isInterior = area == "Interior"
    val rooms = J.list(job.roomsJson)
    if (isInterior && room.isEmpty() && rooms.isNotEmpty()) room = rooms.first()
    val items = if (isInterior) Templates.roomItems(room) else Templates.areaItems[area].orEmpty()
    val ratings = Templates.ratingsFor(area)
    val here = results.filter { it.area == area && it.room == (if (isInterior) room else "") }

    fun rate(item: String, rating: String) = scope.launch {
        val cur = here.firstOrNull { it.item == item }
        val r = if (isInterior) room else ""
        if (cur?.rating == rating) db.results().clear(jobId, area, r, item)
        else db.results().upsert(ItemResult(jobId, area, r, item, rating, cur?.niReason ?: "", cur?.niNote ?: ""))
        if (job.status == "Booked") db.jobs().upsert(job.copy(status = "In progress"))
    }
    fun setReason(item: String, reason: String, note: String) = scope.launch {
        db.results().upsert(ItemResult(jobId, area, if (isInterior) room else "", item, "N/I", reason, note))
    }

    // What "Next" goes to: the next room, else back to the areas list.
    val nextRoom = if (isInterior) rooms.getOrNull(rooms.indexOf(room) + 1) else null

    Screen(
        title = if (isInterior) room.ifEmpty { "Interior" } else areaTitle(area),
        subtitle = if (isInterior) "Interior · ${rooms.size} rooms" else job.address,
        onBack = { nav.popBackStack() },
        headerExtra = {
            if (isInterior) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        ScrollChips(rooms, room, { room = it }) { r ->
                            val its = Templates.roomItems(r)
                            its.all { i -> results.any { it.area == area && it.room == r && it.item == i } }
                        }
                    }
                    TextButton(onClick = { addRoom = true }) { Text("+ Room", color = C.Accent, fontWeight = FontWeight.Bold) }
                }
            }
        },
        bottom = {
            Column {
                BottomBar {
                    SecondaryButton("Add finding", Modifier.weight(1f), icon = Icons.Filled.CameraAlt) {
                        nav.navigate(Routes.finding(jobId, 0, area, if (isInterior) room else "", "", clsFromRating(area, "Minor")))
                    }
                    if (nextRoom != null) PrimaryButton("Next: $nextRoom", Modifier.weight(1f), color = C.Ink) { room = nextRoom }
                    else PrimaryButton("Done", Modifier.weight(1f), color = C.Ink) { nav.popBackStack() }
                }
                JobNav(nav, jobId, "")
            }
        }
    ) {
        if (isInterior && rooms.isEmpty()) EmptyState("No rooms yet. Tap + Room to add one.")
        items.forEach { item ->
            val res = here.firstOrNull { it.item == item }
            val itemFindings = findings.filter { it.area == area && it.room == (if (isInterior) room else "") && it.item == item }
            ItemCard(
                item = item, ratings = ratings, current = res?.rating,
                onRate = { rate(item, it) },
                niReason = res?.niReason ?: "", niNote = res?.niNote ?: "",
                onReason = { r, n -> setReason(item, r, n) },
                roomLabel = if (isInterior) "$item ($room)" else item,
                findings = itemFindings.map { it.id to (it.text.ifBlank { "Finding" }.take(90) + (J.list(it.tradesJson).firstOrNull()?.let { t -> "\nRefer to: $t" } ?: "")) },
                onOpenFinding = { fid -> nav.navigate(Routes.finding(jobId, fid)) },
                onAddFinding = { nav.navigate(Routes.finding(jobId, 0, area, if (isInterior) room else "", item, clsFromRating(area, res?.rating ?: ""))) }
            )
        }
    }

    if (addRoom) AddRoomDialog(rooms, onDismiss = { addRoom = false }) { name, remove ->
        val list = if (remove) rooms - name else rooms + name
        scope.launch { db.jobs().upsert(job.copy(roomsJson = J.list(list))) }
        if (!remove) room = name else if (room == name) room = list.firstOrNull() ?: ""
        addRoom = false
    }
}

@Composable
private fun ItemCard(
    item: String, ratings: List<String>, current: String?, onRate: (String) -> Unit,
    niReason: String, niNote: String, onReason: (String, String) -> Unit, roomLabel: String,
    findings: List<Pair<Long, String>>, onOpenFinding: (Long) -> Unit, onAddFinding: () -> Unit
) {
    CardBox(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Text(item, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ratings.forEach { r ->
                val on = current == r
                val (bg, fg) = if (on) ratingColors(r) else (Color.White to C.Ink2)
                Box(
                    Modifier.weight(1f).height(46.dp).background(bg, RoundedCornerShape(10.dp))
                        .border(if (on) 2.dp else 1.dp, if (on && r == "Safety") C.Ink else if (on) bg else C.Field, RoundedCornerShape(10.dp))
                        .clickable { onRate(r) },
                    contentAlignment = Alignment.Center
                ) { Text(r, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg, textAlign = TextAlign.Center) }
            }
        }
        if (current == "N/I") {
            var note by remember(niNote) { mutableStateOf(niNote) }
            Column(Modifier.padding(top = 10.dp).fillMaxWidth().background(C.LineSoft, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Why wasn't it inspected?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Ink2)
                ChoiceChips(Templates.niReasons, { it == niReason }, { onReason(it, note) })
                Field("Note (optional)", note, { note = it; onReason(niReason, it) }, placeholder = "e.g. fridge and stored boxes against rear wall")
                Text(
                    if (niReason.isNotBlank()) "✓ Added to report limitations: $roomLabel not inspected – ${niReason.lowercase()}" + (if (note.isNotBlank()) "; $note" else "") + "."
                    else "Pick a reason so it can be listed in the report limitations.",
                    fontSize = 12.sp, color = C.Ink2
                )
            }
        }
        findings.forEach { (id, text) ->
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth().background(C.Ground, RoundedCornerShape(10.dp)).clickable { onOpenFinding(id) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, null, tint = C.Ink2, modifier = Modifier.size(20.dp))
                Text(text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("›", fontSize = 20.sp, color = C.NI)
            }
        }
        val needsFinding = current != null && current !in listOf("OK", "None found", "N/I")
        if (needsFinding || findings.isNotEmpty()) {
            Text(
                if (findings.isEmpty()) "+ Add finding & photos for this item" else "+ Add another finding",
                color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onAddFinding).padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun AddRoomDialog(rooms: List<String>, onDismiss: () -> Unit, onPick: (String, Boolean) -> Unit) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rooms") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tap to add a room. Tap a ticked room to remove it.", color = C.Muted, fontSize = 13.sp)
                val all = (Templates.roomSuggestions + rooms).distinct()
                ChoiceChips(all, { it in rooms }, { r -> onPick(r, r in rooms) })
                Field("Other room", custom, { custom = it }, placeholder = "e.g. Bedroom 5, Media room")
            }
        },
        confirmButton = { TextButton(enabled = custom.isNotBlank(), onClick = { onPick(custom.trim(), false) }) { Text("Add room") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Suppress("unused")
private fun jobRooms(job: Job) = J.list(job.roomsJson)

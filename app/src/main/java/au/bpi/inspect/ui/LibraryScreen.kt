package au.bpi.inspect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.J
import au.bpi.inspect.data.LibComment
import kotlinx.coroutines.launch

private val areaOrder = listOf("Site", "Roof exterior", "Exterior", "Interior", "Roof space", "Subfloor", "Timber pest", "Conducive")

@Composable
fun LibraryScreen(nav: NavHostController, pick: Boolean, areaArg: String, subArg: String, jobId: Long) {
    val app = BpiApp.instance
    val db = app.db
    val scope = rememberCoroutineScope()
    val all by db.library().all().collectAsState(initial = emptyList())
    val areas = (areaOrder + all.map { it.area }).distinct().filter { a -> all.any { it.area == a } || a in areaOrder }
    var area by remember { mutableStateOf(areaArg.ifBlank { "Interior" }.let { if (it in areas) it else "Interior" }) }
    var open by remember { mutableStateOf(setOf(subArg)) }
    var q by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(null as LibComment?) }

    fun insert(c: LibComment) {
        nav.previousBackStackEntry?.savedStateHandle?.set(PICKED, listOf(c.cls, c.text, c.tradesJson).joinToString(SEP))
        nav.popBackStack()
    }

    Screen(
        title = "Comment library",
        subtitle = if (pick) "Tap Insert to use a comment" else "Insert, edit or add your own",
        onBack = if (pick || jobId > 0) ({ nav.popBackStack() }) else null,
        actions = {
            TextButton(onClick = { editing = LibComment(id = -1, area = area, sub = all.firstOrNull { it.area == area }?.sub ?: "", cls = if (area == "Conducive") "Cond" else "Minor", text = "") }) {
                Icon(Icons.Filled.Add, null, tint = C.Accent); Text("Add", color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        headerExtra = {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("", q, { q = it }, placeholder = "Search e.g. leak, balustrade, stump")
                if (q.isBlank()) ScrollChips(areas, area, { area = it; open = emptySet() })
            }
        },
        bottom = { if (jobId > 0 && !pick) JobNav(nav, jobId, "Library") else if (!pick) HomeNav(nav, "Library") }
    ) {
        if (q.isNotBlank()) {
            val hits = all.filter { (it.text + " " + it.sub + " " + J.list(it.tradesJson).joinToString(" ")).contains(q, true) }
            Text("${hits.size} matches across all areas", color = C.Muted, fontSize = 13.sp)
            hits.forEach { c -> CommentCard(c, "${c.area} › ${c.sub}", pick, { insert(c) }, { editing = c }) }
        } else {
            val subs = all.filter { it.area == area }.map { it.sub }.distinct()
            subs.forEach { s ->
                val items = all.filter { it.area == area && it.sub == s }
                val isOpen = s in open
                CardBox {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { open = if (isOpen) open - s else open + s }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${items.size}", color = C.Muted, fontSize = 13.sp)
                        Text(if (isOpen) "  ▴" else "  ▾", color = C.Muted, fontSize = 16.sp)
                    }
                    if (isOpen) Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { c -> CommentCard(c, null, pick, { insert(c) }, { editing = c }) }
                        Text(
                            "+ Add comment to $s", fontWeight = FontWeight.SemiBold, color = C.Ink2,
                            modifier = Modifier.fillMaxWidth().border(2.dp, C.Field, RoundedCornerShape(10.dp))
                                .clickable { editing = LibComment(id = -1, area = area, sub = s, cls = if (area == "Conducive") "Cond" else "Minor", text = "") }
                                .padding(14.dp)
                        )
                    }
                }
            }
        }
    }

    editing?.let { e ->
        CommentEditor(e, areas, all, app.trades, onDismiss = { editing = null }, onDelete = {
            scope.launch { db.library().delete(e) }; editing = null
        }) { saved ->
            scope.launch {
                val isNew = e.id <= 0
                db.library().upsert(
                    saved.copy(
                        id = if (isNew) 0 else e.id,
                        flag = if (isNew) "mine" else if (e.flag == "mine") "mine" else "edited",
                        sort = if (isNew) Int.MAX_VALUE else e.sort
                    )
                )
            }
            area = saved.area; open = open + saved.sub; editing = null
        }
    }
}

@Composable
private fun CommentCard(c: LibComment, path: String?, pick: Boolean, onInsert: () -> Unit, onEdit: () -> Unit) {
    val (bg, fg) = ratingColors(c.cls)
    Column(
        Modifier.fillMaxWidth().background(if (c.flag.isNotEmpty()) Color.White else C.Ground, RoundedCornerShape(10.dp))
            .border(if (c.flag.isNotEmpty()) 2.dp else 1.dp, if (c.flag.isNotEmpty()) C.Ink else C.LineSoft, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (path != null) Text(path, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(clsLabel(c.cls), bg, fg)
            if (c.flag == "mine") Badge("Your comment", Color.White, C.Ink)
            if (c.flag == "edited") Badge("Edited", Color.White, C.Ink)
        }
        Text(c.text, fontSize = 15.sp)
        val tr = J.list(c.tradesJson)
        if (tr.isNotEmpty()) Text("Refer to: " + tr.joinToString(", "), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Edit", Modifier.weight(1f).height(44.dp), onClick = onEdit)
            if (pick) PrimaryButton("Insert", Modifier.weight(1f).height(44.dp), color = C.Ink, onClick = onInsert)
        }
    }
}

@Composable
private fun CommentEditor(
    e: LibComment, areas: List<String>, all: List<LibComment>, trades: List<String>,
    onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (LibComment) -> Unit
) {
    var area by remember { mutableStateOf(e.area) }
    var sub by remember { mutableStateOf(e.sub) }
    var newSub by remember { mutableStateOf("") }
    var cls by remember { mutableStateOf(e.cls) }
    var text by remember { mutableStateOf(e.text) }
    var tr by remember { mutableStateOf(J.list(e.tradesJson)) }
    val subs = all.filter { it.area == area }.map { it.sub }.distinct() + "+ New sub-area…"
    val finalSub = if (sub == "+ New sub-area…") newSub.trim() else sub
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id > 0) "Edit library comment" else "New library comment") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Dropdown("Area", areas, area, { area = it; sub = all.firstOrNull { c -> c.area == it }?.sub ?: "+ New sub-area…" })
                Dropdown("Sub-area", subs, sub, { sub = it })
                if (sub == "+ New sub-area…") Field("New sub-area name", newSub, { newSub = it }, placeholder = "e.g. Solar panels")
                Text("Classification", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                ChoiceChips(listOf("Major", "Minor", "Safety", "Pest", "Cond"), { it == cls }, { cls = it }, { ratingColors(it) })
                Field("Comment wording", text, { text = it }, lines = 5, placeholder = "Use [brackets] for details you fill in on site, e.g. [location].")
                Text("Refer to (trades)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                ChoiceChips(trades, { it in tr }, { t -> tr = if (t in tr) tr - t else tr + t })
                if (e.id > 0) TextButton(onClick = onDelete) { Text("Delete this comment", color = C.Accent) }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank() && finalSub.isNotBlank(), onClick = {
                onSave(e.copy(area = area, sub = finalSub, cls = cls, text = text.trim(), tradesJson = J.list(tr)))
            }) { Text("Save to library", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

package au.bpi.inspect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.Finding
import au.bpi.inspect.data.ItemResult
import au.bpi.inspect.data.J
import au.bpi.inspect.data.Job
import au.bpi.inspect.data.Templates
import kotlinx.coroutines.launch

/** Progress of one area: (rated items, total items). */
fun areaProgress(job: Job, area: String, results: List<ItemResult>): Pair<Int, Int> {
    val rated = results.filter { it.area == area }
    return if (area == "Interior") {
        val rooms = J.list(job.roomsJson)
        val total = rooms.sumOf { Templates.roomItems(it).size }
        val done = rooms.sumOf { r -> Templates.roomItems(r).count { i -> rated.any { it.room == r && it.item == i } } }
        done to total
    } else {
        val items = Templates.areaItems[area].orEmpty()
        items.count { i -> rated.any { it.item == i } } to items.size
    }
}

fun findingSummary(fs: List<Finding>): String {
    val parts = listOf("Major" to "major", "Safety" to "safety", "Minor" to "minor", "Pest" to "pest", "Cond" to "conducive")
        .mapNotNull { (k, label) -> fs.count { it.cls == k }.takeIf { it > 0 }?.let { "$it $label" } }
    return parts.joinToString(", ")
}

@Composable
fun JobScreen(nav: NavHostController, jobId: Long) {
    val app = BpiApp.instance
    val db = app.db
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobState by db.jobs().byId(jobId).collectAsState(initial = null)
    val results by db.results().forJob(jobId).collectAsState(initial = emptyList())
    val findings by db.findings().forJob(jobId).collectAsState(initial = emptyList())
    var reorder by remember { mutableStateOf(false) }
    val job = jobState ?: run { Screen("Loading…", onBack = { nav.popBackStack() }) { }; return }

    fun update(j: Job) = scope.launch { db.jobs().upsert(j) }
    val props = J.map(job.propsJson)
    val order = J.list(job.areaOrderJson).ifEmpty { app.prefs.areaOrder.value }
    val stdLabel = when (job.scope) { "General" -> Templates.AS_GENERAL; else -> Templates.AS_BUILDING }

    Screen(
        title = job.address, subtitle = "${job.purpose} · ${Templates.scopeLabel(job)}",
        onBack = { nav.popBackStack() }, dark = true,
        actions = {
            IconButton(onClick = { Actions.directions(ctx, job.address) }) { Icon(Icons.Filled.Directions, "Directions", tint = Color.White) }
        },
        headerExtra = {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(findings.count { it.cls == "Major" }, "Major defects", Color(0xFFFF9A6B), Modifier.weight(1f))
                Stat(findings.count { it.cls == "Safety" }, "Safety hazards", Color(0xFFFFD27A), Modifier.weight(1f))
                Stat(findings.count { it.cls == "Minor" }, "Minor defects", Color.White, Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { nav.navigate("newJob?jobId=${job.id}") }) { Text("Edit booking", color = Color.White) }
                TextButton(onClick = {
                    update(job.copy(status = if (job.status == "Complete") "In progress" else "Complete"))
                }) { Text(if (job.status == "Complete") "Re-open job" else "Mark complete", color = Color.White) }
            }
        },
        bottom = { JobNav(nav, jobId, "Areas") }
    ) {
        SectionLabel("Property details")
        CardBox(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            Templates.propertyFields.chunked(2).forEach { pair ->
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (name, opts) ->
                        Dropdown(name, opts, props[name] ?: "", { v -> update(job.copy(propsJson = J.map(props + (name to v)))) }, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }

        if (Templates.hasBuilding(job)) {
            SectionLabel("Building · $stdLabel") {
                TextButton(onClick = { reorder = !reorder }) { Text(if (reorder) "Done" else "Reorder", color = C.Ink, fontWeight = FontWeight.Bold) }
            }
            CardBox {
                order.forEachIndexed { i, area ->
                    val fs = findings.filter { it.area == area }
                    if (reorder) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(area, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            IconButton(enabled = i > 0, onClick = {
                                val o = order.toMutableList(); o[i] = o[i - 1].also { o[i - 1] = o[i] }
                                update(job.copy(areaOrderJson = J.list(o)))
                            }) { Icon(Icons.Filled.KeyboardArrowUp, "Move $area up") }
                            IconButton(enabled = i < order.size - 1, onClick = {
                                val o = order.toMutableList(); o[i] = o[i + 1].also { o[i + 1] = o[i] }
                                update(job.copy(areaOrderJson = J.list(o)))
                            }) { Icon(Icons.Filled.KeyboardArrowDown, "Move $area down") }
                        }
                        androidx.compose.material3.HorizontalDivider(color = C.LineSoft)
                    } else {
                        val (done, total) = areaProgress(job, area, results)
                        AreaRow(area, done, total, findingSummary(fs)) { nav.navigate(Routes.checklist(job.id, area)) }
                    }
                }
            }
            if (reorder) {
                Text("This order is used on site and in the report.", color = C.Muted, fontSize = 13.sp)
                TextButton(onClick = { app.prefs.setAreaOrder(order) }) { Text("Save as my default order for new jobs", color = C.Accent, fontWeight = FontWeight.Bold) }
            }
        }

        if (Templates.hasPest(job)) {
            SectionLabel("Timber pest · ${Templates.AS_PEST}")
            CardBox {
                listOf(Templates.PEST_AREA, Templates.COND_AREA).forEach { area ->
                    val (done, total) = areaProgress(job, area, results)
                    val label = if (area == Templates.COND_AREA) "Conducive conditions" else "Timber pest activity & damage"
                    AreaRow(label, done, total, findingSummary(findings.filter { it.area == area })) { nav.navigate(Routes.checklist(job.id, area)) }
                }
            }
        }
    }
}

@Composable
private fun Stat(n: Int, label: String, color: Color, modifier: Modifier) {
    Column(modifier.background(Color(0xFF2A2D35), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text("$n", color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        Text(label, color = Color(0xFFD6D8DD), fontSize = 12.sp)
    }
}

@Composable
private fun AreaRow(name: String, done: Int, total: Int, findings: String, onClick: () -> Unit) {
    val state = when { total > 0 && done >= total -> "done"; done > 0 -> "part"; else -> "todo" }
    val detail = buildString {
        append(if (state == "todo") "Not started" else "$done of $total checked")
        if (findings.isNotBlank()) append(" · ").append(findings)
    }
    ListRow(
        name, detail,
        leading = {
            val (bg, fg, border) = when (state) {
                "done" -> Triple(C.Ink, Color.White, C.Ink)
                "part" -> Triple(C.AccentSoft, C.AccentInk, C.Accent)
                else -> Triple(Color.White, C.NI, C.Field)
            }
            Box(Modifier.size(30.dp).background(bg, CircleShape).border(2.dp, border, CircleShape), contentAlignment = Alignment.Center) {
                Text(when (state) { "done" -> "✓"; "part" -> "…"; else -> "" }, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        onClick = onClick
    )
}

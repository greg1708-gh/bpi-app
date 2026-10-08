package au.bpi.inspect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import au.bpi.inspect.data.Templates

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JumpScreen(nav: NavHostController, jobId: Long) {
    val db = BpiApp.instance.db
    val jobState by db.jobs().byId(jobId).collectAsState(initial = null)
    val results by db.results().forJob(jobId).collectAsState(initial = emptyList())
    val findings by db.findings().forJob(jobId).collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf("Areas") }
    var q by remember { mutableStateOf("") }
    val job = jobState ?: run { Screen("Loading…", onBack = { nav.popBackStack() }) { }; return }

    val areas = buildList {
        if (Templates.hasBuilding(job)) addAll(J.list(job.areaOrderJson).ifEmpty { Templates.defaultAreaOrder })
        if (Templates.hasPest(job)) { add(Templates.PEST_AREA); add(Templates.COND_AREA) }
    }

    Screen(
        title = "Jump to", subtitle = "${job.address} · ${job.purpose}",
        onBack = { nav.popBackStack() },
        headerExtra = {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("", q, { q = it }, placeholder = "Room, item or finding")
                Segmented(listOf("Areas", "Findings"), tab, { tab = it }) { if (it == "Findings") "Findings (${findings.size})" else "Areas & rooms" }
            }
        },
        bottom = { JobNav(nav, jobId, "Jump to") }
    ) {
        if (tab == "Areas") {
            areas.forEach { area ->
                val subs = if (area == "Interior") J.list(job.roomsJson) else Templates.areaItems[area].orEmpty()
                val shownSubs = subs.filter { q.isBlank() || it.contains(q, true) || area.contains(q, true) }
                if (q.isNotBlank() && shownSubs.isEmpty()) return@forEach
                val (done, total) = areaProgress(job, area, results)
                CardBox(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.checklist(jobId, area)) }.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(when (area) { Templates.PEST_AREA -> "Timber pest"; Templates.COND_AREA -> "Conducive conditions"; else -> area },
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(if (done == 0) "To do" else if (done >= total) "Done" else "$done of $total", color = C.Muted, fontSize = 13.sp)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shownSubs.forEach { s ->
                            val isRoom = area == "Interior"
                            val checked = if (isRoom) Templates.roomItems(s).all { i -> results.any { it.area == area && it.room == s && it.item == i } }
                                          else results.any { it.area == area && it.item == s }
                            val flagged = findings.any { it.area == area && (if (isRoom) it.room == s else it.item == s) }
                            val (bg, fg, bd) = when {
                                flagged -> Triple(C.AccentSoft, C.AccentInk, C.Accent)
                                checked -> Triple(C.LineSoft, C.Ink2, C.Line)
                                else -> Triple(Color.White, C.Ink, C.Field)
                            }
                            Box(
                                Modifier.heightIn(min = 40.dp).background(bg, RoundedCornerShape(20.dp)).border(1.dp, bd, RoundedCornerShape(20.dp))
                                    .clickable { nav.navigate(Routes.checklist(jobId, area, if (isRoom) s else "")) }.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) { Text(s, fontSize = 14.sp, color = fg) }
                        }
                    }
                }
            }
        } else {
            val groups = listOf("Major" to "Major defects", "Safety" to "Safety hazards", "Pest" to "Timber pest", "Cond" to "Conducive conditions", "Minor" to "Minor defects")
            if (findings.isEmpty()) EmptyState("No findings yet.")
            groups.forEach { (k, label) ->
                val list = findings.filter { it.cls == k && (q.isBlank() || (it.text + it.area + it.room + it.item).contains(q, true)) }
                if (list.isEmpty()) return@forEach
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).background(ratingColors(k).first, RoundedCornerShape(3.dp)))
                    SectionLabel("$label (${list.size})")
                }
                CardBox {
                    list.forEach { f ->
                        ListRow(
                            title = f.text.ifBlank { "Finding" }.take(70),
                            detail = listOf(f.area, f.room, f.item).filter { it.isNotBlank() }.joinToString(" › ") +
                                (J.list(f.tradesJson).takeIf { it.isNotEmpty() }?.let { "\nRefer to: " + it.joinToString(", ") } ?: "")
                        ) { nav.navigate(Routes.finding(jobId, f.id)) }
                    }
                }
            }
        }
    }
}

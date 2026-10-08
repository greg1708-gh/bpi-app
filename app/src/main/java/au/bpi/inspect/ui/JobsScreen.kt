package au.bpi.inspect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.Job
import au.bpi.inspect.data.Templates
import java.time.LocalDate

@Composable
fun JobsScreen(nav: NavHostController) {
    val db = BpiApp.instance.db
    val ctx = LocalContext.current
    val jobs by db.jobs().all().collectAsState(initial = emptyList())
    val contacts by db.contacts().all().collectAsState(initial = emptyList())
    val byId = contacts.associateBy { it.id }
    var tab by remember { mutableStateOf("Today") }
    val today = LocalDate.now().toEpochDay()
    val shown = when (tab) {
        "Today" -> jobs.filter { it.dateEpochDay == today && it.status != "Complete" || it.status == "In progress" }.sortedBy { it.startMin }
        "Upcoming" -> jobs.filter { it.dateEpochDay > today && it.status != "Complete" }.sortedWith(compareBy({ it.dateEpochDay }, { it.startMin }))
        else -> jobs.filter { it.status == "Complete" || (it.dateEpochDay < today && it.status != "In progress") }
    }
    Screen(
        title = "Inspections", subtitle = fmtDay(today),
        headerExtra = { Column(Modifier.padding(top = 10.dp)) { Segmented(listOf("Today", "Upcoming", "Done"), tab, { tab = it }) } },
        bottom = {
            Column {
                BottomBar {
                    PrimaryButton("New inspection", Modifier.weight(1.6f), icon = Icons.Filled.Add) { nav.navigate("newJob") }
                    SecondaryButton("Start now", Modifier.weight(1f)) { nav.navigate("newJob?start=true") }
                }
                HomeNav(nav, "Jobs")
            }
        }
    ) {
        if (shown.isEmpty()) EmptyState(
            when (tab) {
                "Today" -> "Nothing booked for today."
                "Upcoming" -> "No upcoming inspections."
                else -> "No completed inspections yet."
            }
        )
        shown.forEach { j ->
            val client = j.clientId?.let { byId[it] }
            JobCard(
                j, client?.name ?: "No client set",
                showDate = tab != "Today",
                onOpen = { nav.navigate("job/${j.id}") },
                onDirections = { Actions.directions(ctx, j.address) }
            )
        }
    }
}

@Composable
private fun JobCard(j: Job, clientName: String, showDate: Boolean, onOpen: () -> Unit, onDirections: () -> Unit) {
    CardBox {
        Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (showDate) fmtDay(j.dateEpochDay) + " · " else "") + fmtTime(j.startMin) + "–" + fmtTime(j.startMin + j.durationMin),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f)
                )
                val (bg, fg) = when (j.status) {
                    "In progress" -> C.AccentSoft to C.AccentInk
                    "Complete" -> C.Ink to Color.White
                    else -> C.Tint to C.Ink2
                }
                Badge(j.status, bg, fg)
            }
            Text(j.address, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Client: $clientName", fontSize = 14.sp, color = C.Muted)
            Box(Modifier.background(C.Ground, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("${j.purpose} · ${Templates.scopeLabel(j)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Directions", Modifier.weight(1f).height(48.dp), icon = Icons.Filled.Directions, onClick = onDirections)
            PrimaryButton(if (j.status == "In progress") "Continue" else if (j.status == "Complete") "Open" else "Start job", Modifier.weight(1f).height(48.dp), color = C.Ink, onClick = onOpen)
        }
    }
}

package au.bpi.inspect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp

@Composable
fun SettingsScreen(nav: NavHostController) {
    val app = BpiApp.instance
    val p = app.prefs
    val ctx = LocalContext.current
    val version = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (e: Exception) { "" }
    val lengths = listOf(60, 90, 120, 150, 180, 240)

    Screen(title = "Settings", bottom = { HomeNav(nav, "Settings") }) {
        SectionLabel("Business & report")
        Field("Business name", p.businessName.value, { p.setString("businessName", it) }, placeholder = "[Your business name]")
        Field("Inspector & licence no.", p.inspector.value, { p.setString("inspector", it) }, placeholder = "[Name, licence number]")

        SectionLabel("Bookings & calendar")
        CardBox(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            Text("Default inspection length", fontWeight = FontWeight.SemiBold)
            Text("Sets the finish time when you book. You can change it on any job.", fontSize = 13.sp, color = C.Muted)
            listOf(
                "durBuilding" to "Building", "durPest" to "Timber pest",
                "durCombined" to "Building + timber pest", "durGeneral" to "Structural / special (AS 4349.0)"
            ).forEach { (key, label) ->
                val cur = when (key) { "durBuilding" -> p.durBuilding.intValue; "durPest" -> p.durPest.intValue; "durCombined" -> p.durCombined.intValue; else -> p.durGeneral.intValue }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label, Modifier.weight(1f), fontSize = 15.sp)
                    Dropdown("", lengths.map { fmtDur(it) }, fmtDur(cur), { s -> p.setInt(key, lengths.first { fmtDur(it) == s }) }, Modifier.weight(0.8f))
                }
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Travel buffer between jobs", Modifier.weight(1f), fontSize = 15.sp)
                val bufs = listOf(0, 15, 30, 45, 60)
                Dropdown("", bufs.map { if (it == 0) "None" else "$it min" }, if (p.travelBuffer.intValue == 0) "None" else "${p.travelBuffer.intValue} min",
                    { s -> p.setInt("travelBuffer", if (s == "None") 0 else s.removeSuffix(" min").toInt()) }, Modifier.weight(0.8f))
            }
        }
        CardBox {
            ToggleRow("Add new bookings to Google Calendar", "On by default when you book a job", p.addToCalendar.value) { p.setAddToCalendar(it) }
        }

        SectionLabel("Default area order")
        CardBox {
            val order = p.areaOrder.value
            order.forEachIndexed { i, a ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(a, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(enabled = i > 0, onClick = { val o = order.toMutableList(); o[i] = o[i - 1].also { o[i - 1] = o[i] }; p.setAreaOrder(o) }) { Icon(Icons.Filled.KeyboardArrowUp, "Move $a up") }
                    IconButton(enabled = i < order.size - 1, onClick = { val o = order.toMutableList(); o[i] = o[i + 1].also { o[i + 1] = o[i] }; p.setAreaOrder(o) }) { Icon(Icons.Filled.KeyboardArrowDown, "Move $a down") }
                }
                HorizontalDivider(color = C.LineSoft)
            }
        }
        Text("Used for new jobs. Each job can still be reordered on its own.", fontSize = 13.sp, color = C.Muted)

        SectionLabel("Photos")
        Notice(
            "Photos open your S26 Ultra's own camera and are saved full-size to your Gallery in Pictures › Inspections › [address] [date]. " +
                "Set the camera to 3:4, 12M and 0.6× there and it remembers. The built-in camera locked to these settings comes in Stage 2.",
            bg = C.LineSoft, fg = C.Ink2
        )

        SectionLabel("Comment library")
        CardBox { ListRow("Open comment library", "Edit, add your own, or remove comments") { nav.navigate(Routes.library()) } }

        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("BPI Inspections $version · Stage 1", fontSize = 12.sp, color = C.Muted)
        }
    }
}

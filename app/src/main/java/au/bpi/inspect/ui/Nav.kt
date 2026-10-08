package au.bpi.inspect.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/** All screens and how they link. */
@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "jobs") {
        composable("clients") { ClientsScreen(nav) }
        composable("contact/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
            ContactDetailScreen(nav, it.arguments!!.getLong("id"))
        }
        composable(
            "contactEdit/{id}?type={type}",
            listOf(navArgument("id") { type = NavType.LongType }, navArgument("type") { type = NavType.StringType; defaultValue = "Client" })
        ) { ContactEditScreen(nav, it.arguments!!.getLong("id"), it.arguments!!.getString("type") ?: "Client") }
        composable("jobs") { JobsScreen(nav) }
        composable(
            "newJob?jobId={jobId}&clientId={clientId}&start={start}",
            listOf(
                navArgument("jobId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("clientId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("start") { type = NavType.BoolType; defaultValue = false }
            )
        ) { NewJobScreen(nav, it.arguments!!.getLong("jobId"), it.arguments!!.getLong("clientId"), it.arguments!!.getBoolean("start")) }
        composable("job/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
            JobScreen(nav, it.arguments!!.getLong("id"))
        }
        composable(
            "checklist/{jobId}/{area}?room={room}",
            listOf(
                navArgument("jobId") { type = NavType.LongType },
                navArgument("area") { type = NavType.StringType },
                navArgument("room") { type = NavType.StringType; defaultValue = "" }
            )
        ) { ChecklistScreen(nav, it.arguments!!.getLong("jobId"), it.arguments!!.getString("area")!!, it.arguments!!.getString("room") ?: "") }
        composable(
            "finding/{jobId}/{findingId}?area={area}&room={room}&item={item}&cls={cls}",
            listOf(
                navArgument("jobId") { type = NavType.LongType },
                navArgument("findingId") { type = NavType.LongType },
                navArgument("area") { type = NavType.StringType; defaultValue = "" },
                navArgument("room") { type = NavType.StringType; defaultValue = "" },
                navArgument("item") { type = NavType.StringType; defaultValue = "" },
                navArgument("cls") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            val a = it.arguments!!
            FindingScreen(nav, a.getLong("jobId"), a.getLong("findingId"), a.getString("area") ?: "", a.getString("room") ?: "", a.getString("item") ?: "", a.getString("cls") ?: "", it.savedStateHandle)
        }
        composable(
            "library?pick={pick}&area={area}&sub={sub}&jobId={jobId}",
            listOf(
                navArgument("pick") { type = NavType.BoolType; defaultValue = false },
                navArgument("area") { type = NavType.StringType; defaultValue = "" },
                navArgument("sub") { type = NavType.StringType; defaultValue = "" },
                navArgument("jobId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) {
            val a = it.arguments!!
            LibraryScreen(nav, a.getBoolean("pick"), a.getString("area") ?: "", a.getString("sub") ?: "", a.getLong("jobId"))
        }
        composable("jump/{jobId}", listOf(navArgument("jobId") { type = NavType.LongType })) {
            JumpScreen(nav, it.arguments!!.getLong("jobId"))
        }
        composable("settings") { SettingsScreen(nav) }
    }
}

object Routes {
    fun enc(s: String): String = Uri.encode(s)
    fun checklist(jobId: Long, area: String, room: String = "") = "checklist/$jobId/${enc(area)}?room=${enc(room)}"
    fun finding(jobId: Long, findingId: Long, area: String = "", room: String = "", item: String = "", cls: String = "") =
        "finding/$jobId/$findingId?area=${enc(area)}&room=${enc(room)}&item=${enc(item)}&cls=${enc(cls)}"
    fun library(pick: Boolean = false, area: String = "", sub: String = "", jobId: Long = 0) =
        "library?pick=$pick&area=${enc(area)}&sub=${enc(sub)}&jobId=$jobId"
}

/** Moves between the main tabs without stacking them up. */
fun NavHostController.tab(route: String) {
    navigate(route) {
        popUpTo("jobs") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class NavItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun NavRow(items: List<NavItem>, current: String) {
    Column {
        HorizontalDivider(color = C.Line)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            items.forEach { n ->
                val on = n.label == current
                Column(
                    Modifier.weight(1f).height(56.dp)
                        .background(if (on) C.LineSoft else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable(onClick = n.onClick),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(n.icon, contentDescription = null, tint = if (on) C.Ink else C.Ink2, modifier = Modifier.size(22.dp))
                    Text(n.label, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = if (on) C.Ink else C.Ink2)
                }
            }
        }
    }
}

/** Bottom bar for the main sections. */
@Composable
fun HomeNav(nav: NavHostController, current: String) = NavRow(
    listOf(
        NavItem("Clients", Icons.Filled.People) { nav.tab("clients") },
        NavItem("Jobs", Icons.Filled.Work) { nav.tab("jobs") },
        NavItem("Library", Icons.AutoMirrored.Filled.Comment) { nav.tab(Routes.library()) },
        NavItem("Settings", Icons.Filled.Settings) { nav.tab("settings") }
    ), current
)

/** Bottom bar while working inside a job. */
@Composable
fun JobNav(nav: NavHostController, jobId: Long, current: String) = NavRow(
    listOf(
        NavItem("Areas", Icons.Filled.Home) { nav.navigate("job/$jobId") { popUpTo("job/$jobId") { inclusive = true }; launchSingleTop = true } },
        NavItem("Jump to", Icons.AutoMirrored.Filled.List) { nav.navigate("jump/$jobId") { launchSingleTop = true } },
        NavItem("Library", Icons.AutoMirrored.Filled.Comment) { nav.navigate(Routes.library(jobId = jobId)) { launchSingleTop = true } },
        NavItem("Jobs", Icons.Filled.Work) { nav.tab("jobs") }
    ), current
)

package au.bpi.inspect.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import au.bpi.inspect.BpiApp
import au.bpi.inspect.data.Finding
import au.bpi.inspect.data.J
import au.bpi.inspect.data.LibComment
import au.bpi.inspect.data.Photo
import au.bpi.inspect.data.Templates
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** Key used to hand a picked library comment back to this screen. */
const val PICKED = "picked"
const val SEP = "\u0001"

@Composable
fun FindingScreen(
    nav: NavHostController, jobId: Long, findingId: Long,
    areaArg: String, roomArg: String, itemArg: String, clsArg: String,
    handle: SavedStateHandle
) {
    val app = BpiApp.instance
    val db = app.db
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val job by db.jobs().byId(jobId).collectAsState(initial = null)
    val photos by db.photos().forJob(jobId).collectAsState(initial = emptyList())

    var id by remember { mutableLongStateOf(findingId) }
    var loaded by remember { mutableStateOf(findingId == 0L) }
    var area by remember { mutableStateOf(areaArg) }
    var room by remember { mutableStateOf(roomArg) }
    var item by remember { mutableStateOf(itemArg) }
    var cls by remember { mutableStateOf(clsArg.ifEmpty { "Minor" }) }
    var location by remember { mutableStateOf(listOf(roomArg, itemArg).filter { it.isNotBlank() }.joinToString(", ")) }
    var text by remember { mutableStateOf("") }
    var libText by remember { mutableStateOf("") }   // wording as inserted from the library, for "Undo my edits"
    var trades by remember { mutableStateOf(listOf<String>()) }
    var showAllTrades by remember { mutableStateOf(false) }
    var savedToLib by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Photos taken on this screen before the finding is first saved.
    var draftPhotoIds by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(findingId) {
        if (findingId > 0) db.findings().get(findingId)?.let { f ->
            area = f.area; room = f.room; item = f.item; cls = f.cls; location = f.location; text = f.text; trades = J.list(f.tradesJson)
            loaded = true
        }
    }

    // A comment picked in the library comes back here.
    val picked by handle.getStateFlow(PICKED, "").collectAsState()
    LaunchedEffect(picked) {
        if (picked.isNotEmpty()) {
            val p = picked.split(SEP)
            if (p.size >= 3) {
                cls = p[0]
                val t = p[1].replace("[location]", location.ifBlank { "[location]" })
                text = if (text.isBlank()) t else text.trimEnd() + "\n" + t
                libText = text
                trades = (trades + J.list(p[2])).distinct()
            }
            handle[PICKED] = ""
        }
    }

    val myPhotos = photos.filter { (id > 0 && it.findingId == id) || it.id in draftPhotoIds }

    var pendingUri by remember { mutableStateOf(null as Uri?) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pendingUri
        if (u != null) {
            if (ok) scope.launch {
                val pid = db.photos().insert(Photo(jobId = jobId, findingId = id.takeIf { it > 0 }, uri = u.toString()))
                if (id == 0L) draftPhotoIds = draftPhotoIds + pid
            } else Actions.discard(ctx, u)
        }
        pendingUri = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        scope.launch {
            uris.forEach { u ->
                try { ctx.contentResolver.takePersistableUriPermission(u, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                val pid = db.photos().insert(Photo(jobId = jobId, findingId = id.takeIf { it > 0 }, uri = u.toString()))
                if (id == 0L) draftPhotoIds = draftPhotoIds + pid
            }
        }
    }
    fun takePhoto() {
        val j = job ?: return
        val u = Actions.newPhotoUri(ctx, j) ?: return
        pendingUri = u
        camera.launch(u)
    }

    fun save() = scope.launch {
        val f = Finding(id = id, jobId = jobId, area = area, room = room, item = item, cls = cls, location = location.trim(), text = text.trim(), tradesJson = J.list(trades))
        val rid = db.findings().upsert(f)
        val fid = if (rid > 0) rid else id
        if (draftPhotoIds.isNotEmpty()) db.photos().attach(draftPhotoIds, fid)
        nav.popBackStack()
    }

    val classes = when (area) {
        Templates.PEST_AREA -> listOf("Pest", "Major", "Minor")
        Templates.COND_AREA -> listOf("Cond", "Pest")
        else -> listOf("Minor", "Major", "Safety")
    }
    val (libArea, libSub) = Templates.librarySubFor(area, item)

    Screen(
        title = if (findingId > 0) "Finding" else "New finding",
        subtitle = listOf(area, room, item).filter { it.isNotBlank() }.joinToString(" › "),
        onBack = { nav.popBackStack() },
        actions = { if (id > 0) TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = C.Accent) } },
        bottom = { BottomBar { PrimaryButton("Save finding", Modifier.weight(1f), enabled = text.isNotBlank() || myPhotos.isNotEmpty()) { save() } } }
    ) {
        if (!loaded) { Text("Loading…"); return@Screen }

        SectionLabel("Photos")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            myPhotos.forEach { p ->
                Box {
                    AsyncImage(
                        model = p.uri, contentDescription = "Photo", contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)).background(C.Tint)
                    )
                    IconButton(onClick = { scope.launch { db.photos().delete(p); draftPhotoIds = draftPhotoIds - p.id } },
                        modifier = Modifier.align(Alignment.TopEnd).size(32.dp).background(Color(0x99000000), RoundedCornerShape(16.dp))) {
                        Icon(Icons.Filled.Close, "Remove photo from finding", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Take photo", Modifier.weight(1f), color = C.Ink, icon = Icons.Filled.CameraAlt) { takePhoto() }
            SecondaryButton("From Gallery", Modifier.weight(1f), icon = Icons.Filled.PhotoLibrary) {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        Text("Photos are saved full-size to your Gallery in Pictures › Inspections › ${job?.address ?: ""}. Removing one here doesn't delete it from your Gallery.", fontSize = 12.sp, color = C.Muted)

        SectionLabel("Classification")
        ChoiceChips(classes, { it == cls }, { cls = it }, { ratingColors(it) })
        Field("Location", location, { location = it }, placeholder = "e.g. Kitchen, north wall, above window head")

        SectionLabel("Comment") {
            TextButton(onClick = { nav.navigate(Routes.library(pick = true, area = libArea, sub = libSub, jobId = jobId)) }) {
                Icon(Icons.AutoMirrored.Filled.Comment, null, tint = C.Accent, modifier = Modifier.size(18.dp))
                Text(" Comment library", color = C.Accent, fontWeight = FontWeight.Bold)
            }
        }
        Field("", text, { text = it; savedToLib = false }, lines = 5, placeholder = "Describe the defect, or pick a comment from the library")
        Column(Modifier.fillMaxWidth().background(C.LineSoft, RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Edit freely. Changes here apply to this inspection only; your library stays as it was.", fontSize = 13.sp, color = C.Ink2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Undo my edits", Modifier.weight(1f).height(44.dp)) { if (libText.isNotEmpty()) text = libText }
                PrimaryButton(if (savedToLib) "Saved ✓" else "Save to library", Modifier.weight(1f).height(44.dp), color = C.Ink, enabled = text.isNotBlank() && !savedToLib) {
                    scope.launch {
                        db.library().upsert(LibComment(area = libArea, sub = libSub.ifBlank { "General" }, cls = cls, text = text.trim(), tradesJson = J.list(trades), flag = "mine", sort = Int.MAX_VALUE))
                        savedToLib = true
                    }
                }
            }
        }

        SectionLabel("Refer to (trades)") {
            TextButton(onClick = { showAllTrades = !showAllTrades }) { Text(if (showAllTrades) "Show fewer" else "Show all", color = C.Ink) }
        }
        val common = listOf("QBCC licensed builder", "QBCC licensed structural engineer", "QBCC licensed plumber", "QBCC licensed electrician", "QBCC licensed roof plumber", "QBCC licensed pest manager")
        val shown = if (showAllTrades) app.trades else (trades + common).distinct()
        ChoiceChips(shown, { it in trades }, { t -> trades = if (t in trades) trades - t else trades + t })
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete this finding?") },
        text = { Text("The comment is removed from this job. Its photos stay in your Gallery.") },
        confirmButton = { TextButton(onClick = {
            scope.launch { db.findings().get(id)?.let { db.findings().delete(it) }; nav.popBackStack() }
        }) { Text("Delete", color = C.Accent) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}

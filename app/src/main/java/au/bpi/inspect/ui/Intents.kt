package au.bpi.inspect.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.widget.Toast
import au.bpi.inspect.data.Job
import au.bpi.inspect.data.Templates
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Hand-offs to other apps on the phone: Maps, Calendar, phone, email, camera. */
object Actions {

    private fun start(ctx: Context, i: Intent, fallback: Intent? = null) {
        try { ctx.startActivity(i) } catch (e: Exception) {
            if (fallback != null) try { ctx.startActivity(fallback); return } catch (_: Exception) {}
            Toast.makeText(ctx, "No app found to open this", Toast.LENGTH_SHORT).show()
        }
    }

    /** Opens Google Maps with driving directions to the address. */
    fun directions(ctx: Context, address: String) {
        val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(address) + "&mode=d"))
            .setPackage("com.google.android.apps.maps")
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=" + Uri.encode(address)))
        start(ctx, nav, web)
    }

    fun call(ctx: Context, phone: String) = start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))
    fun sms(ctx: Context, phone: String) = start(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone))))
    fun email(ctx: Context, to: String) = start(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(to))))

    /** Opens Google Calendar with the booking filled in, ready to save. */
    fun addToCalendar(ctx: Context, job: Job, clientLine: String, agentLine: String) {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.ofEpochDay(job.dateEpochDay)
        val begin = day.atStartOfDay(zone).toInstant().toEpochMilli() + job.startMin * 60_000L
        val end = begin + job.durationMin * 60_000L
        val title = if (job.scope == "General") "${job.purpose} inspection (AS 4349.0) – ${job.address}"
                    else "${job.purpose} ${Templates.scopeLabel(job)} – ${job.address}"
        val desc = buildString {
            if (clientLine.isNotBlank()) append("Client: ").append(clientLine).append('\n')
            if (agentLine.isNotBlank()) append("Agent / referrer: ").append(agentLine).append('\n')
            if (job.specialScope.isNotBlank()) append("Scope: ").append(job.specialScope).append('\n')
            append("Booked in BPI Inspections")
        }
        val i = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, job.address)
            .putExtra(CalendarContract.Events.DESCRIPTION, desc)
            .setPackage("com.google.android.calendar")
        val anyCal = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtras(i.extras ?: android.os.Bundle())
        start(ctx, i, anyCal)
    }

    /**
     * Creates the Gallery entry the camera will write the full-resolution photo into,
     * in Pictures/Inspections/<address> <date>.
     */
    fun newPhotoUri(ctx: Context, job: Job): Uri? {
        val folder = (job.address.ifBlank { "Job ${job.id}" } + " " + LocalDate.ofEpochDay(job.dateEpochDay).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            .replace(Regex("[\\\\/:*?\"<>|]"), "-").trim()
        val name = "BPI_" + System.currentTimeMillis() + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Inspections/" + folder)
        }
        return ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    /** Removes an empty Gallery entry if the photo was cancelled. */
    fun discard(ctx: Context, uri: Uri) {
        try { ctx.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
    }
}

fun fmtTime(min: Int): String = "%02d:%02d".format(min / 60, min % 60)
fun fmtDur(min: Int): String = when {
    min % 60 == 0 -> "${min / 60} hr" + if (min >= 120) "s" else ""
    min > 60 -> "%.1f hrs".format(min / 60.0)
    else -> "$min min"
}
fun fmtDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE d MMM yyyy"))

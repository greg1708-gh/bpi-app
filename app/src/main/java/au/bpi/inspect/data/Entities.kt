package au.bpi.inspect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A client, agent, conveyancer or other contact. */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "Client",          // Client, Agent, Conveyancer, Other
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val address: String = "",
    val notes: String = "",
    val copyReports: Boolean = false
)

/** One inspection job, from booking through to completion. */
@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val purpose: String = "Pre-purchase",      // Pre-purchase, Vendor pre-sale, Standard, Structural, Special purpose
    val scope: String = "Combined",            // Building, Timber pest, Combined, General (AS 4349.0)
    val specialScope: String = "",
    val clientId: Long? = null,
    val agentId: Long? = null,
    val dateEpochDay: Long,
    val startMin: Int = 8 * 60 + 30,
    val durationMin: Int = 150,
    val status: String = "Booked",             // Booked, In progress, Complete
    val agreementAccepted: Boolean = false,
    val propsJson: String = "{}",              // property details, keyed by field name
    val areaOrderJson: String = "[]",          // building area order for this job
    val roomsJson: String = "[]",              // interior rooms for this job
    val createdAt: Long = System.currentTimeMillis()
)

/** The rating given to one checklist item on one job. */
@Entity(tableName = "item_results", primaryKeys = ["jobId", "area", "room", "item"])
data class ItemResult(
    val jobId: Long,
    val area: String,
    val room: String,      // interior room name, or "" for other areas
    val item: String,
    val rating: String,    // OK, Minor, Major, Safety, N/I  (pest areas: None, Found, N/I)
    val niReason: String = "",
    val niNote: String = ""
)

/** A defect or timber pest finding with its comment and trade referrals. */
@Entity(tableName = "findings")
data class Finding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val area: String,
    val room: String = "",
    val item: String = "",
    val cls: String = "Minor",       // Major, Minor, Safety, Pest, Cond
    val location: String = "",
    val text: String = "",
    val tradesJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

/** A photo saved to the phone's Gallery and linked to a job (and optionally a finding). */
@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val findingId: Long? = null,
    val uri: String,
    val takenAt: Long = System.currentTimeMillis()
)

/** A comment in the reusable library. flag: "" standard, "mine" added by you, "edited" changed by you. */
@Entity(tableName = "library")
data class LibComment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val area: String,
    val sub: String,
    val cls: String,
    val text: String,
    val tradesJson: String = "[]",
    val flag: String = "",
    val sort: Int = 0
)

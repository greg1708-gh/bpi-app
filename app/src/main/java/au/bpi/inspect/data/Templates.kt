package au.bpi.inspect.data

import org.json.JSONArray
import org.json.JSONObject

/** Standards, areas, checklist items and drop-down options used across the app. */
object Templates {

    const val AS_BUILDING = "AS 4349.1 - 2007"
    const val AS_PEST = "AS 4349.3 - 2010"
    const val AS_GENERAL = "AS 4349.0 - 2007"

    val purposes = listOf("Pre-purchase", "Vendor pre-sale", "Standard", "Structural", "Special purpose")
    fun isGeneralPurpose(purpose: String) = purpose == "Structural" || purpose == "Special purpose"

    val scopes = listOf("Building", "Timber pest", "Combined")

    val defaultAreaOrder = listOf("Site", "Roof exterior", "Exterior", "Interior", "Roof space", "Subfloor")
    const val PEST_AREA = "Timber pest"
    const val COND_AREA = "Conducive"

    /** Building areas shown for this scope. */
    fun hasBuilding(job: Job) = job.scope != "Timber pest"
    fun hasPest(job: Job) = job.scope == "Timber pest" || job.scope == "Combined"

    fun scopeLabel(job: Job): String = when {
        job.scope == "General" -> "Structural / special (AS 4349.0)"
        job.scope == "Combined" -> "Building + Timber Pest"
        else -> job.scope
    }

    /** Checklist items for areas other than Interior (one row per sub-area). */
    val areaItems: Map<String, List<String>> = mapOf(
        "Site" to listOf("Fences & gates", "Paths & driveways", "Retaining walls", "Surface water & stormwater", "Outbuildings, carports & sheds", "Trees & vegetation"),
        "Roof exterior" to listOf("Roof cladding", "Ridge & hip capping", "Flashings", "Gutters & downpipes", "Eaves, fascias & barges", "Skylights, vents & penetrations"),
        "Exterior" to listOf("Walls & cladding", "Windows & external doors", "Stairs, balustrades & handrails", "Decks, verandahs & balconies", "DPC, weep holes & vents", "Paintwork & finishes"),
        "Roof space" to listOf("Roof framing", "Sarking & insulation", "Ceiling framing & supports", "Electrical & services", "Moisture & leaks", "Access limitations"),
        "Subfloor" to listOf("Stumps, piers & footings", "Bearers & joists", "Ventilation", "Moisture & drainage", "Plumbing", "Debris & storage"),
        PEST_AREA to listOf("Termite activity", "Termite damage", "Termite management system", "Borers & fungal decay"),
        COND_AREA to listOf("Moisture & leaks", "Drainage & ground levels", "Garden beds & landscaping", "Timber in contact with soil", "Ventilation", "Slab edge & weep holes", "Termite management system", "Trees, stumps & fencing", "Attached structures", "Risk assessment")
    )

    val defaultRooms = listOf("Entry", "Lounge", "Dining", "Kitchen", "Laundry", "Bathroom", "Ensuite", "Toilet", "Bedroom 1", "Bedroom 2", "Bedroom 3", "Hallway")
    val roomSuggestions = listOf("Entry", "Lounge", "Family", "Dining", "Kitchen", "Laundry", "Bathroom", "Ensuite", "Toilet", "Bedroom 1", "Bedroom 2", "Bedroom 3", "Bedroom 4", "Study", "Hallway", "Rumpus", "Stairs", "Garage")

    fun roomItems(room: String): List<String> {
        val r = room.lowercase()
        val base = mutableListOf("Ceilings", "Walls", "Floors", "Doors & windows")
        when {
            r.contains("kitchen") -> base += listOf("Benchtops & cabinets", "Sink & taps")
            r.contains("bath") || r.contains("ensuite") || r.contains("laundry") || r.contains("toilet") ->
                base += listOf("Shower & bath", "Vanity, tubs & taps", "Waterproofing & seals", "Exhaust ventilation")
            r.contains("stair") -> base += listOf("Internal stairs & balustrades")
        }
        base += listOf("Visible electrical & smoke alarms", "Moisture readings")
        return base
    }

    /** Which library sub-area best matches a checklist item, so the right comments come up first. */
    fun librarySubFor(area: String, item: String): Pair<String, String> {
        if (area != "Interior") return area to item
        val sub = when (item) {
            "Ceilings" -> "Ceilings"
            "Walls" -> "Walls"
            "Floors" -> "Floors"
            "Doors & windows" -> "Doors & windows"
            "Benchtops & cabinets", "Sink & taps" -> "Kitchen"
            "Shower & bath", "Vanity, tubs & taps", "Waterproofing & seals", "Exhaust ventilation" -> "Bathroom, ensuite & laundry"
            "Internal stairs & balustrades" -> "Internal stairs & balustrades"
            "Visible electrical & smoke alarms" -> "Smoke alarms & visible electrical"
            else -> item
        }
        return "Interior" to sub
    }

    fun ratingsFor(area: String): List<String> =
        if (area == PEST_AREA || area == COND_AREA) listOf("None found", "Found", "N/I") else listOf("OK", "Minor", "Major", "Safety", "N/I")

    val niReasons = listOf("Furniture / stored goods", "Floor coverings", "Locked / no access", "Unsafe to access", "Not visible", "Other")

    /** Property detail fields and their drop-down options, in display order. */
    val propertyFields: List<Pair<String, List<String>>> = listOf(
        "Building type" to listOf("Detached house", "Townhouse", "Unit", "Duplex", "Other"),
        "Construction" to listOf("Timber frame", "Brick veneer", "Double brick", "Concrete block", "Lightweight cladding", "Other"),
        "Storeys" to listOf("Single", "Two", "Split level", "Highset", "Lowset", "Three or more"),
        "Front facade faces" to listOf("North", "North-east", "East", "South-east", "South", "South-west", "West", "North-west"),
        "Occupancy" to listOf("Owner-occupied", "Tenanted", "Vacant"),
        "Site topography" to listOf("Level", "Gentle slope", "Moderate slope", "Steep slope", "Falls to the front", "Falls to the rear", "Falls side to side", "Elevated / ridge", "Low-lying"),
        "Structure age (approx.)" to listOf("Under 5 years", "5–10 years", "10–20 years", "20–40 years", "40–60 years", "60–100 years", "Over 100 years", "Unknown"),
        "Utility status" to listOf("All services connected", "Power & water on, no gas", "Power off", "Water off", "All services disconnected", "Not determined"),
        "Weather conditions" to listOf("Fine & dry", "Overcast, dry", "Light rain", "Heavy rain", "Showers", "Recent rain (last 48 hrs)", "Hot", "Windy", "Storm"),
        "Floor type" to listOf("Suspended timber", "Concrete slab on ground", "Suspended concrete slab", "Timber & slab (mixed)", "Sheet flooring (particleboard)", "Other"),
        "Roof type" to listOf("Concrete tile", "Terracotta tile", "Metal sheet (e.g. Colorbond)", "Corrugated iron (galvanised)", "Fibre cement sheet", "Slate", "Membrane / flat roof", "Other"),
        "Furnishing status" to listOf("Unfurnished", "Partly furnished", "Fully furnished", "Furnished + stored goods", "Moving in / out")
    )

    val contactTypes = listOf("Client", "Agent", "Conveyancer", "Other")
}

/** Small JSON helpers for the few list/map fields stored as text. */
object J {
    fun list(s: String): List<String> = try {
        val a = JSONArray(s); List(a.length()) { a.getString(it) }
    } catch (e: Exception) { emptyList() }

    fun list(l: List<String>): String = JSONArray(l).toString()

    fun map(s: String): Map<String, String> = try {
        val o = JSONObject(s); o.keys().asSequence().associateWith { o.getString(it) }
    } catch (e: Exception) { emptyMap() }

    fun map(m: Map<String, String>): String = JSONObject(m).toString()
}

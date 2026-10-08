package au.bpi.inspect.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** Your settings, kept on the phone. Changes show up immediately across the app. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("bpi_settings", Context.MODE_PRIVATE)

    val businessName = mutableStateOf(sp.getString("businessName", "") ?: "")
    val inspector = mutableStateOf(sp.getString("inspector", "") ?: "")
    val travelBuffer = mutableIntStateOf(sp.getInt("travelBuffer", 30))
    val durBuilding = mutableIntStateOf(sp.getInt("durBuilding", 90))
    val durPest = mutableIntStateOf(sp.getInt("durPest", 60))
    val durCombined = mutableIntStateOf(sp.getInt("durCombined", 150))
    val durGeneral = mutableIntStateOf(sp.getInt("durGeneral", 120))
    val albumStyle = mutableStateOf(sp.getString("albumStyle", "address-date") ?: "address-date")
    val areaOrder = mutableStateOf(J.list(sp.getString("areaOrder", null) ?: J.list(Templates.defaultAreaOrder)))
    val addToCalendar = mutableStateOf(sp.getBoolean("addToCalendar", true))

    fun setString(key: String, v: String) {
        sp.edit().putString(key, v).apply()
        when (key) {
            "businessName" -> businessName.value = v
            "inspector" -> inspector.value = v
            "albumStyle" -> albumStyle.value = v
        }
    }

    fun setInt(key: String, v: Int) {
        sp.edit().putInt(key, v).apply()
        when (key) {
            "travelBuffer" -> travelBuffer.intValue = v
            "durBuilding" -> durBuilding.intValue = v
            "durPest" -> durPest.intValue = v
            "durCombined" -> durCombined.intValue = v
            "durGeneral" -> durGeneral.intValue = v
        }
    }

    fun setAddToCalendar(v: Boolean) {
        sp.edit().putBoolean("addToCalendar", v).apply(); addToCalendar.value = v
    }

    fun setAreaOrder(order: List<String>) {
        sp.edit().putString("areaOrder", J.list(order)).apply(); areaOrder.value = order
    }

    fun defaultDuration(purpose: String, scope: String): Int = when {
        Templates.isGeneralPurpose(purpose) -> durGeneral.intValue
        scope == "Building" -> durBuilding.intValue
        scope == "Timber pest" -> durPest.intValue
        else -> durCombined.intValue
    }
}

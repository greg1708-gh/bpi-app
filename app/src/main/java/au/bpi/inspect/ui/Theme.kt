package au.bpi.inspect.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** High-contrast field palette, same as the mockup. */
object C {
    val Ground = Color(0xFFF4F3EF)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF16181D)
    val Ink2 = Color(0xFF3A3C42)
    val Muted = Color(0xFF5A5C63)
    val Line = Color(0xFFD9D6CC)
    val LineSoft = Color(0xFFECE9E1)
    val Field = Color(0xFFC9C5B9)
    val Tint = Color(0xFFE7E4DA)
    val Accent = Color(0xFFB93B0B)
    val AccentSoft = Color(0xFFFCE7DC)
    val AccentInk = Color(0xFF8A2C08)
    val Minor = Color(0xFF1E4FA8)
    val Safety = Color(0xFFF2B233)
    val Pest = Color(0xFF5B3A8C)
    val Cond = Color(0xFF0F5E5E)
    val NI = Color(0xFF6B6D73)
    val Warn = Color(0xFFFDF0D5)
    val WarnInk = Color(0xFF3D2B04)
}

/** Colour pair (background, text) for a rating or classification. */
fun ratingColors(r: String): Pair<Color, Color> = when (r) {
    "OK", "None found" -> C.Ink to Color.White
    "Minor" -> C.Minor to Color.White
    "Major" -> C.Accent to Color.White
    "Safety" -> C.Safety to C.Ink
    "Pest", "Found" -> C.Pest to Color.White
    "Cond" -> C.Cond to Color.White
    "N/I" -> C.NI to Color.White
    else -> C.Tint to C.Ink
}

fun clsLabel(cls: String): String = when (cls) {
    "Major" -> "Major defect"
    "Minor" -> "Minor defect"
    "Safety" -> "Safety hazard"
    "Pest" -> "Timber pest"
    "Cond" -> "Conducive condition"
    else -> cls
}

@Composable
fun BpiTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = C.Ink, onPrimary = Color.White,
        secondary = C.Accent, onSecondary = Color.White,
        background = C.Ground, onBackground = C.Ink,
        surface = C.Card, onSurface = C.Ink,
        surfaceVariant = C.Tint, onSurfaceVariant = C.Muted,
        outline = C.Field, error = C.Accent
    )
    val base = FontFamily.SansSerif
    val type = Typography(
        titleLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        bodyLarge = TextStyle(fontFamily = base, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = base, fontSize = 15.sp),
        labelLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    )
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}

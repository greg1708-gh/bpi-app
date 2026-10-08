package au.bpi.inspect.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Page frame: header, scrolling content, optional bottom bar. */
@Composable
fun Screen(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    dark: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    headerExtra: @Composable ColumnScope.() -> Unit = {},
    bottom: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = if (dark) C.Ink else C.Ground
    val fg = if (dark) Color.White else C.Ink
    val sub = if (dark) Color(0xFFC9CBD1) else C.Muted
    Column(Modifier.fillMaxSize().background(C.Ground).imePadding()) {
        Column(
            Modifier.fillMaxWidth().background(bg).statusBarsPadding()
                .padding(start = if (onBack != null) 6.dp else 20.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = fg)
                    }
                }
                Column(Modifier.weight(1f)) {
                    if (subtitle != null) Text(subtitle, color = sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(title, color = fg, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                actions()
            }
            Column(Modifier.padding(start = if (onBack != null) 14.dp else 0.dp)) { headerExtra() }
        }
        HorizontalDivider(color = C.LineSoft)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScrollSafe().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
        Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) { bottom() }
    }
}

@Composable
private fun Modifier.verticalScrollSafe(): Modifier = this.verticalScroll(rememberScrollState())

@Composable
fun SectionLabel(text: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), color = C.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(0.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(C.Card, RoundedCornerShape(14.dp)).border(1.dp, C.Line, RoundedCornerShape(14.dp)).padding(padding),
        content = content
    )
}

/** A tappable list row with title, detail and chevron. */
@Composable
fun ListRow(title: String, detail: String? = null, leading: @Composable (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            if (!detail.isNullOrBlank()) Text(detail, fontSize = 13.sp, color = C.Muted)
        }
        if (trailing != null) trailing() else Text("›", fontSize = 22.sp, color = C.NI)
    }
    HorizontalDivider(color = C.LineSoft)
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = C.Accent, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White, disabledContainerColor = Color(0xFFA9A597), disabledContentColor = Color.White)
    ) {
        if (icon != null) { Icon(icon, null); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, C.Ink),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = C.Ink)
    ) {
        if (icon != null) { Icon(icon, null); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Bottom action area with consistent padding. */
@Composable
fun BottomBar(content: @Composable RowScope.() -> Unit) {
    Column {
        HorizontalDivider(color = C.LineSoft)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** A row of equal-width options; one selected. */
@Composable
fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit, labels: (String) -> String = { it }) {
    Row(Modifier.fillMaxWidth().background(C.Tint, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.weight(1f).height(44.dp).background(if (on) Color.White else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center
            ) { Text(labels(o), fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) C.Ink else C.Ink2, textAlign = TextAlign.Center) }
        }
    }
}

/** Wrapping choice chips. multi = several can be on. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(options: List<String>, isOn: (String) -> Boolean, onTap: (String) -> Unit, onColor: (String) -> Pair<Color, Color> = { C.Ink to Color.White }) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = isOn(o)
            val (bg, fg) = if (on) onColor(o) else (Color.White to C.Ink)
            Box(
                Modifier.heightIn(min = 44.dp).background(bg, RoundedCornerShape(22.dp))
                    .border(1.dp, if (on) bg else C.Field, RoundedCornerShape(22.dp))
                    .clickable { onTap(o) }.padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(o, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg) }
        }
    }
}

/** Horizontal scrolling chips for quick switching. */
@Composable
fun ScrollChips(options: List<String>, selected: String?, onTap: (String) -> Unit, done: (String) -> Boolean = { false }) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.height(42.dp).background(if (on) C.Ink else Color.White, RoundedCornerShape(21.dp))
                    .border(1.dp, if (on) C.Ink else C.Field, RoundedCornerShape(21.dp))
                    .clickable { onTap(o) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text(if (done(o) && !on) "$o ✓" else o, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) Color.White else C.Ink2) }
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
        OutlinedTextField(
            value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = C.NI) },
            singleLine = lines == 1, minLines = lines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = C.Field, focusedBorderColor = C.Ink)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(label: String, options: List<String>, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
            OutlinedTextField(
                value = value.ifEmpty { "Select" }, onValueChange = {}, readOnly = true, singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = if (value.isEmpty()) C.NI else C.Ink),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = C.Field, focusedBorderColor = C.Ink)
            )
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); open = false })
                }
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, detail: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (detail != null) Text(detail, fontSize = 13.sp, color = C.Muted)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.Ink, checkedThumbColor = Color.White))
    }
}

@Composable
fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun Avatar(name: String, type: String, size: Int = 44) {
    val (bg, fg) = when (type) {
        "Client" -> C.AccentSoft to C.AccentInk
        "Agent" -> Color(0xFFE3EAF7) to C.Minor
        "Conveyancer" -> Color(0xFFE4EFEA) to C.Cond
        else -> C.LineSoft to C.Ink2
    }
    val initials = name.replace("[", "").replace("]", "").split(' ', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Text(initials.ifEmpty { "?" }, color = fg, fontWeight = FontWeight.Bold, fontSize = (size / 2.6).sp)
    }
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier, bg: Color = C.Warn, fg: Color = C.WarnInk) {
    Text(text, fontSize = 14.sp, color = fg, modifier = modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).padding(12.dp))
}

@Composable
fun EmptyState(text: String) {
    Text(text, color = C.Muted, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center)
}

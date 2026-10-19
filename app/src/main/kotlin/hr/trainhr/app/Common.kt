package hr.trainhr.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Boje stanja štoperice i oznaka (rad, odmor), uz MaterialTheme boje. */
data class Extra(val work: Color, val workBg: Color, val rest: Color, val restBg: Color)

val LightExtra = Extra(Color(0xFF1D7A4C), Color(0xFFDCF1E5), Color(0xFF9A5B00), Color(0xFFFBECD2))
val DarkExtra = Extra(Color(0xFF6FD39E), Color(0xFF163527), Color(0xFFF2B65A), Color(0xFF3A2A12))

@Composable
fun extra(): Extra = if (isSystemInDarkTheme()) DarkExtra else LightExtra

/** Nešto veća slova od zadanih – čitljivost u teretani. */
private val BigTypography = Typography().let { t ->
    t.copy(
        bodyLarge = t.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelLarge = t.labelLarge.copy(fontSize = 16.sp),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF8CC4EF))
        else lightColorScheme(primary = Color(0xFF1F5E8C)),
        typography = BigTypography,
        content = content,
    )
}

fun parseNumber(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

private val WEEKDAYS = listOf("pon", "uto", "sri", "čet", "pet", "sub", "ned")
fun weekday(day: Long): String = WEEKDAYS[LocalDate.ofEpochDay(day).dayOfWeek.value - 1]
fun formatDate(day: Long): String = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d. M. yyyy."))
fun formatShort(day: Long): String = LocalDate.ofEpochDay(day).let { "${weekday(day)} ${it.dayOfMonth}. ${it.monthValue}." }
fun formatInt(n: Int): String = "%,d".format(java.util.Locale.forLanguageTag("hr"), n)

/** 90 → "1:30 min", 120 → "2 min", 45 → "45 s". */
fun restLabel(sec: Int): String = when {
    sec < 60 -> "$sec s"
    sec % 60 == 0 -> "${sec / 60} min"
    else -> "${sec / 60}:${(sec % 60).toString().padStart(2, '0')} min"
}

fun clock(seconds: Long): String {
    val s = kotlin.math.abs(seconds)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
fun TitleBar(title: String, onSettings: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (onSettings != null) {
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Postavke") }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun BoxCard(highlight: Boolean = false, warn: Boolean = false, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = if (warn) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer) else CardDefaults.cardColors(),
        border = if (highlight) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
fun Badge(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, decimal: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

/** Veliki −/+ gumbi oko brojčanog polja. */
@Composable
fun Stepper(label: String, value: String, onChange: (String) -> Unit, step: Double, decimal: Boolean, min: Double = 0.0) {
    fun bump(d: Double) {
        val cur = parseNumber(value) ?: 0.0
        val v = maxOf(min, Math.round((cur + d) * 100) / 100.0)
        onChange(if (decimal) hr.trainhr.core.kg(v) else v.toInt().toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { bump(-step) }, modifier = Modifier.size(64.dp, 56.dp)) { Text("−", fontSize = 26.sp) }
        NumberField(label, value, onChange, Modifier.weight(1f), decimal)
        OutlinedButton(onClick = { bump(step) }, modifier = Modifier.size(64.dp, 56.dp)) { Text("+", fontSize = 26.sp) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Chips(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

/** Tanka traka napretka (0..1). */
@Composable
fun Meter(fraction: Float, color: Color = MaterialTheme.colorScheme.primary) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        drawRoundRect(color.copy(alpha = 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
        drawRoundRect(color, size = size.copy(width = size.width * fraction.coerceIn(0f, 1f)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
    }
}

/** Red brojčanih pokazatelja (vrijednost + oznaka). */
@Composable
fun Kpis(items: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (v, l) ->
            Card(Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(v, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Muted(l)
                }
            }
        }
    }
}

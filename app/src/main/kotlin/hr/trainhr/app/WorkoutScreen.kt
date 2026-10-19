package hr.trainhr.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.trainhr.core.LoadKind
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.SetEntry
import hr.trainhr.core.kg
import kotlinx.coroutines.delay
import kotlin.math.abs

fun describeSets(sets: List<SetEntry>, timed: Boolean): String =
    sets.joinToString("  ") { s -> (s.weightKg?.let { "${kg(it)}×" } ?: "") + s.reps + if (timed) "s" else "" }

fun clock(seconds: Long): String {
    val s = abs(seconds)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Kratka vibracija i zvučni signal na kraju odmora. */
private fun signal(context: Context) {
    runCatching {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 800)
    }
}

@Composable
fun WorkoutScreen(vm: AppViewModel, d: AppData, a: ActiveWorkout) {
    val catalog = vm.catalog(d)
    val context = LocalContext.current
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    BackHandler { confirmCancel = true }

    // Zaslon ostaje upaljen tijekom treninga.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    // Signal kad odmor istekne.
    val timer = a.timer
    LaunchedEffect(timer) {
        if (timer != null && timer.kind != TimerKind.WORK) {
            val left = timer.startedAt + timer.durationSec * 1000L - System.currentTimeMillis()
            if (left > 0) {
                delay(left)
                signal(context)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TimerPanel(a, now, catalog[a.entries.getOrNull(timer?.entryIndex ?: -1)?.exerciseId ?: ""]?.name, vm::endSet, vm::skipRest)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            a.entries.forEachIndexed { i, e ->
                val ex = catalog[e.exerciseId] ?: return@forEachIndexed
                val rec = vm.recommendation(d, ex.id)
                val last = vm.history(d, ex.id).lastOrNull()
                val working = timer != null && timer.kind == TimerKind.WORK && timer.entryIndex == i
                val pending = a.pending?.takeIf { it.entryIndex == i }
                BoxCard {
                    Text(
                        "${ex.name}  (${e.sets.size}/$SETS_PER_EXERCISE)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    rec?.let { Text(it.text, style = MaterialTheme.typography.bodyMedium) }
                    last?.let { (day, sets) ->
                        Text("Prošli put (${formatDate(day)}): ${describeSets(sets, ex.timed)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    e.sets.forEachIndexed { si, s ->
                        Text("Serija ${si + 1}: ${describeSets(listOf(s), ex.timed)}" + (s.workSeconds?.let { " · rad ${clock(it.toLong())}" } ?: ""))
                    }
                    if (!working) {
                        OutlinedButton(onClick = { vm.startSet(i) }, enabled = timer?.kind != TimerKind.WORK) {
                            Text("▶ Počni seriju ${e.sets.size + 1}")
                        }
                    }
                    if (pending != null) {
                        Text("Upišite rezultat serije (rad ${clock(pending.workSeconds.toLong())}):", fontWeight = FontWeight.SemiBold)
                    }
                    SetInput(
                        key = "$i-${e.sets.size}",
                        showWeight = ex.kind != LoadKind.BODYWEIGHT,
                        defaultWeight = e.sets.lastOrNull()?.weightKg ?: rec?.weightKg,
                        timed = ex.timed,
                    ) { w, r -> vm.addSet(i, w, r) }
                    if (e.sets.isNotEmpty()) TextButton(onClick = { vm.removeLastSet(i) }) { Text("Poništi zadnju seriju") }
                }
            }
            Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth()) { Text("Završi trening") }
            TextButton(onClick = { confirmCancel = true }) { Text("Odustani od treninga") }
        }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Završiti trening?") },
            text = { Text("Upisane serije se spremaju, a preporuke težina ažuriraju.") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; vm.finishWorkout() }) { Text("Završi") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Natrag") } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Odustati od treninga?") },
            text = { Text("Upisane serije neće biti spremljene.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancelWorkout() }) { Text("Odustani") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Nastavi trening") } },
        )
    }
}

@Composable
private fun TimerPanel(a: ActiveWorkout, now: Long, exerciseName: String?, onEndSet: () -> Unit, onSkipRest: () -> Unit) {
    val t = a.timer
    val color = when (t?.kind) {
        TimerKind.WORK -> MaterialTheme.colorScheme.primaryContainer
        TimerKind.REST_SET, TimerKind.REST_EXERCISE -> MaterialTheme.colorScheme.tertiaryContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${a.name} · ukupno ${clock((now - a.startedAt) / 1000)}", style = MaterialTheme.typography.bodyMedium)
            if (t == null) {
                Text("Pritisnite „Počni seriju“ kod vježbe.", style = MaterialTheme.typography.bodyMedium)
            } else when (t.kind) {
                TimerKind.WORK -> {
                    Text("RAD", fontWeight = FontWeight.SemiBold)
                    Text(clock((now - t.startedAt) / 1000), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    exerciseName?.let { Text(it) }
                    Button(onClick = onEndSet, modifier = Modifier.fillMaxWidth()) {
                        Text("■ Kraj serije")
                    }
                }
                TimerKind.REST_SET, TimerKind.REST_EXERCISE -> {
                    val left = (t.startedAt + t.durationSec * 1000L - now) / 1000
                    val label = if (t.kind == TimerKind.REST_SET) "ODMOR (serije)" else "ODMOR (sljedeća vježba)"
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (left >= 0) clock(left) else "+${clock(left)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (left >= 0) "Odbrojava se odmor" else "Odmor je istekao", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onSkipRest) { Text("Zaustavi") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetInput(key: String, showWeight: Boolean, defaultWeight: Double?, timed: Boolean, onAdd: (Double?, Int) -> Unit) {
    var weight by remember(key) { mutableStateOf(defaultWeight?.let { kg(it) } ?: "") }
    var reps by remember(key) { mutableStateOf("") }
    val w = parseNumber(weight)
    val r = reps.toIntOrNull()
    val valid = r != null && r > 0 && (!showWeight || (w != null && w >= 0))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showWeight) NumberField("kg", weight, { weight = it }, Modifier.weight(1f))
        NumberField(if (timed) "sekunde" else "ponavljanja", reps, { reps = it }, Modifier.weight(1f), decimal = false)
    }
    Button(onClick = { onAdd(if (showWeight) w else null, r!!) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
        Text("Spremi seriju")
    }
}

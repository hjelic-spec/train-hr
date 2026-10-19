package hr.trainhr.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import hr.trainhr.core.LoadKind
import hr.trainhr.core.SetEntry
import hr.trainhr.core.kg

fun describeSets(sets: List<SetEntry>, timed: Boolean): String =
    sets.joinToString("  ") { s -> (s.weightKg?.let { "${kg(it)}×" } ?: "") + s.reps + if (timed) "s" else "" }

@Composable
fun WorkoutScreen(vm: AppViewModel, d: AppData, a: ActiveWorkout) {
    val catalog = vm.catalog(d)
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    BackHandler { confirmCancel = true }

    // Zaslon ostaje upaljen tijekom treninga.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    ScreenColumn {
        Title(a.name)
        a.entries.forEachIndexed { i, e ->
            val ex = catalog[e.exerciseId] ?: return@forEachIndexed
            val rec = vm.recommendation(d, ex.id)
            val last = vm.history(d, ex.id).lastOrNull()
            BoxCard {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                rec?.let { Text(it.text, style = MaterialTheme.typography.bodyMedium) }
                last?.let { (day, sets) ->
                    Text("Prošli put (${formatDate(day)}): ${describeSets(sets, ex.timed)}", style = MaterialTheme.typography.bodyMedium)
                }
                e.sets.forEachIndexed { si, s ->
                    Text("Serija ${si + 1}: ${describeSets(listOf(s), ex.timed)}")
                }
                SetInput(
                    key = "$i-${e.sets.size}",
                    showWeight = ex.kind != LoadKind.BODYWEIGHT,
                    defaultWeight = e.sets.lastOrNull()?.weightKg ?: rec?.weightKg,
                    timed = ex.timed,
                ) { vm.addSet(i, it) }
                if (e.sets.isNotEmpty()) TextButton(onClick = { vm.removeLastSet(i) }) { Text("Poništi zadnju seriju") }
            }
        }
        Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth()) { Text("Završi trening") }
        TextButton(onClick = { confirmCancel = true }) { Text("Odustani od treninga") }
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
private fun SetInput(key: String, showWeight: Boolean, defaultWeight: Double?, timed: Boolean, onAdd: (SetEntry) -> Unit) {
    var weight by remember(key) { mutableStateOf(defaultWeight?.let { kg(it) } ?: "") }
    var reps by remember(key) { mutableStateOf("") }
    val w = parseNumber(weight)
    val r = reps.toIntOrNull()
    val valid = r != null && r > 0 && (!showWeight || (w != null && w >= 0))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showWeight) NumberField("kg", weight, { weight = it }, Modifier.weight(1f))
        NumberField(if (timed) "sekunde" else "ponavljanja", reps, { reps = it }, Modifier.weight(1f), decimal = false)
    }
    Button(onClick = { onAdd(SetEntry(if (showWeight) w else null, r!!)) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
        Text("Dodaj seriju")
    }
}

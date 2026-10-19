package hr.trainhr.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.trainhr.core.Catalog
import hr.trainhr.core.DAY_OFFSETS
import hr.trainhr.core.DURATIONS
import hr.trainhr.core.Exercise
import hr.trainhr.core.Goal
import hr.trainhr.core.LoadKind
import hr.trainhr.core.MuscleGroup
import hr.trainhr.core.REST_EXERCISE_OPTIONS
import hr.trainhr.core.REST_SET_OPTIONS
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.Timing
import hr.trainhr.core.WARMUP_MINUTES
import hr.trainhr.core.kg
import hr.trainhr.core.pinnedPerWeek

private const val DAY_MS = 86_400_000L
private const val NO_END = 0

@Composable
fun SetupScreen(d: AppData, onSave: (SetupChoice) -> Unit, onCancel: (() -> Unit)?) {
    val firstSetup = d.startDay == null
    var goal by remember { mutableStateOf(d.goal) }
    var restSets by remember { mutableStateOf(d.restSets) }
    var restEx by remember { mutableStateOf(d.restExercises) }
    var days by remember { mutableStateOf(d.daysPerWeek) }
    var minutes by remember { mutableStateOf(d.durationMinutes) }
    var startDay by remember { mutableStateOf(d.startDay ?: today()) }
    var endDay by remember { mutableStateOf(if (firstSetup) today() + 8 * 7 - 1 else d.endDay) }
    var bodyWeight by remember { mutableStateOf(kg(d.bodyWeightKg)) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(d.selected) } }
    val pinned = remember { mutableStateListOf<String>().apply { addAll(d.pinned) } }
    val custom = remember { mutableStateListOf<Exercise>().apply { addAll(d.customExercises) } }
    var addingTo by remember { mutableStateOf<MuscleGroup?>(null) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" | "end"
    val catalog = Catalog(custom.toList())
    val capacity = Timing.capacity(minutes, goal, restSets, restEx)
    val weeksSel = when (val e = endDay) { null -> NO_END; else -> if ((e - startDay + 1) % 7 == 0L) ((e - startDay + 1) / 7).toInt() else -1 }
    val bw = parseNumber(bodyWeight)
    val endOk = endDay?.let { it >= startDay } ?: true

    ScreenColumn {
        Title("Postavke plana")

        Section("Cilj")
        Chips(Goal.entries, goal, { it.label }) { g ->
            // Odmori koji su bili na preporuci prate novi cilj; ručno podešeni ostaju.
            if (restSets == goal.restBetweenSetsSec) restSets = g.restBetweenSetsSec
            if (restEx == goal.restBetweenExercisesSec) restEx = g.restBetweenExercisesSec
            goal = g
        }
        Muted("$SETS_PER_EXERCISE serije × ${goal.repLabel} ponavljanja")

        Section("Početak treninga")
        OutlinedButton(onClick = { picking = "start" }) { Text("Datum prvog treninga: ${formatDate(startDay)}") }

        Section("Trajanje programa")
        Chips(listOf(4, 6, 8, 12, NO_END), weeksSel, { if (it == NO_END) "Bez kraja" else "$it tjedana" }) { w ->
            endDay = if (w == NO_END) null else startDay + w * 7L - 1
        }
        OutlinedButton(onClick = { picking = "end" }) { Text(endDay?.let { "Do datuma: ${formatDate(it)}" } ?: "Odaberi datum završetka") }
        Muted(
            when {
                endDay == null -> "Program traje dok ga ne promijenite."
                !endOk -> "Kraj je prije početka – odaberite kasniji datum."
                else -> "Program: ${formatDate(startDay)} – ${formatDate(endDay!!)} (${(endDay!! - startDay + 7) / 7} tj.)"
            },
        )

        Section("Broj treninga tjedno")
        Chips((1..5).toList(), days, { "$it" }) { days = it }
        Muted("Dani: ${DAY_OFFSETS.getValue(days).joinToString { weekday(startDay + it) }} (svaki tjedan isto).")

        Section("Trajanje treninga")
        Chips(DURATIONS, minutes, { "$it min" }) { minutes = it }

        Section("Odmor između serija")
        Chips(REST_SET_OPTIONS, restSets, { restLabel(it) + if (it == goal.restBetweenSetsSec) " ★" else "" }) { restSets = it }
        Section("Odmor između vježbi")
        Chips(REST_EXERCISE_OPTIONS, restEx, { restLabel(it) + if (it == goal.restBetweenExercisesSec) " ★" else "" }) { restEx = it }
        Muted("★ preporuka za ${goal.label.lowercase()}. U $minutes min stane do $capacity vježbi po treningu (uključeno $WARMUP_MINUTES min zagrijavanja).")
        if (restSets != goal.restBetweenSetsSec || restEx != goal.restBetweenExercisesSec) {
            TextButton(onClick = { restSets = goal.restBetweenSetsSec; restEx = goal.restBetweenExercisesSec }) { Text("Vrati preporučene odmore") }
        }

        Section("Tjelesna masa")
        Stepper("kg (za procjenu kalorija)", bodyWeight, { bodyWeight = it }, 1.0, decimal = true, min = 30.0)

        Section("Vježbe po mišićnim skupinama")
        Muted("Odaberite vježbe koje želite raditi – slobodno više nego što stane u jedan trening. Aplikacija ih svaki tjedan nanovo raspoređuje i miješa.")
        val perWeek = pinnedPerWeek(days)
        Muted("Stalna vježba ne miješa se: dolazi svaki tjedan u $perWeek ${if (perWeek == 1) "trening" else "treninga"}, kako biste joj pratili napredak. Preporuka: 1–3 glavne vježbe.")
        val pinnedPerDay = (pinned.size * perWeek + days - 1) / days
        if (pinnedPerDay > capacity) {
            BoxCard(warn = true) { Text("Previše stalnih vježbi: u trening ih treba ~$pinnedPerDay, a stane $capacity. Smanjite broj stalnih vježbi ili produljite trening.") }
        }
        MuscleGroup.entries.forEach { g ->
            val list = catalog.all.filter { it.group == g }
            val n = list.count { it.id in selected }
            BoxCard {
                Text(g.label + if (n > 0) " ($n)" else "", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                list.forEach { ex ->
                    val on = ex.id in selected
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).clickable {
                                if (on) { selected.remove(ex.id); pinned.remove(ex.id) } else selected.add(ex.id)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(on, null)
                            Text(ex.name)
                        }
                        if (on) {
                            val pin = ex.id in pinned
                            FilterChip(selected = pin, onClick = { if (pin) pinned.remove(ex.id) else pinned.add(ex.id) },
                                label = { Text(if (pin) "✓ Stalna" else "Stalna") })
                        }
                    }
                }
                TextButton(onClick = { addingTo = g }) { Text("+ Vlastita vježba") }
            }
        }
        Button(
            onClick = {
                onSave(SetupChoice(goal, days, minutes, restSets, restEx, startDay, endDay,
                    bw?.takeIf { it in 20.0..300.0 } ?: d.bodyWeightKg, selected.toList(), pinned.toList(), custom.toList()))
            },
            enabled = selected.isNotEmpty() && endOk,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Spremi i složi plan") }
        if (onCancel != null) TextButton(onClick = onCancel) { Text("Odustani") }
    }

    addingTo?.let { g ->
        CustomExerciseDialog(g, onDismiss = { addingTo = null }) { ex ->
            custom.add(ex); selected.add(ex.id); addingTo = null
        }
    }
    picking?.let { which ->
        DateDialog(if (which == "start") startDay else (endDay ?: startDay + 55), onDismiss = { picking = null }) { day ->
            if (which == "start") {
                val len = endDay?.let { it - startDay } // zadrži trajanje programa
                startDay = day
                if (len != null) endDay = day + len
            } else endDay = day
            picking = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial * DAY_MS)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onPick(it / DAY_MS) } ?: onDismiss() }) { Text("U redu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    ) { DatePicker(state) }
}

@Composable
private fun CustomExerciseDialog(group: MuscleGroup, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LoadKind.DUMBBELL) }
    var pair by remember { mutableStateOf(true) }
    var timed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vlastita vježba – ${group.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Naziv") }, singleLine = true)
                Text("Opterećenje")
                Chips(LoadKind.entries, kind, { it.label }) { kind = it }
                if (kind == LoadKind.DUMBBELL) {
                    Row(Modifier.clickable { pair = !pair }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(pair, null); Text("Dvije bučice (računa se dvostruko)")
                    }
                }
                Row(Modifier.clickable { timed = !timed }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(timed, null); Text("Bilježi se u sekundama")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(Exercise("custom_${System.currentTimeMillis()}", name.trim(), group, kind, timed,
                    pair = if (kind == LoadKind.DUMBBELL) pair else false))
            }) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}

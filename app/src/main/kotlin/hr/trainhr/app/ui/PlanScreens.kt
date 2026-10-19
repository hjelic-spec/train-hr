package hr.trainhr.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.trainhr.app.data.AppData
import hr.trainhr.core.model.Exercise
import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.Focus
import hr.trainhr.core.model.LoadType
import hr.trainhr.core.model.MovementPattern
import hr.trainhr.core.model.MuscleGroup
import hr.trainhr.core.model.PlanMode
import hr.trainhr.core.model.PlanSettings
import hr.trainhr.core.model.Prescription
import hr.trainhr.core.model.TrainingPlan
import hr.trainhr.core.plan.PlanGenerator
import hr.trainhr.core.plan.PlanValidator
import hr.trainhr.core.util.formatKg

@Composable
fun PlanSetupScreen(initial: PlanSettings, onCreate: (PlanSettings) -> Unit, onCancel: (() -> Unit)?) {
    var days by remember { mutableStateOf(initial.daysPerWeek) }
    var minutes by remember { mutableStateOf(initial.durationMinutes) }
    var focus by remember { mutableStateOf(initial.focus) }
    var mode by remember { mutableStateOf(initial.mode) }

    ScreenColumn {
        Text("Postavke plana", style = MaterialTheme.typography.headlineSmall)
        SectionTitle("Treninga tjedno")
        ChoiceChips(listOf(2, 3, 4), days, { "$it" }) { days = it }
        Text("Preporuka (ACSM 2026): svaka glavna mišićna skupina najmanje 2× tjedno.", style = MaterialTheme.typography.bodyMedium)
        SectionTitle("Trajanje treninga")
        ChoiceChips(listOf(30, 45, 60), minutes, { "$it min" }) { minutes = it }
        SectionTitle("Fokus")
        ChoiceChips(Focus.entries, focus, { it.label }) { focus = it }
        SectionTitle("Način rada")
        ChoiceChips(PlanMode.entries, mode, { it.label }) { mode = it }
        Text(mode.description, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = { onCreate(initial.copy(daysPerWeek = days, durationMinutes = minutes, focus = focus, mode = mode)) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text(if (mode == PlanMode.MANUAL) "Napravi prazan plan" else "Složi plan") }
        if (onCancel != null) TextButton(onClick = onCancel) { Text("Odustani") }
    }
}

private fun Prescription.describe(timed: Boolean): String {
    val unit = if (timed) " s" else ""
    return "$sets × $repMin–$repMax$unit, RIR $targetRir, odmor $restSeconds s"
}

@Composable
fun PlanScreen(vm: AppViewModel, d: AppData, onChangeSettings: () -> Unit) {
    val plan = d.plan ?: return
    val profile = d.profile ?: return
    val catalog = vm.catalog(d)
    val editable = plan.settings.mode != PlanMode.AUTOMATIC
    var editing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var addingTo by remember { mutableStateOf<Int?>(null) }

    fun update(transform: (TrainingPlan) -> TrainingPlan) = vm.updatePlan(transform(plan))

    ScreenColumn {
        Text("Plan", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${plan.settings.daysPerWeek}× tjedno · ${plan.settings.durationMinutes} min · ${plan.settings.focus.label} · " +
                "način: ${plan.settings.mode.label}",
        )
        val warnings = PlanValidator.warnings(plan, profile, catalog)
        if (warnings.isNotEmpty()) InfoCard(warnings.joinToString("\n• ", prefix = "• "), warning = true)
        if (!editable) {
            OutlinedButton(onClick = {
                update { it.copy(settings = it.settings.copy(mode = PlanMode.CUSTOMIZED)) }
            }) { Text("Prilagodi plan ručno") }
        }
        plan.workouts.forEachIndexed { wi, w ->
            SectionTitle("${w.name} (~${PlanValidator.estimatedMinutes(w, catalog)} min)")
            ContentCard {
                Text("Zagrijavanje: ${w.warmup}", style = MaterialTheme.typography.bodyMedium)
                w.items.forEachIndexed { ii, p ->
                    val ex = catalog[p.exerciseId]
                    HorizontalDivider()
                    Text(ex?.name ?: p.exerciseId, style = MaterialTheme.typography.titleMedium)
                    Text(p.describe(ex?.timed == true), style = MaterialTheme.typography.bodyMedium)
                    val weight = vm.suggestedWeight(d, p)
                    if (ex != null && ex.loadType != LoadType.NONE) {
                        Text(
                            weight?.let { "Radna težina: ${formatKg(it)} ${loadUnit(ex.loadType)}" }
                                ?: "Težina: kalibracija na prvom treningu",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (!p.autoProgression) Text("Automatska progresija isključena", style = MaterialTheme.typography.bodyMedium)
                    if (editable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { editing = wi to ii }) { Text("Uredi") }
                            TextButton(enabled = ii > 0, onClick = { update { it.moveItem(wi, ii, ii - 1) } }) { Text("Gore") }
                            TextButton(enabled = ii < w.items.lastIndex, onClick = { update { it.moveItem(wi, ii, ii + 1) } }) { Text("Dolje") }
                            TextButton(onClick = { update { it.removeItem(wi, ii) } }) { Text("Ukloni") }
                        }
                    }
                }
                if (editable) {
                    OutlinedButton(onClick = { addingTo = wi }) { Text("Dodaj vježbu") }
                }
            }
        }
        OutlinedButton(onClick = onChangeSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Promijeni postavke i složi novi plan")
        }
    }

    editing?.let { (wi, ii) ->
        val p = plan.workouts[wi].items[ii]
        PrescriptionDialog(
            p, catalog[p.exerciseId]?.timed == true,
            onDismiss = { editing = null },
            onSave = { np -> update { it.replaceItem(wi, ii, np) }; editing = null },
        )
    }
    addingTo?.let { wi ->
        ExercisePickerDialog(
            vm, d, catalog,
            onDismiss = { addingTo = null },
            onPick = { ex ->
                val np = PlanGenerator.prescribe(ex, profile.experience)
                update { it.addItem(wi, np) }
                addingTo = null
            },
        )
    }
}

private fun TrainingPlan.mapWorkout(wi: Int, f: (List<Prescription>) -> List<Prescription>) =
    copy(workouts = workouts.mapIndexed { i, w -> if (i == wi) w.copy(items = f(w.items)) else w })

private fun TrainingPlan.moveItem(wi: Int, from: Int, to: Int) = mapWorkout(wi) { items ->
    items.toMutableList().apply { add(to, removeAt(from)) }
}

private fun TrainingPlan.removeItem(wi: Int, ii: Int) = mapWorkout(wi) { it.filterIndexed { i, _ -> i != ii } }

private fun TrainingPlan.replaceItem(wi: Int, ii: Int, p: Prescription) =
    mapWorkout(wi) { items -> items.mapIndexed { i, old -> if (i == ii) p else old } }

private fun TrainingPlan.addItem(wi: Int, p: Prescription) = mapWorkout(wi) { it + p }

@Composable
private fun PrescriptionDialog(p: Prescription, timed: Boolean, onDismiss: () -> Unit, onSave: (Prescription) -> Unit) {
    var sets by remember { mutableStateOf(p.sets.toString()) }
    var min by remember { mutableStateOf(p.repMin.toString()) }
    var max by remember { mutableStateOf(p.repMax.toString()) }
    var rest by remember { mutableStateOf(p.restSeconds.toString()) }
    var rir by remember { mutableStateOf(p.targetRir) }
    var auto by remember { mutableStateOf(p.autoProgression) }
    val s = sets.toIntOrNull()
    val lo = min.toIntOrNull()
    val hi = max.toIntOrNull()
    val re = rest.toIntOrNull()
    val valid = s != null && s in 1..8 && lo != null && hi != null && lo in 1..hi && re != null && re in 0..600
    val unit = if (timed) "sekunde" else "ponavljanja"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uredi vježbu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NumberField("Serije", sets, { sets = it }, decimal = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Min $unit", min, { min = it }, Modifier.weight(1f), decimal = false)
                    NumberField("Max $unit", max, { max = it }, Modifier.weight(1f), decimal = false)
                }
                NumberField("Odmor (s)", rest, { rest = it }, decimal = false)
                Text("Ciljani RIR")
                ChoiceChips((0..4).toList(), rir, { it.toString() }) { rir = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(auto, { auto = it })
                    Text("  Automatska progresija težine")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(p.copy(sets = s!!, repMin = lo!!, repMax = hi!!, restSeconds = re!!, targetRir = rir, autoProgression = auto))
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}

@Composable
private fun ExercisePickerDialog(
    vm: AppViewModel,
    d: AppData,
    catalog: ExerciseCatalog,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
) {
    val profile = d.profile ?: return
    var pattern by remember { mutableStateOf<MovementPattern?>(null) }
    var creating by remember { mutableStateOf(false) }
    val options = catalog.available(profile.equipment.available)
        .filter { pattern == null || it.pattern == pattern }
        .sortedBy { it.name }

    if (creating) {
        CustomExerciseDialog(
            onDismiss = { creating = false },
            onSave = { ex -> vm.addCustomExercise(ex); creating = false; onPick(ex) },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj vježbu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChips(listOf<MovementPattern?>(null) + MovementPattern.entries, pattern, { it?.label ?: "Sve" }) { pattern = it }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(options, key = { it.id }) { ex ->
                        val risky = ex.stresses.intersect(profile.limitations)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(ex) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(ex.name)
                            Text(
                                ex.pattern.label + if (risky.isNotEmpty()) " · oprez: ${risky.joinToString { it.label.lowercase() }}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { creating = true }) { Text("Nova vlastita vježba") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } },
    )
}

@Composable
private fun CustomExerciseDialog(onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf(MovementPattern.ARMS) }
    var load by remember { mutableStateOf(LoadType.DUMBBELL) }
    var muscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var timed by remember { mutableStateOf(false) }
    val loadLabels = mapOf(
        LoadType.DUMBBELL to "Bučice", LoadType.BARBELL to "Šipka", LoadType.TRAP_BAR to "Trap bar",
        LoadType.LAT_STACK to "Lat mašina", LoadType.CABLE_STACK to "Sajla", LoadType.NONE to "Bez opterećenja",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vlastita vježba") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text("Naziv") }, singleLine = true)
                        Text("Obrazac pokreta")
                        ChoiceChips(MovementPattern.entries, pattern, { it.label }) { pattern = it }
                        Text("Opterećenje")
                        ChoiceChips(LoadType.entries, load, { loadLabels.getValue(it) }) { load = it }
                        Text("Mišićne skupine")
                        MultiChips(MuscleGroup.entries, muscles, { it.label }) { m ->
                            muscles = if (m in muscles) muscles - m else muscles + m
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(timed, { timed = it })
                            Text("  Na vrijeme (sekunde)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && muscles.isNotEmpty(), onClick = {
                onSave(
                    Exercise(
                        id = "custom_" + System.currentTimeMillis(),
                        name = name.trim(),
                        pattern = pattern,
                        muscles = muscles,
                        equipment = emptySet(),
                        loadType = load,
                        compound = pattern !in setOf(MovementPattern.ARMS, MovementPattern.SHOULDERS, MovementPattern.CALF),
                        timed = timed,
                        custom = true,
                    ),
                )
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}

/** Za ručni plan bez vježbi – pokazuje se umjesto praznog popisa. */
fun TrainingPlan.isEmpty() = workouts.all { it.items.isEmpty() }

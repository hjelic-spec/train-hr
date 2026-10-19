package hr.trainhr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import hr.trainhr.app.data.AppData
import hr.trainhr.core.model.LoadType
import hr.trainhr.core.model.SetLog
import hr.trainhr.core.util.formatKg
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(vm: AppViewModel, d: AppData) {
    val plan = d.plan ?: return
    val lastName = d.sessions.lastOrNull()?.workoutName
    val lastIndex = plan.workouts.indexOfFirst { it.name == lastName }
    val nextIndex = if (lastIndex < 0) 0 else (lastIndex + 1) % plan.workouts.size

    ScreenColumn {
        Text("Danas", style = MaterialTheme.typography.headlineSmall)
        d.profile?.name?.takeIf { it.isNotBlank() }?.let { Text("Pozdrav, $it!") }
        Text("Tjedan ${vm.weekIndex(d) + 1} plana")
        if (vm.isDeloadWeek(d)) {
            InfoCard("Ovaj tjedan je lakši (deload): serija manje i oko 10 % manja težina. Oporavak je dio napretka.")
        }
        val sinceExport = d.lastExportEpochDay?.let { AppViewModel.today() - it }
        if (d.sessions.isNotEmpty() && (sinceExport == null || sinceExport > 30)) {
            InfoCard("Podaci su samo na ovom telefonu. Napravite sigurnosnu kopiju u Postavkama (Izvoz).", warning = true)
        }
        if (plan.isEmpty()) {
            InfoCard("Plan još nema vježbi. Dodajte ih na kartici Plan.")
            return@ScreenColumn
        }
        plan.workouts.forEachIndexed { i, w ->
            ContentCard {
                Text(w.name + if (i == nextIndex) "  · sljedeći" else "", style = MaterialTheme.typography.titleMedium)
                Text("${w.items.size} vježbi", style = MaterialTheme.typography.bodyMedium)
                if (i == nextIndex) {
                    Button(onClick = { vm.startWorkout(i) }, enabled = w.items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("Započni trening")
                    }
                } else {
                    OutlinedButton(onClick = { vm.startWorkout(i) }, enabled = w.items.isNotEmpty()) { Text("Započni") }
                }
            }
        }
    }
}

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
        Text(a.name, style = MaterialTheme.typography.headlineSmall)
        if (a.deload) InfoCard("Lakši tjedan – težine i broj serija su smanjeni.")
        Text("Zagrijavanje: ${a.warmup}", style = MaterialTheme.typography.bodyMedium)
        RestTimer(a.restEndsAtMillis, onSkip = vm::skipRest)

        a.entries.forEachIndexed { i, e ->
            val ex = catalog[e.prescription.exerciseId]
            val p = e.prescription
            val timed = ex?.timed == true
            ContentCard {
                Text(ex?.name ?: p.exerciseId, style = MaterialTheme.typography.titleMedium)
                if (!ex?.cue.isNullOrBlank()) Text(ex!!.cue, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${e.targetSets} × ${p.repMin}–${p.repMax}${if (timed) " s" else ""} · RIR ${p.targetRir} · odmor ${p.restSeconds} s",
                )
                val type = ex?.loadType ?: LoadType.NONE
                when {
                    type == LoadType.NONE -> {}
                    e.suggestedWeightKg != null -> Text("Predložena težina: ${formatKg(e.suggestedWeightKg)} ${loadUnit(type)}")
                    else -> InfoCard(
                        "Kalibracija: izaberite težinu s kojom možete ~${p.repMax} ponavljanja uz 2–3 u rezervi. " +
                            "Aplikacija će iz toga postaviti radnu težinu.",
                    )
                }
                e.logged.forEachIndexed { si, s ->
                    Text(
                        "Serija ${si + 1}: " + (s.weightKg?.let { "${formatKg(it)} kg × " } ?: "") +
                            "${s.reps}${if (timed) " s" else ""}" + (s.rir?.let { ", RIR $it" } ?: "") +
                            if (s.pain > 0) ", bol ${s.pain}" else "",
                    )
                }
                SetInput(
                    key = "$i-${e.logged.size}",
                    showWeight = type != LoadType.NONE,
                    defaultWeight = e.logged.lastOrNull()?.weightKg ?: e.suggestedWeightKg,
                    timed = timed,
                    done = e.logged.size >= e.targetSets,
                    onLog = { vm.logSet(i, it) },
                )
                if (e.logged.isNotEmpty()) TextButton(onClick = { vm.removeLastSet(i) }) { Text("Poništi zadnju seriju") }
            }
        }
        Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Završi trening")
        }
        TextButton(onClick = { confirmCancel = true }) { Text("Odustani od treninga") }
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Završiti trening?") },
            text = { Text("Serije se spremaju, a težine za sljedeći trening računaju prema rezultatima.") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; vm.finishWorkout() }) { Text("Završi") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Natrag") } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Odustati od treninga?") },
            text = { Text("Upisane serije neće biti spremljene.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancelWorkout() }) { Text("Odustani od treninga") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Nastavi trening") } },
        )
    }
}

@Composable
private fun SetInput(
    key: String,
    showWeight: Boolean,
    defaultWeight: Double?,
    timed: Boolean,
    done: Boolean,
    onLog: (SetLog) -> Unit,
) {
    var weight by remember(key) { mutableStateOf(defaultWeight?.let { formatKg(it) } ?: "") }
    var reps by remember(key) { mutableStateOf("") }
    var rir by remember(key) { mutableStateOf<Int?>(null) }
    var pain by remember(key) { mutableStateOf(0) }
    val w = parseNumber(weight)
    val r = reps.toIntOrNull()
    val valid = r != null && r >= 0 && (!showWeight || (w != null && w >= 0)) && (timed || rir != null)

    if (done) Text("Sve planirane serije su odrađene. Možete dodati još jednu ako želite.", style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showWeight) NumberField("kg", weight, { weight = it }, Modifier.weight(1f))
        NumberField(if (timed) "sekunde" else "ponavljanja", reps, { reps = it }, Modifier.weight(1f), decimal = false)
    }
    if (!timed) {
        Text("U rezervi (RIR)", style = MaterialTheme.typography.bodyMedium)
        ChoiceChips((0..4).toList(), rir, { if (it == 4) "4+" else it.toString() }) { rir = it }
    }
    Text("Bol (0–10)", style = MaterialTheme.typography.bodyMedium)
    ChoiceChips(listOf(0, 1, 2, 3, 5, 7), pain, { it.toString() }) { pain = it }
    Button(
        onClick = { onLog(SetLog(if (showWeight) w else null, r!!, if (timed) null else rir, pain)) },
        enabled = valid,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Spremi seriju") }
}

@Composable
private fun RestTimer(endsAt: Long?, onSkip: () -> Unit) {
    if (endsAt == null) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (System.currentTimeMillis() < endsAt) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val left = ((endsAt - now) / 1000).coerceAtLeast(0)
    if (left > 0) {
        ContentCard {
            Text("Odmor: ${left / 60}:${(left % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onSkip) { Text("Preskoči odmor") }
        }
    } else {
        InfoCard("Odmor je gotov – sljedeća serija.")
    }
}

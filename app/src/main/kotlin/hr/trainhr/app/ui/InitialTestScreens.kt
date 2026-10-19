package hr.trainhr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.trainhr.app.data.AppData
import hr.trainhr.core.model.LoadType
import hr.trainhr.core.test.FunctionalTest
import hr.trainhr.core.test.InitialTest
import hr.trainhr.core.test.TestAttempt
import hr.trainhr.core.test.Verdict
import hr.trainhr.core.util.formatKg

fun loadUnit(type: LoadType): String = when (type) {
    LoadType.DUMBBELL -> "kg po bučici"
    LoadType.BARBELL, LoadType.TRAP_BAR -> "kg ukupno (sa šipkom)"
    LoadType.LAT_STACK, LoadType.CABLE_STACK -> "kg na mašini"
    LoadType.NONE -> ""
}

@Composable
fun InitialTestScreen(vm: AppViewModel, d: AppData, retest: Boolean, onClose: (() -> Unit)?) {
    var openExercise by remember { mutableStateOf<String?>(null) }
    val profile = d.profile ?: return
    val catalog = vm.catalog(d)

    openExercise?.let { id ->
        BackHandler { openExercise = null }
        TestExerciseScreen(vm, d, id, onDone = { openExercise = null })
        return
    }
    if (onClose != null) BackHandler { onClose() }

    val items = InitialTest.exercisesFor(profile, catalog)
    ScreenColumn {
        Text(if (retest) "Ponovni test" else "Početni test", style = MaterialTheme.typography.headlineSmall)
        InfoCard(
            "Test je obavezan i određuje radne težine. Ne dižete maksimum: za svaku vježbu birate težinu " +
                "s kojom možete napraviti 4–10 čistih ponavljanja i stajete kad bi ostalo još oko jedno. " +
                "Aplikacija iz toga procjenjuje maksimum (1RM). Test je podijeljen u dva dana.",
        )
        InfoCard(
            "Odmah prekinite kod boli u prsima, vrtoglavice ili neuobičajenog nedostatka zraka.",
            warning = true,
        )
        for (day in listOf(1, 2)) {
            SectionTitle("Dan $day")
            items.filter { it.first == day }.forEach { (_, id) ->
                val ex = catalog[id] ?: return@forEach
                val result = d.test.results[id]
                ContentCard {
                    Text(ex.name, style = MaterialTheme.typography.titleMedium)
                    when {
                        result != null -> Text(
                            "Procjena 1RM: ${formatKg(result.estimatedOneRmKg)} kg " +
                                "(${formatKg(result.weightKg)} kg × ${result.reps}, RIR ${result.rir}; pouzdanost ${result.confidence.label})",
                        )
                        id in d.test.skipped -> Text("Preskočeno – težina će se kalibrirati na prvom treningu.")
                        else -> Text("Nije testirano.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { openExercise = id }) {
                            Text(if (result == null && id !in d.test.skipped) "Testiraj" else "Ponovi")
                        }
                    }
                }
            }
        }
        SectionTitle("Funkcionalni testovi")
        FunctionalTestCard(d.test.functional) { vm.saveFunctional(it) }

        val complete = vm.testComplete(d)
        Button(
            onClick = { vm.finishTest(); onClose?.invoke() },
            enabled = complete,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("Završi test") }
        if (!complete) Text("Za završetak testirajte (ili preskočite) sve vježbe i upišite funkcionalne testove.")
        if (onClose != null) TextButton(onClick = onClose) { Text("Natrag") }
    }
}

@Composable
private fun FunctionalTestCard(initial: FunctionalTest?, onSave: (FunctionalTest) -> Unit) {
    var chair by remember { mutableStateOf(initial?.chairStands30s?.toString() ?: "") }
    var left by remember { mutableStateOf(initial?.balanceLeftSec?.toString() ?: "") }
    var right by remember { mutableStateOf(initial?.balanceRightSec?.toString() ?: "") }
    val c = chair.toIntOrNull()
    val l = left.toIntOrNull()?.coerceIn(0, 30)
    val r = right.toIntOrNull()?.coerceIn(0, 30)
    ContentCard {
        Text("Ustajanje sa stolice: broj punih ustajanja u 30 s, ruke prekrižene na prsima.")
        NumberField("Ustajanja u 30 s", chair, { chair = it }, Modifier.fillMaxWidth(), decimal = false)
        Text("Stajanje na jednoj nozi: sekunde (najviše 30), uz oslonac na dohvat ruke.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Lijeva (s)", left, { left = it }, Modifier.weight(1f), decimal = false)
            NumberField("Desna (s)", right, { right = it }, Modifier.weight(1f), decimal = false)
        }
        Button(onClick = { onSave(FunctionalTest(c!!, l!!, r!!)) }, enabled = c != null && l != null && r != null) {
            Text(if (initial == null) "Spremi" else "Spremljeno – ažuriraj")
        }
    }
}

@Composable
fun TestExerciseScreen(vm: AppViewModel, d: AppData, exerciseId: String, onDone: () -> Unit) {
    val profile = d.profile ?: return
    val catalog = vm.catalog(d)
    val loads = vm.loads(d)
    val ex = catalog[exerciseId] ?: return
    val attempts = remember { mutableStateListOf<TestAttempt>() }
    var weight by remember { mutableStateOf(loads.minimum(ex.loadType)?.let { formatKg(it) } ?: "") }
    var reps by remember { mutableStateOf("") }
    var rir by remember { mutableStateOf<Int?>(null) }
    var pain by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

    val w = parseNumber(weight)
    val r = reps.toIntOrNull()
    val warnings = InitialTest.warnings(exerciseId, profile, catalog)

    ScreenColumn {
        Text(ex.name, style = MaterialTheme.typography.headlineSmall)
        if (ex.cue.isNotBlank()) Text(ex.cue)
        if (warnings.isNotEmpty()) {
            InfoCard(
                "Ova vježba opterećuje: ${warnings.joinToString { it.label.lowercase() }} (navedeno u ograničenjima). " +
                    "Ako osjetite bol, preskočite je.",
                warning = true,
            )
        }
        InfoCard(
            "1) Zagrijte se 5–8 min na ergometru i napravite 2 lagane serije.\n" +
                "2) Izaberite težinu za 4–10 čistih ponavljanja.\n" +
                "3) Stanite kad bi ostalo još ~1 ponavljanje (ne do otkaza).\n" +
                "Najviše ${InitialTest.MAX_ATTEMPTS} pokušaja, 3 min odmora između.",
        )
        attempts.forEachIndexed { i, a ->
            Text("Pokušaj ${i + 1}: ${formatKg(a.weightKg)} kg × ${a.reps}, RIR ${a.rir}, bol ${a.pain}")
        }
        message?.let { InfoCard(it, warning = !finished) }

        if (!finished) {
            NumberField("Težina (${loadUnit(ex.loadType)})", weight, { weight = it }, Modifier.fillMaxWidth())
            NumberField("Ponavljanja", reps, { reps = it }, Modifier.fillMaxWidth(), decimal = false)
            Text("Koliko ste još ponavljanja mogli napraviti (RIR)?")
            ChoiceChips((0..5).toList(), rir, { if (it == 5) "5+" else it.toString() }) { rir = it }
            Text("Bol tijekom vježbe (0 = bez boli, 10 = najjača)")
            ChoiceChips((0..10).toList(), pain, { it.toString() }) { pain = it }
            Button(
                onClick = {
                    attempts += TestAttempt(w!!, r!!, rir!!, pain)
                    val e = InitialTest.evaluate(exerciseId, attempts.toList(), loads, catalog)
                    message = e.message
                    when (e.verdict) {
                        Verdict.DONE -> {
                            e.result?.let { vm.saveTestResult(it) }
                            finished = true
                            message = e.message + e.result?.let { " Procjena 1RM: ${formatKg(it.estimatedOneRmKg)} kg." }.orEmpty()
                        }
                        Verdict.STOP_PAIN -> {
                            vm.skipTestExercise(exerciseId)
                            finished = true
                        }
                        Verdict.TOO_LIGHT, Verdict.TOO_HEAVY -> {
                            e.nextWeightKg?.let { weight = formatKg(it) }
                            reps = ""
                            rir = null
                            pain = 0
                        }
                    }
                },
                enabled = w != null && w > 0 && r != null && r >= 0 && rir != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Spremi pokušaj") }
            OutlinedButton(
                onClick = { vm.skipTestExercise(exerciseId); onDone() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ne mogu izvesti – preskoči") }
        } else {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Natrag na test") }
        }
        TextButton(onClick = onDone) { Text("Odustani") }
    }
}

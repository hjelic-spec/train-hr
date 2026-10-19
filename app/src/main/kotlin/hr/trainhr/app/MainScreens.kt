package hr.trainhr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.trainhr.core.Action
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.SetEntry
import hr.trainhr.core.Timing
import hr.trainhr.core.kg
import hr.trainhr.core.setVolume
import hr.trainhr.core.workingSets
import kotlin.math.roundToInt

fun describeSets(sets: List<SetEntry>, timed: Boolean): String =
    sets.joinToString("  ") { s -> (s.weightKg?.let { "${kg(it)}×" } ?: "") + s.reps + if (timed) "s" else "" }

private fun statLine(s: Stats) = "${s.minutes} min · ${s.sets} serija · ${formatInt(s.kg)} kg · ≈${formatInt(s.kcal)} kcal"

// ===== Početna =====

@Composable
fun HomeScreen(vm: AppViewModel, d: AppData, onSettings: () -> Unit, onOpenSession: (Long) -> Unit, onNewProgram: () -> Unit) {
    val t = today()
    val c = d.catalog()
    val cur = maxOf(0, d.weekOf(t))
    val next = d.nextTarget(t)
    val pw = d.programWeeks()
    val last = d.sessions.maxWithOrNull(compareBy<Session> { it.epochDay }.thenBy { it.id })
    ScreenColumn {
        TitleBar("Početna", onSettings)
        BoxCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${d.goal.label} · ${d.daysPerWeek}× tjedno", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Muted(if (d.start > t) "počinje ${formatShort(d.start)}" else if (pw != null) "tjedan ${minOf(cur + 1, pw)} od $pw" else "tjedan ${cur + 1}")
            }
            if (pw != null && d.endDay != null) {
                Meter(((t - d.start + 1).toFloat() / (pw * 7)))
                Muted("${formatDate(d.start)} – ${formatDate(d.endDay)}")
            }
            if (d.start <= t) {
                val ks = (0 until d.daysPerWeek).filter { d.inProgram(cur, it) }
                Muted("Ovaj tjedan: ${ks.count { d.sessionFor(cur, it) != null }} od ${ks.size} treninga")
            }
        }
        if (next != null) {
            val exs = d.planForWeek(next.week).days[next.k].mapNotNull { c[it] }
            val day = d.dayDate(next.week, next.k)
            BoxCard(highlight = true) {
                Badge(if (day == t) "Danas" else "Sljedeći trening")
                Text("${formatShort(day)} · Tjedan ${next.week + 1} · Trening ${'A' + next.k}", style = MaterialTheme.typography.titleMedium)
                Muted("~${Timing.workoutMinutes(exs, d.goal, d.restSets, d.restExercises)} min · ${exs.size} vježbi: ${exs.joinToString { it.name }}")
                Button(onClick = { vm.startWorkout(next.week, next.k) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (day == t) "Započni trening" else "Započni sada")
                }
            }
        } else if (d.endDay != null && t > d.endDay) {
            BoxCard {
                Text("Program je završen", style = MaterialTheme.typography.titleMedium)
                Muted("${formatDate(d.start)} – ${formatDate(d.endDay)}. Postavite novi program ili produljite postojeći.")
                Button(onClick = onNewProgram, modifier = Modifier.fillMaxWidth()) { Text("Novi program") }
            }
        }
        BoxCard {
            if (last == null) {
                Text("Posljednji trening", style = MaterialTheme.typography.titleMedium)
                Muted("Još nema odrađenih treninga.")
            } else {
                Badge("Posljednji trening")
                Text("${formatShort(last.epochDay)} · ${last.workoutName}", style = MaterialTheme.typography.titleMedium)
                Muted(statLine(d.stats(last)))
                last.entries.forEach { e ->
                    val ex = c[e.exerciseId] ?: return@forEach
                    val r = d.recommendation(ex.id)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name)
                            Muted(describeSets(workingSets(e.sets), ex.timed))
                        }
                        val x = extra()
                        when (r?.action) {
                            Action.INCREASE -> Text("↑ " + (r.weightKg?.let { "${kg(it)} kg" } ?: "teže"), color = x.work, fontWeight = FontWeight.Bold)
                            Action.DECREASE -> Text("↓ ${kg(r.weightKg ?: 0.0)} kg", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            else -> Text("=", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Muted("↑ povećati sljedeći put · = zadržati · ↓ smanjiti")
                TextButton(onClick = { onOpenSession(last.id) }) { Text("Detalji treninga") }
            }
        }
    }
}

// ===== Plan =====

@Composable
fun PlanScreen(vm: AppViewModel, d: AppData, onSettings: () -> Unit) {
    val t = today()
    val c = d.catalog()
    val cur = maxOf(0, d.weekOf(t))
    val lw = d.lastWeek()
    var shown by rememberSaveable { mutableStateOf<Int?>(null) }
    val w = minOf(shown ?: cur, lw ?: Int.MAX_VALUE).coerceAtLeast(0)
    val plan = d.planForWeek(w)
    val next = d.nextTarget(t)
    val notStarted = d.start > t
    val g = d.goal
    ScreenColumn {
        TitleBar("Plan – ${g.label}", onSettings)
        Muted("${d.daysPerWeek}× tjedno · ${d.durationMinutes} min · $SETS_PER_EXERCISE × ${g.repLabel} pon. · odmor ${restLabel(d.restSets)} / ${restLabel(d.restExercises)}")
        if (notStarted) BoxCard { Text("Početak: ${formatDate(d.start)} (za ${d.start - t} ${if (d.start - t == 1L) "dan" else "dana"}).") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { shown = maxOf(0, w - 1) }, enabled = w > 0) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
            Text(
                "Tjedan ${w + 1}" + (lw?.let { " od ${it + 1}" } ?: "") + if (w == cur && !notStarted) " · ovaj tjedan" else "",
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { shown = w + 1 }, enabled = lw == null || w < lw) { Text("›", style = MaterialTheme.typography.headlineSmall) }
        }
        Muted("${formatShort(d.dayDate(w, 0))} – ${formatShort(d.dayDate(w, d.daysPerWeek - 1))}")
        if (plan.days.indices.any { d.sessionFor(w, it) == null && d.inProgram(w, it) }) {
            OutlinedButton(onClick = { vm.reshuffle(w) }) { Text("⟳ Promiješaj neodrađene treninge") }
        }
        if (plan.omitted.isNotEmpty()) {
            BoxCard(warn = true) { Text("Ovaj tjedan ne stane: ${plan.omitted.joinToString { it.name }}. Doći će na red idući tjedan.") }
        }
        plan.days.forEachIndexed { k, ids ->
            if (!d.inProgram(w, k)) return@forEachIndexed
            val day = d.dayDate(w, k)
            val done = d.sessionFor(w, k)
            val isNext = next?.week == w && next.k == k
            val exs = (done?.entries?.map { it.exerciseId } ?: ids).mapNotNull { c[it] }
            BoxCard(highlight = isNext && done == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${formatShort(day)} · Trening ${'A' + k}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    val x = extra()
                    when {
                        done != null -> Badge("✓ Odrađeno", x.work)
                        day == t -> Badge("Danas", x.rest)
                        day < t -> Badge("Propušteno", MaterialTheme.colorScheme.onSurfaceVariant)
                        isNext -> Badge("Sljedeći")
                    }
                }
                if (done != null) Muted(done.durationSeconds?.let { "${it / 60} min" } ?: "")
                else Muted("~${Timing.workoutMinutes(exs, g, d.restSets, d.restExercises)} min · ${exs.size} vježbi")
                exs.forEachIndexed { i, e ->
                    val r = if (done == null) d.recommendation(e.id) else null
                    Row {
                        Text("${i + 1}. ${e.name}" + if (e.id in d.pinned) "  · stalna" else "", modifier = Modifier.weight(1f))
                        r?.weightKg?.let { Muted("${kg(it)} kg") }
                    }
                }
                if (done == null) {
                    if (isNext) Button(onClick = { vm.startWorkout(w, k) }, modifier = Modifier.fillMaxWidth()) { Text("Započni trening") }
                    else OutlinedButton(onClick = { vm.startWorkout(w, k) }) { Text("Započni") }
                }
            }
        }
    }
}

// ===== Treninzi =====

@Composable
fun SessionsScreen(vm: AppViewModel, d: AppData, openId: Long?, onOpen: (Long?) -> Unit, onSettings: () -> Unit) {
    val s = openId?.let { id -> d.sessions.find { it.id == id } }
    if (s != null) { SessionDetail(vm, d, s) { onOpen(null) }; return }
    val list = d.sessions.sortedWith(compareByDescending<Session> { it.epochDay }.thenByDescending { it.id })
    ScreenColumn {
        TitleBar("Treninzi", onSettings)
        if (list.isEmpty()) Muted("Još nema odrađenih treninga. Pokrenite trening na Početnoj ili u Planu.")
        list.forEach { x ->
            androidx.compose.material3.Card(Modifier.fillMaxWidth().clickable { onOpen(x.id) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${formatShort(x.epochDay)} · ${x.workoutName}", fontWeight = FontWeight.SemiBold)
                        Muted(statLine(d.stats(x)))
                    }
                    Muted("›")
                }
            }
        }
    }
}

@Composable
private fun SessionDetail(vm: AppViewModel, d: AppData, s: Session, onBack: () -> Unit) {
    val c = d.catalog()
    val st = d.stats(s)
    var confirm by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    ScreenColumn {
        TextButton(onClick = onBack) { Text("‹ Treninzi") }
        Title("${formatShort(s.epochDay)} · ${s.workoutName}")
        Kpis(listOf("${st.minutes}" to "min", "${st.sets}" to "serija", formatInt(st.kg) to "kg", "≈${formatInt(st.kcal)}" to "kcal"))
        s.entries.forEach { e ->
            val ex = c[e.exerciseId]
            val vol = e.sets.sumOf { setVolume(ex, it) }
            BoxCard {
                Row {
                    Text(ex?.name ?: e.exerciseId, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (vol > 0) Muted("${formatInt(vol.roundToInt())} kg")
                }
                e.sets.forEachIndexed { k, x ->
                    Text("Serija ${k + 1}: ${describeSets(listOf(x), ex?.timed == true)}" + (x.workSeconds?.let { " · rad ${clock(it.toLong())}" } ?: ""))
                }
            }
        }
        TextButton(onClick = { confirm = true }) { Text("Obriši trening", color = MaterialTheme.colorScheme.error) }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Obrisati trening?") },
            text = { Text("Brisanje se ne može poništiti.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.deleteSession(s.id); onBack() }) { Text("Obriši") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Natrag") } },
        )
    }
}

// ===== Analiza =====

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(d: AppData, onOpenSession: (Long) -> Unit, onSettings: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(0) }
    ScreenColumn {
        TitleBar("Analiza", onSettings)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Tjedni", "Vježbe").forEachIndexed { i, l ->
                SegmentedButton(selected = mode == i, onClick = { mode = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(l) }
            }
        }
        if (d.sessions.isEmpty()) Muted("Analiza se puni kako odrađujete treninge.")
        else if (mode == 0) WeeksAnalysis(d, onOpenSession) else ExercisesAnalysis(d)
    }
}

@Composable
private fun WeeksAnalysis(d: AppData, onOpenSession: (Long) -> Unit) {
    val t = today()
    val total = d.sessions.map { d.stats(it) }.fold(Stats.ZERO, Stats::plus)
    val weekOfS = { s: Session -> d.weekOf(s.plannedDay ?: s.epochDay) }
    val sw = d.sessions.map(weekOfS)
    val maxW = maxOf(0, d.weekOf(t), sw.maxOrNull() ?: 0)
    val minW = minOf(0, sw.minOrNull() ?: 0)
    data class Wk(val w: Int, val ss: List<Session>, val sum: Stats, val planned: Int)
    val weeks = (minW..maxW).mapNotNull { w ->
        val ss = d.sessions.filter { weekOfS(it) == w }.sortedBy { it.epochDay }
        if (w < 0 && ss.isEmpty()) return@mapNotNull null
        // Planirani dani do danas; današnji se broji tek kad je odrađen.
        val planned = (0 until d.daysPerWeek).count { k -> d.inProgram(w, k) && (d.dayDate(w, k) < t || d.sessionFor(w, k) != null) }
        Wk(w, ss, ss.map { d.stats(it) }.fold(Stats.ZERO, Stats::plus), planned)
    }
    Kpis(listOf(
        "${d.sessions.size}" to "treninga", formatInt(total.kg) to "kg ukupno",
        "%.1f".format(java.util.Locale.forLanguageTag("hr"), total.minutes / 60.0) to "sati", "≈${formatInt(total.kcal)}" to "kcal",
    ))
    BoxCard {
        Text("Podignuti kilogrami po tjednu", style = MaterialTheme.typography.titleMedium)
        BarChart(weeks.takeLast(8).map { (if (it.w < 0) "–" else "T${it.w + 1}") to it.sum.kg })
    }
    weeks.asReversed().forEach { x ->
        BoxCard {
            Row {
                Text(if (x.w < 0) "Prije programa" else "Tjedan ${x.w + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Muted("${formatShort(d.start + 7L * x.w)} – ${formatShort(d.start + 7L * x.w + 6)}")
            }
            Text("${x.ss.size}${if (x.planned > 0) " od ${x.planned}" else ""} treninga · ${x.sum.sets} serija · ${formatInt(x.sum.kg)} kg · ${x.sum.minutes} min · ≈${formatInt(x.sum.kcal)} kcal")
            x.ss.forEach { s ->
                val st = d.stats(s)
                OutlinedButton(onClick = { onOpenSession(s.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${formatShort(s.epochDay)} · ${s.workoutName.substringAfter(" · ")}", modifier = Modifier.weight(1f))
                    Text("${formatInt(st.kg)} kg · ≈${st.kcal} kcal")
                }
            }
        }
    }
    Muted("Kilogrami = težina × ponavljanja; kod vježbi s dvije bučice (i jednoručnog veslanja, obje strane) računa se dvostruko. " +
        "Vježbe s vlastitom težinom i na vrijeme nisu uračunate. Kalorije su gruba procjena: 3,5 MET × ${kg(d.bodyWeightKg)} kg × trajanje " +
        "(2024 Adult Compendium of Physical Activities, šifra 02054).")
}

@Composable
private fun ExercisesAnalysis(d: AppData) {
    val c = d.catalog()
    val ids = d.sessions.flatMap { s -> s.entries.map { it.exerciseId } }.distinct()
    ids.forEach { id ->
        val ex = c[id] ?: return@forEach
        val h = d.history(id)
        if (h.isEmpty()) return@forEach
        val values = h.map { (_, sets) -> sets.mapNotNull { it.weightKg }.maxOrNull() ?: sets.maxOf { it.reps }.toDouble() }
        val unit = if (h.last().second.any { it.weightKg != null }) "kg" else if (ex.timed) "s" else "pon."
        val first = values.first()
        val now = values.last()
        val change = if (first > 0) ((now - first) / first * 100).roundToInt() else 0
        val vol = h.sumOf { (_, sets) -> sets.sumOf { setVolume(ex, it) } }
        BoxCard {
            Row {
                Text(ex.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Muted("${h.size}×" + if (vol > 0) " · ${formatInt(vol.roundToInt())} kg" else "")
            }
            Text("Početak ${kg(first)} $unit → sada ${kg(now)} $unit" + if (h.size > 1) " (${if (change >= 0) "+" else ""}$change %)" else "")
            if (values.size >= 2) LineChart(values)
            d.recommendation(id)?.let { Muted("Preporuka: ${it.text}") }
            h.takeLast(3).asReversed().forEach { (day, sets) -> Muted("${formatDate(day)}: ${describeSets(workingSets(sets), ex.timed)}") }
        }
    }
}

@Composable
private fun BarChart(rows: List<Pair<String, Int>>) {
    val color = MaterialTheme.colorScheme.primary
    val max = maxOf(1, rows.maxOfOrNull { it.second } ?: 1)
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val bw = size.width / rows.size
        rows.forEachIndexed { i, (_, v) ->
            val h = v.toFloat() / max * size.height
            drawRoundRect(color, Offset(i * bw + bw * 0.15f, size.height - h), Size(bw * 0.7f, h), CornerRadius(6f))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        rows.forEach { (l, v) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Muted(l)
                Muted(if (v >= 1000) "%.1f t".format(java.util.Locale.forLanguageTag("hr"), v / 1000.0) else "$v")
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val min = values.min()
        val span = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val pad = 8.dp.toPx()
        val pts = values.mapIndexed { i, v ->
            Offset(pad + i * (size.width - 2 * pad) / (values.size - 1), size.height - pad - ((v - min) / span * (size.height - 2 * pad)).toFloat())
        }
        drawPath(Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }, color, style = Stroke(3.dp.toPx()))
        pts.forEach { drawCircle(color, 4.dp.toPx(), it) }
    }
}

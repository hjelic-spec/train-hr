package hr.trainhr.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.trainhr.core.Advisor
import hr.trainhr.core.LoadKind
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.TIMED_MAX_SEC
import hr.trainhr.core.TIMED_MIN_SEC
import hr.trainhr.core.kg
import kotlinx.coroutines.delay

/** Kratka vibracija i zvučni signal na kraju odmora. */
private fun signal(context: Context) {
    runCatching {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 800)
    }
}

@Composable
fun WorkoutScreen(vm: AppViewModel, d: AppData, a: ActiveWorkout) {
    val context = LocalContext.current
    var confirmCancel by remember { mutableStateOf(false) }
    BackHandler {
        if (a.view != WorkoutView.EXERCISE) vm.showView(WorkoutView.EXERCISE) else vm.showView(WorkoutView.OVERVIEW)
    }
    // Zaslon ostaje upaljen tijekom treninga.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(250) } }
    val timer = a.timer
    LaunchedEffect(timer) {
        if (timer != null && timer.kind != TimerKind.WORK) {
            val left = timer.startedAt + timer.durationSec * 1000L - System.currentTimeMillis()
            if (left > 0) { delay(left); signal(context) }
        }
    }

    when (a.view) {
        WorkoutView.SUMMARY -> Summary(vm, d, a, onCancel = { confirmCancel = true })
        WorkoutView.OVERVIEW -> Overview(vm, a, d, now)
        WorkoutView.EXERCISE -> Focus(vm, d, a, now)
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Odbaciti trening?") },
            text = { Text("Upisane serije neće biti spremljene.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancelWorkout() }) { Text("Odbaci") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Natrag") } },
        )
    }
}

@Composable
private fun Head(left: String, a: ActiveWorkout, now: Long) {
    Row(Modifier.fillMaxWidth()) {
        Muted(left, Modifier.weight(1f))
        Muted("ukupno ${clock((now - a.startedAt) / 1000)}")
    }
}

@Composable
private fun Focus(vm: AppViewModel, d: AppData, a: ActiveWorkout, now: Long) {
    val c = d.catalog()
    val i = a.current
    val e = a.entries[i]
    val ex = c[e.exerciseId] ?: return
    val t = a.timer
    val r = d.recommendation(ex.id)
    val last = d.history(ex.id).lastOrNull()
    val working = t != null && t.kind == TimerKind.WORK && t.entryIndex == i
    val pending = a.pending?.takeIf { it.entryIndex == i }
    val done = a.done(i)
    val setNo = e.sets.size + 1
    val nextPos = a.nextUndone()
    val nextEx = nextPos?.let { c[a.entries[a.order[it]].exerciseId] }
    val x = extra()

    // Predupis: zadnja težina u ovom treningu ili preporuka; ponavljanja: izmjereno vrijeme (vježbe na vrijeme),
    // prošli rezultat iste serije, inače cilj.
    val defW = e.sets.lastOrNull()?.weightKg ?: r?.weightKg
    val lastSet = last?.second?.getOrNull(e.sets.size)
    val defR = when {
        ex.timed && pending != null -> pending.workSeconds
        lastSet != null -> lastSet.reps
        ex.timed -> TIMED_MIN_SEC
        else -> d.goal.repMax
    }
    val key = "$i-${e.sets.size}-${pending?.workSeconds}"
    var weight by remember(key) { mutableStateOf(defW?.let { kg(it) } ?: "") }
    var reps by remember(key) { mutableStateOf(defR.toString()) }
    val showW = ex.kind != LoadKind.BODYWEIGHT

    fun save() {
        val w = parseNumber(weight)
        val n = reps.toIntOrNull()
        if (n == null || n <= 0 || (showW && (w == null || w < 0))) {
            vm.say("Upišite " + (if (showW) "težinu i " else "") + if (ex.timed) "sekunde." else "ponavljanja."); return
        }
        vm.saveSet(i, if (showW) w else null, n)
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Head("Vježba ${a.pos + 1} od ${a.order.size}", a, now)
            ProgressBar(a)
            Text(ex.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            ExerciseArtView(ex.id)
            val range = if (ex.timed) "$TIMED_MIN_SEC–$TIMED_MAX_SEC s" else "${d.goal.repLabel} pon."
            Muted("$SETS_PER_EXERCISE × $range" + if (ex.id in d.pinned) " · stalna" else "")
            r?.let { Text(it.text) }
            last?.let { (day, sets) -> Muted("Prošli put (${formatDate(day)}): ${describeSets(sets, ex.timed)}") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until maxOf(SETS_PER_EXERCISE, e.sets.size)).forEach { k ->
                    val st = e.sets.getOrNull(k)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (st != null) x.workBg else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (st != null) x.work else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) { Text(st?.let { describeSets(listOf(it), ex.timed) } ?: "Serija ${k + 1}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 14.sp) }
                }
            }
            TimerBlock(a, now, i)
            if (!working) {
                if (pending != null) Text("Upišite rezultat serije $setNo (rad ${clock(pending.workSeconds.toLong())})", fontWeight = FontWeight.SemiBold)
                if (!done || pending != null) {
                    if (showW) Stepper("kg", weight, { weight = it }, ex.kind.stepKg, decimal = true)
                    Stepper(if (ex.timed) "sekunde" else "ponavljanja", reps, { reps = it }, if (ex.timed) 5.0 else 1.0, decimal = false, min = 1.0)
                }
            }
            val big = Modifier.fillMaxWidth().heightIn(min = 60.dp)
            when {
                working -> Button(onClick = vm::endSet, modifier = big) { Text("■ Kraj serije", fontSize = 20.sp) }
                pending != null -> Button(onClick = ::save, modifier = big) { Text("Spremi seriju $setNo", fontSize = 20.sp) }
                done -> {
                    Button(onClick = vm::goNext, modifier = big) { Text("Sljedeća vježba ›", fontSize = 20.sp) }
                    TextButton(onClick = { vm.startSet(i) }) { Text("+ Dodatna serija") }
                }
                else -> Button(onClick = { vm.startSet(i) }, modifier = big) { Text("▶ Počni seriju $setNo", fontSize = 20.sp) }
            }
            if (!working && pending == null && !done) TextButton(onClick = ::save) { Text("Upiši seriju bez štoperice") }
            if (e.sets.isNotEmpty() && !working) TextButton(onClick = { vm.undoSet(i) }) { Text("Poništi zadnju seriju") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (!done && !working && nextPos != null) TextButton(onClick = vm::later) { Text("Sprava zauzeta – kasnije") }
                TextButton(onClick = { vm.showView(WorkoutView.OVERVIEW) }) { Text("Pregled") }
                TextButton(onClick = { vm.showView(WorkoutView.SUMMARY) }) { Text("Završi") }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::goPrev, enabled = a.pos > 0) { Text("‹", fontSize = 28.sp) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (nextEx != null) {
                    Muted("Sljedeće")
                    Text(nextEx.name, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    d.recommendation(nextEx.id)?.weightKg?.let { Muted("${kg(it)} kg") }
                } else Muted(if (a.entries.indices.all { a.done(it) }) "Sve vježbe su odrađene" else "Zadnja vježba")
            }
            TextButton(onClick = vm::goNextPos, enabled = a.pos < a.order.size - 1) { Text("›", fontSize = 28.sp) }
        }
    }
}

@Composable
private fun ProgressBar(a: ActiveWorkout) {
    val x = extra()
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val n = a.order.size
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        a.order.forEachIndexed { p, idx ->
            val col = if (a.done(idx)) x.work else if (p == a.pos) primary else idle
            drawRoundRect(col, Offset(p * (w + gap), 0f), Size(w, size.height), CornerRadius(size.height / 2))
        }
    }
}

@Composable
private fun TimerBlock(a: ActiveWorkout, now: Long, current: Int) {
    val t = a.timer
    val x = extra()
    val (label, value, bg, fg, over) = when {
        t == null -> Quint("Spremni", "0:00", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, false)
        t.kind == TimerKind.WORK -> Quint("Rad · serija ${a.entries[t.entryIndex].sets.size + 1}", clock((now - t.startedAt) / 1000), x.workBg, x.work, false)
        else -> {
            val left = Math.floorDiv(t.startedAt + t.durationSec * 1000L - now + 999, 1000L)
            val l = if (t.kind == TimerKind.REST_SET) "Odmor između serija" else if (t.entryIndex != current) "Odmor · pripremite se" else "Odmor između vježbi"
            Quint(l, (if (left < 0) "+" else "") + clock(left), x.restBg, x.rest, left < 0)
        }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), color = fg, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(value, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (t != null && t.kind != TimerKind.WORK) {
                Muted(if (over) "Odmor je istekao – krenite kad ste spremni." else "Odbrojava se do sljedeće serije.")
            }
        }
    }
}

private data class Quint<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

@Composable
private fun Overview(vm: AppViewModel, a: ActiveWorkout, d: AppData, now: Long) {
    val c = d.catalog()
    ScreenColumn {
        Head(a.name, a, now)
        Title("Pregled treninga")
        a.order.forEachIndexed { p, idx ->
            val e = a.entries[idx]
            Card(Modifier.fillMaxWidth().clickable { vm.jump(p) }, border = if (p == a.pos) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                Row(Modifier.padding(12.dp)) {
                    Text("${p + 1}. ${c[e.exerciseId]?.name ?: e.exerciseId}", modifier = Modifier.weight(1f))
                    Text((if (a.done(idx)) "✓ " else "") + "${e.sets.size}/$SETS_PER_EXERCISE", color = if (a.done(idx)) extra().work else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(onClick = { vm.showView(WorkoutView.EXERCISE) }, modifier = Modifier.fillMaxWidth()) { Text("Natrag na vježbu") }
        Button(onClick = { vm.showView(WorkoutView.SUMMARY) }, modifier = Modifier.fillMaxWidth()) { Text("Završi trening") }
    }
}

@Composable
private fun Summary(vm: AppViewModel, d: AppData, a: ActiveWorkout, onCancel: () -> Unit) {
    val c = d.catalog()
    val done = a.entries.filter { it.sets.isNotEmpty() }
    val all = a.entries.indices.all { a.done(it) }
    ScreenColumn {
        Title(if (all) "Trening gotov" else "Završiti trening?")
        Muted("${a.name} · ${(System.currentTimeMillis() - a.startedAt) / 60000} min · ${done.sumOf { it.sets.size }} serija")
        if (!all) BoxCard(warn = true) { Text("Nisu odrađene sve serije. Spremaju se samo upisane.") }
        done.forEach { e ->
            val ex = c[e.exerciseId] ?: return@forEach
            val next = Advisor.recommend(ex, d.goal, d.history(ex.id).map { it.second } + listOf(e.sets))
            BoxCard {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                Text(describeSets(e.sets, ex.timed))
                Muted("Sljedeći put: ${next.text}")
            }
        }
        Button(onClick = vm::saveWorkout, modifier = Modifier.fillMaxWidth()) { Text("Spremi trening") }
        OutlinedButton(onClick = { vm.showView(WorkoutView.EXERCISE) }, modifier = Modifier.fillMaxWidth()) { Text("Natrag na trening") }
        TextButton(onClick = onCancel) { Text("Odbaci trening bez spremanja", color = MaterialTheme.colorScheme.error) }
    }
}

package hr.trainhr.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hr.trainhr.core.Exercise
import hr.trainhr.core.Goal
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.SetEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Izbor s ekrana postavki plana. */
data class SetupChoice(
    val goal: Goal,
    val days: Int,
    val minutes: Int,
    val restSets: Int,
    val restExercises: Int,
    val startDay: Long,
    val endDay: Long?,
    val bodyWeightKg: Double,
    val selected: List<String>,
    val pinned: List<String>,
    val custom: List<Exercise>,
)

enum class TimerKind { WORK, REST_SET, REST_EXERCISE }

/** Štoperica: rad mjeri vrijeme od početka; odmor odbrojava [durationSec]. */
data class TimerState(val kind: TimerKind, val entryIndex: Int, val startedAt: Long, val durationSec: Int = 0)

/** Završena serija čije trajanje je izmjereno, a rezultat još nije upisan. */
data class PendingSet(val entryIndex: Int, val workSeconds: Int)

enum class WorkoutView { EXERCISE, OVERVIEW, SUMMARY }

/** Trening u tijeku (samo u memoriji dok se ne spremi). */
data class ActiveWorkout(
    val name: String,
    val plannedDay: Long,
    val startedAt: Long,
    val entries: List<Entry>,
    /** Redoslijed izvođenja (indeksi u [entries]); „kasnije” premješta vježbu na kraj. */
    val order: List<Int>,
    val pos: Int = 0,
    val view: WorkoutView = WorkoutView.EXERCISE,
    val timer: TimerState? = null,
    val pending: PendingSet? = null,
) {
    val current: Int get() = order[pos]
    fun done(i: Int) = entries[i].sets.size >= SETS_PER_EXERCISE

    /** Sljedeća neodrađena vježba nakon pozicije [from] (kružno); null ako su sve odrađene. */
    fun nextUndone(from: Int = pos): Int? {
        val n = order.size
        for (k in 1 until n) { val p = (from + k) % n; if (!done(order[p])) return p }
        return null
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app.filesDir)
    val data: StateFlow<AppData> = repo.data

    private val _active = MutableStateFlow<ActiveWorkout?>(null)
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }
    fun say(text: String) { _message.value = text }

    private fun save(transform: (AppData) -> AppData) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun saveSetup(s: SetupChoice) = save { d ->
        d.copy(
            goal = s.goal, daysPerWeek = s.days, durationMinutes = s.minutes,
            // Spremamo samo odstupanje od preporuke, da promjena cilja vrati preporučene odmore.
            restSetsSec = s.restSets.takeIf { it != s.goal.restBetweenSetsSec },
            restExercisesSec = s.restExercises.takeIf { it != s.goal.restBetweenExercisesSec },
            startDay = s.startDay, endDay = s.endDay, bodyWeightKg = s.bodyWeightKg,
            selected = s.selected, pinned = s.pinned.filter { it in s.selected }, customExercises = s.custom,
        )
    }

    fun reshuffle(week: Int) {
        save { d -> d.copy(salts = d.salts + (week to (d.salts[week] ?: 0) + 1)) }
        say("Treninzi su promiješani.")
    }

    fun deleteSession(id: Long) = save { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }

    // --- Trening ---

    fun startWorkout(week: Int, k: Int) {
        val d = data.value
        val c = d.catalog()
        val ids = d.planForWeek(week).days.getOrNull(k).orEmpty().filter { c[it] != null }
        if (ids.isEmpty()) { say("Ovaj trening nema vježbi – odaberite vježbe u postavkama."); return }
        _active.value = ActiveWorkout(
            name = "Tjedan ${week + 1} · Trening ${'A' + k}", plannedDay = d.dayDate(week, k),
            startedAt = System.currentTimeMillis(), entries = ids.map { Entry(it, emptyList()) }, order = ids.indices.toList(),
        )
    }

    private fun update(f: (ActiveWorkout) -> ActiveWorkout) { _active.value = _active.value?.let(f) }

    private fun rest(a: ActiveWorkout, entry: Int, setsAfter: Int, now: Long): TimerState {
        val d = data.value
        return if (setsAfter >= SETS_PER_EXERCISE) TimerState(TimerKind.REST_EXERCISE, entry, now, d.restExercises)
        else TimerState(TimerKind.REST_SET, entry, now, d.restSets)
    }

    fun startSet(i: Int) = update { a ->
        // Neupisana serija druge vježbe: vrati korisnika na nju umjesto da se izmjereno vrijeme izgubi.
        val p = a.pending
        if (p != null && p.entryIndex != i) {
            say("Prvo upišite rezultat prethodne serije.")
            a.copy(pos = a.order.indexOf(p.entryIndex))
        } else {
            a.copy(timer = TimerState(TimerKind.WORK, i, System.currentTimeMillis()), pending = null)
        }
    }

    /** Kraj serije: zaustavlja rad i odmah pokreće odmor (između serija ili između vježbi). */
    fun endSet() = update { a ->
        val t = a.timer ?: return@update a
        if (t.kind != TimerKind.WORK) return@update a
        val now = System.currentTimeMillis()
        a.copy(
            pending = PendingSet(t.entryIndex, ((now - t.startedAt) / 1000).toInt()),
            timer = rest(a, t.entryIndex, a.entries[t.entryIndex].sets.size + 1, now),
        )
    }

    fun saveSet(i: Int, weightKg: Double?, reps: Int) = update { a ->
        val work = a.pending?.takeIf { it.entryIndex == i }?.workSeconds
        val entries = a.entries.mapIndexed { j, e -> if (j == i) e.copy(sets = e.sets + SetEntry(weightKg, reps, work)) else e }
        var next = a.copy(entries = entries, pending = if (a.pending?.entryIndex == i) null else a.pending)
        if (work == null) next = next.copy(timer = rest(next, i, entries[i].sets.size, System.currentTimeMillis()))
        // Nakon zadnje serije: sam prelazi na sljedeću vježbu (odmor između vježbi teče dalje).
        if (entries[i].sets.size == SETS_PER_EXERCISE) {
            val np = next.nextUndone()
            next = if (np == null) next.copy(view = WorkoutView.SUMMARY) else next.copy(pos = np)
        }
        next
    }

    fun undoSet(i: Int) = update { a ->
        a.copy(
            entries = a.entries.mapIndexed { j, e -> if (j == i) e.copy(sets = e.sets.dropLast(1)) else e },
            pending = if (a.pending?.entryIndex == i) null else a.pending,
        )
    }

    fun goNext() = update { a -> a.nextUndone()?.let { a.copy(pos = it) } ?: a.copy(view = WorkoutView.SUMMARY) }
    fun goPrev() = update { a -> a.copy(pos = maxOf(0, a.pos - 1)) }
    fun goNextPos() = update { a -> a.copy(pos = minOf(a.order.size - 1, a.pos + 1)) }

    /** „Sprava zauzeta”: trenutnu vježbu premjesti na kraj redoslijeda. */
    fun later() {
        update { a ->
            val idx = a.order[a.pos]
            val order = a.order.toMutableList().apply { removeAt(a.pos); add(idx) }
            var moved = a.copy(order = order, pos = minOf(a.pos, order.size - 1))
            if (moved.current == idx || moved.done(moved.current)) moved.nextUndone()?.let { moved = moved.copy(pos = it) }
            moved
        }
        say("Vježba premještena na kraj.")
    }

    fun showView(v: WorkoutView) = update { it.copy(view = v) }
    fun jump(p: Int) = update { it.copy(pos = p, view = WorkoutView.EXERCISE) }
    fun skipRest() = update { a -> if (a.timer?.kind == TimerKind.WORK) a else a.copy(timer = null) }

    fun saveWorkout() {
        val a = _active.value ?: return
        _active.value = null
        val entries = a.entries.filter { it.sets.isNotEmpty() }
        if (entries.isEmpty()) { say("Ništa nije upisano – trening nije spremljen."); return }
        val now = System.currentTimeMillis()
        save { d ->
            d.copy(sessions = d.sessions + Session(now, today(), a.name, entries, ((now - a.startedAt) / 1000).toInt(), a.plannedDay))
        }
        say("Trening spremljen.")
    }

    fun cancelWorkout() { _active.value = null }

    // --- Sigurnosna kopija ---

    fun export(uri: Uri) {
        viewModelScope.launch {
            val out = getApplication<Application>().contentResolver.openOutputStream(uri)
            _message.value = if (out == null) "Izvoz nije uspio." else { repo.export(out); "Podaci izvezeni." }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val input = getApplication<Application>().contentResolver.openInputStream(uri)
            _message.value = if (input == null) "Uvoz nije uspio." else repo.import(input).fold(
                onSuccess = { "Podaci uvezeni." },
                onFailure = { "Datoteka nije valjana – postojeći podaci nisu mijenjani." },
            )
        }
    }
}

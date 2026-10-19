package hr.trainhr.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hr.trainhr.core.Advisor
import hr.trainhr.core.Catalog
import hr.trainhr.core.Exercise
import hr.trainhr.core.Goal
import hr.trainhr.core.PlanBuilder
import hr.trainhr.core.Recommendation
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.SetEntry
import hr.trainhr.core.Timing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Izbor s ekrana postavki plana. */
data class SetupChoice(
    val goal: Goal,
    val days: Int,
    val minutes: Int,
    val restSets: Int,
    val restExercises: Int,
    val selected: List<String>,
    val custom: List<Exercise>,
)

enum class TimerKind { WORK, REST_SET, REST_EXERCISE }

/** Štoperica: rad mjeri vrijeme od početka; odmor odbrojava [durationSec]. */
data class TimerState(val kind: TimerKind, val entryIndex: Int, val startedAt: Long, val durationSec: Int = 0)

/** Završena serija čije trajanje je izmjereno, a ponavljanja još nisu upisana. */
data class PendingSet(val entryIndex: Int, val workSeconds: Int)

/** Trening u tijeku (samo u memoriji dok se ne završi). */
data class ActiveWorkout(
    val name: String,
    val entries: List<Entry>,
    val startedAt: Long,
    val timer: TimerState? = null,
    val pending: PendingSet? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app.filesDir)
    val data: StateFlow<AppData> = repo.data

    private val _active = MutableStateFlow<ActiveWorkout?>(null)
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun catalog(d: AppData = data.value) = Catalog(d.customExercises)

    fun consumeMessage() {
        _message.value = null
    }

    private fun save(transform: (AppData) -> AppData) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun saveSetup(s: SetupChoice) = save { d ->
        val catalog = Catalog(s.custom)
        val capacity = Timing.capacity(s.minutes, s.goal, s.restSets, s.restExercises)
        val result = PlanBuilder.build(s.days, s.selected.mapNotNull { catalog[it] }, capacity)
        d.copy(
            goal = s.goal, daysPerWeek = s.days, durationMinutes = s.minutes,
            // Spremamo samo odstupanje od preporuke, da promjena cilja vrati preporučene odmore.
            restSetsSec = s.restSets.takeIf { it != s.goal.restBetweenSetsSec },
            restExercisesSec = s.restExercises.takeIf { it != s.goal.restBetweenExercisesSec },
            selected = s.selected,
            customExercises = s.custom, plan = result.workouts, omitted = result.omitted.map { it.id },
        )
    }

    /** Povijest serija vježbe po treninzima, kronološki. */
    fun history(d: AppData, exerciseId: String): List<Pair<Long, List<SetEntry>>> =
        d.sessions.mapNotNull { s ->
            s.entries.firstOrNull { it.exerciseId == exerciseId }?.takeIf { it.sets.isNotEmpty() }?.let { s.epochDay to it.sets }
        }

    fun recommendation(d: AppData, exerciseId: String): Recommendation? {
        val ex = catalog(d)[exerciseId] ?: return null
        return Advisor.recommend(ex, d.goal, history(d, exerciseId).map { it.second })
    }

    fun nextWorkoutIndex(d: AppData): Int {
        val last = d.sessions.lastOrNull()?.workoutName
        val i = d.plan.indexOfFirst { it.name == last }
        return if (i < 0 || d.plan.isEmpty()) 0 else (i + 1) % d.plan.size
    }

    // --- Trening i štoperica ---

    fun startWorkout(index: Int) {
        val w = data.value.plan.getOrNull(index) ?: return
        _active.value = ActiveWorkout(w.name, w.exerciseIds.map { Entry(it, emptyList()) }, System.currentTimeMillis())
    }

    private fun update(f: (ActiveWorkout) -> ActiveWorkout) {
        _active.value = _active.value?.let(f)
    }

    fun startSet(entryIndex: Int) = update {
        it.copy(timer = TimerState(TimerKind.WORK, entryIndex, System.currentTimeMillis()), pending = null)
    }

    /** Kraj serije: zaustavlja rad i odmah pokreće odmor (između serija ili između vježbi). */
    fun endSet() = update { a ->
        val t = a.timer ?: return@update a
        if (t.kind != TimerKind.WORK) return@update a
        val now = System.currentTimeMillis()
        val d = data.value
        val setsAfter = a.entries[t.entryIndex].sets.size + 1
        val rest = if (setsAfter >= SETS_PER_EXERCISE) {
            TimerState(TimerKind.REST_EXERCISE, t.entryIndex, now, d.restExercises)
        } else {
            TimerState(TimerKind.REST_SET, t.entryIndex, now, d.restSets)
        }
        a.copy(timer = rest, pending = PendingSet(t.entryIndex, ((now - t.startedAt) / 1000).toInt()))
    }

    fun skipRest() = update { a -> if (a.timer?.kind == TimerKind.WORK) a else a.copy(timer = null) }

    fun addSet(entryIndex: Int, weightKg: Double?, reps: Int) = update { a ->
        val work = a.pending?.takeIf { it.entryIndex == entryIndex }?.workSeconds
        val set = SetEntry(weightKg, reps, work)
        a.copy(
            entries = a.entries.mapIndexed { j, e -> if (j == entryIndex) e.copy(sets = e.sets + set) else e },
            pending = if (work != null) null else a.pending,
        )
    }

    fun removeLastSet(entryIndex: Int) = update { a ->
        a.copy(entries = a.entries.mapIndexed { j, e -> if (j == entryIndex) e.copy(sets = e.sets.dropLast(1)) else e })
    }

    fun cancelWorkout() {
        _active.value = null
    }

    fun finishWorkout() {
        val a = _active.value ?: return
        _active.value = null
        val entries = a.entries.filter { it.sets.isNotEmpty() }
        if (entries.isEmpty()) return
        val duration = ((System.currentTimeMillis() - a.startedAt) / 1000).toInt()
        save { d ->
            d.copy(sessions = d.sessions + Session(System.currentTimeMillis(), LocalDate.now().toEpochDay(), a.name, entries, duration))
        }
        _message.value = "Trening spremljen."
    }

    fun deleteSession(id: Long) = save { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }

    // --- Sigurnosna kopija ---

    fun export(uri: Uri) {
        viewModelScope.launch {
            val out = getApplication<Application>().contentResolver.openOutputStream(uri)
            _message.value = if (out == null) "Izvoz nije uspio." else {
                repo.export(out)
                "Podaci izvezeni."
            }
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

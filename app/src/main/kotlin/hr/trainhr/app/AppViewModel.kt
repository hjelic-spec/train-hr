package hr.trainhr.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hr.trainhr.core.Advisor
import hr.trainhr.core.Catalog
import hr.trainhr.core.Exercise
import hr.trainhr.core.PlanBuilder
import hr.trainhr.core.Recommendation
import hr.trainhr.core.RepRange
import hr.trainhr.core.SetEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Trening u tijeku (samo u memoriji dok se ne završi). */
data class ActiveWorkout(val name: String, val entries: List<Entry>)

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

    fun saveSetup(days: Int, range: RepRange, selected: List<String>, custom: List<Exercise>) = save { d ->
        val catalog = Catalog(custom)
        val plan = PlanBuilder.build(days, selected.mapNotNull { catalog[it] })
        d.copy(daysPerWeek = days, repRange = range, selected = selected, customExercises = custom, plan = plan)
    }

    /** Povijest serija vježbe po treninzima, kronološki. */
    fun history(d: AppData, exerciseId: String): List<Pair<Long, List<SetEntry>>> =
        d.sessions.mapNotNull { s ->
            s.entries.firstOrNull { it.exerciseId == exerciseId }?.takeIf { it.sets.isNotEmpty() }?.let { s.epochDay to it.sets }
        }

    fun recommendation(d: AppData, exerciseId: String): Recommendation? {
        val ex = catalog(d)[exerciseId] ?: return null
        return Advisor.recommend(ex, d.repRange, history(d, exerciseId).map { it.second })
    }

    fun nextWorkoutIndex(d: AppData): Int {
        val last = d.sessions.lastOrNull()?.workoutName
        val i = d.plan.indexOfFirst { it.name == last }
        return if (i < 0 || d.plan.isEmpty()) 0 else (i + 1) % d.plan.size
    }

    fun startWorkout(index: Int) {
        val w = data.value.plan.getOrNull(index) ?: return
        _active.value = ActiveWorkout(w.name, w.exerciseIds.map { Entry(it, emptyList()) })
    }

    fun addSet(entryIndex: Int, set: SetEntry) = editEntry(entryIndex) { it + set }

    fun removeLastSet(entryIndex: Int) = editEntry(entryIndex) { it.dropLast(1) }

    private fun editEntry(i: Int, f: (List<SetEntry>) -> List<SetEntry>) {
        val a = _active.value ?: return
        _active.value = a.copy(entries = a.entries.mapIndexed { j, e -> if (j == i) e.copy(sets = f(e.sets)) else e })
    }

    fun cancelWorkout() {
        _active.value = null
    }

    fun finishWorkout() {
        val a = _active.value ?: return
        _active.value = null
        val entries = a.entries.filter { it.sets.isNotEmpty() }
        if (entries.isEmpty()) return
        save { d ->
            d.copy(sessions = d.sessions + Session(System.currentTimeMillis(), LocalDate.now().toEpochDay(), a.name, entries))
        }
        _message.value = "Trening spremljen."
    }

    fun deleteSession(id: Long) = save { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }

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

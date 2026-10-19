package hr.trainhr.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hr.trainhr.app.data.AppData
import hr.trainhr.app.data.CompletedTest
import hr.trainhr.app.data.InitialTestState
import hr.trainhr.app.data.Repository
import hr.trainhr.app.data.SessionEntry
import hr.trainhr.app.data.SessionRecord
import hr.trainhr.core.model.EquipmentSettings
import hr.trainhr.core.model.Exercise
import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.LoadType
import hr.trainhr.core.model.PlanMode
import hr.trainhr.core.model.PlanSettings
import hr.trainhr.core.model.Prescription
import hr.trainhr.core.model.SetLog
import hr.trainhr.core.model.TrainingPlan
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.model.WorkoutTemplate
import hr.trainhr.core.plan.PlanGenerator
import hr.trainhr.core.progression.ExerciseState
import hr.trainhr.core.progression.Progression
import hr.trainhr.core.test.FunctionalTest
import hr.trainhr.core.test.InitialTest
import hr.trainhr.core.test.TestResult
import hr.trainhr.core.util.Loads
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ActiveEntry(
    val prescription: Prescription,
    val targetSets: Int,
    /** null = bez opterećenja ili kalibracija (korisnik sam bira težinu). */
    val suggestedWeightKg: Double?,
    val calibration: Boolean,
    val logged: List<SetLog> = emptyList(),
)

data class ActiveWorkout(
    val templateIndex: Int,
    val name: String,
    val warmup: String,
    val deload: Boolean,
    val entries: List<ActiveEntry>,
    val restEndsAtMillis: Long? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app.filesDir)
    val data: StateFlow<AppData> = repo.data

    private val _active = MutableStateFlow<ActiveWorkout?>(null)
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

    /** Poruka za kratki prikaz (snackbar). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun catalog(d: AppData = data.value) = ExerciseCatalog(d.customExercises)

    fun loads(d: AppData = data.value) = Loads(d.profile?.equipment ?: EquipmentSettings())

    fun consumeMessage() {
        _message.value = null
    }

    private fun save(transform: (AppData) -> AppData) {
        viewModelScope.launch { repo.update(transform) }
    }

    // --- Uvod ---

    fun saveProfile(p: UserProfile) = save { d ->
        d.copy(profile = p.copy(equipment = d.profile?.equipment ?: p.equipment))
    }

    fun saveReadiness(answers: List<Boolean>, acknowledged: Boolean) = save { d ->
        d.copy(
            profile = d.profile?.copy(readinessAnswers = answers, readinessAcknowledged = acknowledged),
            readinessDone = true,
        )
    }

    fun saveEquipment(eq: EquipmentSettings) = save { d ->
        d.copy(profile = d.profile?.copy(equipment = eq), equipmentDone = true)
    }

    // --- Početni test ---

    fun saveTestResult(r: TestResult) = save { d ->
        d.copy(test = d.test.copy(results = d.test.results + (r.exerciseId to r), skipped = d.test.skipped - r.exerciseId))
    }

    fun skipTestExercise(id: String) = save { d ->
        d.copy(test = d.test.copy(results = d.test.results - id, skipped = d.test.skipped + id))
    }

    fun resetTestExercise(id: String) = save { d ->
        d.copy(test = d.test.copy(results = d.test.results - id, skipped = d.test.skipped - id))
    }

    fun saveFunctional(f: FunctionalTest) = save { d -> d.copy(test = d.test.copy(functional = f)) }

    fun testComplete(d: AppData): Boolean {
        val profile = d.profile ?: return false
        val ids = InitialTest.exercisesFor(profile, catalog(d)).map { it.second }
        return d.test.functional != null && ids.all { it in d.test.results || it in d.test.skipped }
    }

    /**
     * Završava test: rezultati idu u povijest. Kod ponovnog testa radna težina se postavlja
     * iz testa samo za vježbe koje je još nemaju – postojeća progresija se ne poništava.
     */
    fun finishTest() = save { d ->
        val done = CompletedTest(today(), d.test.results, d.test.skipped, d.test.functional)
        val withHistory = d.copy(testHistory = d.testHistory + done, test = InitialTestState())
        withHistory.copy(exerciseStates = statesFromTest(withHistory, d.test.results))
    }

    fun startRetest() = save { d -> d.copy(test = InitialTestState()) }

    /** Postoji li završen test (uvjet za plan). */
    fun hasCompletedTest(d: AppData) = d.testHistory.isNotEmpty()

    private fun statesFromTest(d: AppData, results: Map<String, TestResult>): Map<String, ExerciseState> {
        val profile = d.profile ?: return d.exerciseStates
        val plan = d.plan ?: return d.exerciseStates
        val catalog = catalog(d)
        val loads = loads(d)
        val updated = d.exerciseStates.toMutableMap()
        plan.workouts.flatMap { it.items }.distinctBy { it.exerciseId }.forEach { p ->
            if (updated[p.exerciseId]?.workingWeightKg != null) return@forEach
            val r = results[p.exerciseId] ?: return@forEach
            val ex = catalog[p.exerciseId] ?: return@forEach
            val w = Progression.initialWeight(r.estimatedOneRmKg, p, ex, profile.experience, loads)
            updated[p.exerciseId] = ExerciseState(p.exerciseId, w)
        }
        return updated
    }

    fun latestTest(d: AppData): CompletedTest? = d.testHistory.lastOrNull()

    // --- Plan ---

    fun createPlan(settings: PlanSettings) = save { d ->
        val profile = d.profile ?: return@save d
        val plan = if (settings.mode == PlanMode.MANUAL) {
            TrainingPlan(settings, List(settings.daysPerWeek.coerceIn(2, 4)) { i ->
                WorkoutTemplate("Trening ${'A' + i}", PlanGenerator.generate(profile, settings, catalog(d)).workouts.first().warmup, emptyList())
            })
        } else {
            PlanGenerator.generate(profile, settings, catalog(d))
        }
        d.copy(plan = plan, planStartEpochDay = d.planStartEpochDay ?: today())
    }

    fun updatePlan(plan: TrainingPlan) = save { d -> d.copy(plan = plan) }

    fun addCustomExercise(ex: Exercise) = save { d -> d.copy(customExercises = d.customExercises + ex.copy(custom = true)) }

    fun weekIndex(d: AppData): Int = d.planStartEpochDay?.let { ((today() - it) / 7).toInt() } ?: 0

    fun isDeloadWeek(d: AppData): Boolean =
        d.plan?.let { Progression.isDeloadWeek(weekIndex(d), it.settings.deloadEveryWeeks) } ?: false

    /** Prijedlog težine: zadnje stanje progresije → procjena iz testa → kalibracija (null). */
    fun suggestedWeight(d: AppData, p: Prescription): Double? {
        d.exerciseStates[p.exerciseId]?.workingWeightKg?.let { return it }
        val profile = d.profile ?: return null
        val ex = catalog(d)[p.exerciseId] ?: return null
        val r = latestTest(d)?.results?.get(p.exerciseId) ?: return null
        return Progression.initialWeight(r.estimatedOneRmKg, p, ex, profile.experience, loads(d))
    }

    // --- Trening ---

    fun startWorkout(index: Int) {
        val d = data.value
        val template = d.plan?.workouts?.getOrNull(index) ?: return
        val deload = isDeloadWeek(d)
        val catalog = catalog(d)
        val entries = template.items.map { p ->
            val ex = catalog[p.exerciseId]
            val extra = d.exerciseStates[p.exerciseId]?.extraSets ?: 0
            val base = suggestedWeight(d, p)
            if (deload && ex != null) {
                val (sets, w) = Progression.deload(p, base, ex, loads(d))
                ActiveEntry(p, sets, w, calibration = base == null && ex.loadType != LoadType.NONE)
            } else {
                ActiveEntry(p, p.sets + extra, base, calibration = base == null && ex?.loadType != LoadType.NONE)
            }
        }
        _active.value = ActiveWorkout(index, template.name, template.warmup, deload, entries)
    }

    fun logSet(entryIndex: Int, set: SetLog) {
        val a = _active.value ?: return
        val entries = a.entries.toMutableList()
        val e = entries[entryIndex]
        entries[entryIndex] = e.copy(logged = e.logged + set)
        _active.value = a.copy(
            entries = entries,
            restEndsAtMillis = System.currentTimeMillis() + e.prescription.restSeconds * 1000L,
        )
    }

    fun removeLastSet(entryIndex: Int) {
        val a = _active.value ?: return
        val entries = a.entries.toMutableList()
        val e = entries[entryIndex]
        entries[entryIndex] = e.copy(logged = e.logged.dropLast(1))
        _active.value = a.copy(entries = entries)
    }

    fun skipRest() {
        _active.value = _active.value?.copy(restEndsAtMillis = null)
    }

    fun cancelWorkout() {
        _active.value = null
    }

    fun finishWorkout() {
        val a = _active.value ?: return
        _active.value = null
        save { d ->
            val profile = d.profile ?: return@save d
            val catalog = catalog(d)
            val loads = loads(d)
            val states = d.exerciseStates.toMutableMap()
            val entries = a.entries.filter { it.logged.isNotEmpty() }.map { e ->
                val ex = catalog[e.prescription.exerciseId]
                val note = if (ex == null || a.deload || !e.prescription.autoProgression) {
                    if (a.deload) "Lakši tjedan – bez promjene težine." else ""
                } else {
                    val current = states[ex.id] ?: ExerciseState(ex.id, e.suggestedWeightKg)
                    val decision = Progression.next(e.prescription, ex, current, e.logged, loads, profile.experience)
                    states[ex.id] = decision.state
                    decision.reason
                }
                SessionEntry(e.prescription.exerciseId, e.logged, note)
            }
            if (entries.isEmpty()) return@save d
            val record = SessionRecord(System.currentTimeMillis(), today(), a.name, a.deload, entries)
            _message.value = "Trening spremljen."
            d.copy(sessions = d.sessions + record, exerciseStates = states)
        }
    }

    // --- Sigurnosna kopija ---

    fun export(uri: Uri) {
        viewModelScope.launch {
            val out = getApplication<Application>().contentResolver.openOutputStream(uri)
            if (out == null) {
                _message.value = "Izvoz nije uspio."
                return@launch
            }
            repo.export(out)
            repo.update { it.copy(lastExportEpochDay = today()) }
            _message.value = "Podaci izvezeni."
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val input = getApplication<Application>().contentResolver.openInputStream(uri)
            if (input == null) {
                _message.value = "Uvoz nije uspio."
                return@launch
            }
            _message.value = repo.import(input).fold(
                onSuccess = { "Podaci uvezeni." },
                onFailure = { "Datoteka nije valjana sigurnosna kopija – postojeći podaci nisu mijenjani." },
            )
        }
    }

    companion object {
        fun today(): Long = LocalDate.now().toEpochDay()
    }
}

package hr.trainhr.core.test

import hr.trainhr.core.model.BodyRegion
import hr.trainhr.core.model.Equipment
import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.util.Loads
import hr.trainhr.core.util.OneRepMax
import hr.trainhr.core.util.formatKg
import kotlinx.serialization.Serializable

/**
 * Obavezni početni test: procjena 1RM iz submaksimalne serije (4–10 ponavljanja),
 * raspoređen u dva dana radi umora.
 */
object InitialTest {

    const val MIN_REPS = 4
    const val MAX_REPS = 10
    const val MAX_ATTEMPTS = 3
    /** Bol od ove razine naviše prekida test vježbe. */
    const val PAIN_STOP = 3

    data class Slot(val day: Int, val primaryId: String, val fallbackIds: List<String>)

    private val protocol = listOf(
        Slot(1, "trap_bar_deadlift", listOf("rdl_dumbbell")),
        Slot(1, "bench_press", listOf("db_bench_press")),
        Slot(1, "lat_pulldown", emptyList()),
        Slot(1, "db_curl", emptyList()),
        Slot(2, "back_squat", listOf("goblet_squat")),
        Slot(2, "cable_row", listOf("db_row")),
        Slot(2, "db_shoulder_press", emptyList()),
    )

    val allTestExerciseIds: Set<String> =
        protocol.flatMap { listOf(it.primaryId) + it.fallbackIds }.toSet()

    /**
     * Testne vježbe za korisnika: primarna vježba ako je oprema dostupna; inače zamjena.
     * Trap bar mrtvo dizanje i pregib bučicama su uvijek uključeni kad je oprema dostupna.
     * Ograničenja iz profila ne isključuju vježbu – korisnik vidi upozorenje i može je preskočiti.
     */
    fun exercisesFor(profile: UserProfile, catalog: ExerciseCatalog): List<Pair<Int, String>> {
        val eq = profile.equipment.available
        return protocol.mapNotNull { slot ->
            val id = (listOf(slot.primaryId) + slot.fallbackIds).firstOrNull { id ->
                catalog[id]?.equipment?.let { eq.containsAll(it) } == true
            }
            id?.let { slot.day to it }
        }
    }

    fun warnings(exerciseId: String, profile: UserProfile, catalog: ExerciseCatalog): List<BodyRegion> =
        catalog[exerciseId]?.stresses.orEmpty().intersect(profile.limitations).toList()

    fun evaluate(
        exerciseId: String,
        attempts: List<TestAttempt>,
        loads: Loads,
        catalog: ExerciseCatalog,
    ): AttemptEvaluation {
        require(attempts.isNotEmpty())
        val type = catalog[exerciseId]?.loadType ?: error("Nepoznata vježba: $exerciseId")
        val last = attempts.last()
        val toFailure = last.reps + last.rir
        val estimate = if (last.reps > 0) OneRepMax.estimate(last.weightKg, last.reps, last.rir) else null

        if (last.pain >= PAIN_STOP) {
            return AttemptEvaluation(Verdict.STOP_PAIN, bestResult(exerciseId, attempts), null,
                "Bol ${last.pain}/10 – test ove vježbe se prekida. Izaberite zamjensku vježbu ili se posavjetujte sa stručnjakom.")
        }
        if (last.reps == 0 || estimate == null) {
            val lighter = loads.previous(type, last.weightKg)
            return AttemptEvaluation(Verdict.TOO_HEAVY, bestResult(exerciseId, attempts), lighter,
                "Nijedno ponavljanje – pokušajte s manjom težinom.")
        }
        if (toFailure > MAX_REPS) {
            val target = loads.roundDown(type, OneRepMax.weightFor(estimate, 8))
            val nextWeight = when {
                target != null && target > last.weightKg -> target
                else -> loads.next(type, last.weightKg)
            }
            if (nextWeight == null) {
                return AttemptEvaluation(Verdict.DONE, result(exerciseId, last, Confidence.LOW), null,
                    "Dosegnuta je najveća dostupna težina – procjena je manje pouzdana.")
            }
            if (attempts.size >= MAX_ATTEMPTS) {
                return AttemptEvaluation(Verdict.DONE, result(exerciseId, last, Confidence.LOW), null,
                    "Iskorištena su sva 3 pokušaja – procjena je manje pouzdana i ispravit će se kroz treninge.")
            }
            return AttemptEvaluation(Verdict.TOO_LIGHT, null, nextWeight,
                "Previše ponavljanja za pouzdanu procjenu. Odmorite 3 minute pa pokušajte s ${formatKg(nextWeight)} kg.")
        }
        if (last.reps < MIN_REPS) {
            return AttemptEvaluation(Verdict.DONE, result(exerciseId, last, Confidence.MEDIUM), null,
                "Težina je bila blizu maksimuma. Procjena je prihvaćena; ne idite teže.")
        }
        return AttemptEvaluation(Verdict.DONE, result(exerciseId, last, Confidence.HIGH), null,
            "Test vježbe je završen.")
    }

    private fun bestResult(exerciseId: String, attempts: List<TestAttempt>): TestResult? =
        attempts.filter { it.reps > 0 && it.pain < PAIN_STOP }
            .maxByOrNull { OneRepMax.estimate(it.weightKg, it.reps, it.rir) }
            ?.let { result(exerciseId, it, Confidence.LOW) }

    private fun result(exerciseId: String, a: TestAttempt, confidence: Confidence) = TestResult(
        exerciseId = exerciseId,
        weightKg = a.weightKg,
        reps = a.reps,
        rir = a.rir,
        estimatedOneRmKg = OneRepMax.estimate(a.weightKg, a.reps, a.rir),
        confidence = confidence,
    )

    fun requiredEquipmentMissing(profile: UserProfile): Set<Equipment> =
        setOf(Equipment.TRAP_BAR, Equipment.DUMBBELL) - profile.equipment.available
}

@Serializable
data class TestAttempt(val weightKg: Double, val reps: Int, val rir: Int, val pain: Int = 0)

@Serializable
enum class Confidence(val label: String) { HIGH("visoka"), MEDIUM("srednja"), LOW("niska") }

@Serializable
data class TestResult(
    val exerciseId: String,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val estimatedOneRmKg: Double,
    val confidence: Confidence,
)

enum class Verdict { DONE, TOO_LIGHT, TOO_HEAVY, STOP_PAIN }

data class AttemptEvaluation(
    val verdict: Verdict,
    /** Rezultat ako je test vježbe gotov (ili najbolja dosadašnja procjena kod prekida). */
    val result: TestResult?,
    /** Predložena težina sljedećeg pokušaja. */
    val nextWeightKg: Double?,
    val message: String,
)

@Serializable
data class FunctionalTest(
    /** Broj ustajanja sa stolice u 30 s. */
    val chairStands30s: Int,
    /** Sekunde stajanja na jednoj nozi (najviše 30). */
    val balanceLeftSec: Int,
    val balanceRightSec: Int,
)

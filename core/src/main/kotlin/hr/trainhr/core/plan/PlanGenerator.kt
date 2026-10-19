package hr.trainhr.core.plan

import hr.trainhr.core.model.Equipment
import hr.trainhr.core.model.Exercise
import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.Experience
import hr.trainhr.core.model.Focus
import hr.trainhr.core.model.MovementPattern
import hr.trainhr.core.model.MovementPattern.ARMS
import hr.trainhr.core.model.MovementPattern.CALF
import hr.trainhr.core.model.MovementPattern.CARRY
import hr.trainhr.core.model.MovementPattern.CORE
import hr.trainhr.core.model.MovementPattern.HINGE
import hr.trainhr.core.model.MovementPattern.HORIZONTAL_PULL
import hr.trainhr.core.model.MovementPattern.HORIZONTAL_PUSH
import hr.trainhr.core.model.MovementPattern.SHOULDERS
import hr.trainhr.core.model.MovementPattern.SINGLE_LEG
import hr.trainhr.core.model.MovementPattern.SQUAT
import hr.trainhr.core.model.MovementPattern.VERTICAL_PULL
import hr.trainhr.core.model.MovementPattern.VERTICAL_PUSH
import hr.trainhr.core.model.PlanSettings
import hr.trainhr.core.model.Prescription
import hr.trainhr.core.model.Sport
import hr.trainhr.core.model.TrainingPlan
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.model.WorkoutTemplate
import hr.trainhr.core.test.InitialTest

/**
 * Slaže tjedni plan po obrascima pokreta. 2–3 dana: cijelo tijelo svaki put;
 * 4 dana: donji/gornji dio dvaput tjedno – tako se svaka glavna mišićna skupina
 * trenira najmanje 2× tjedno (ACSM 2026).
 */
object PlanGenerator {

    fun generate(profile: UserProfile, settings: PlanSettings, catalog: ExerciseCatalog): TrainingPlan {
        val days = settings.daysPerWeek.coerceIn(2, 4)
        val slotsPerDay = exercisesFor(settings.durationMinutes)
        val dayPatterns: List<List<MovementPattern>> = if (days == 4) {
            listOf(lowerDay(profile.sport, settings.focus), upperDay(profile.sport, settings.focus)).let { it + it }
        } else {
            List(days) { fullBodyDay(it, days, profile.sport, settings.focus) }
        }
        val candidates = catalog.available(profile.equipment.available)
            .filter { it.pattern != MovementPattern.CONDITIONING && it.pattern != MovementPattern.BALANCE }
            .filter { it.stresses.intersect(profile.limitations).isEmpty() }

        val workouts = dayPatterns.mapIndexed { dayIndex, patterns ->
            val used = mutableSetOf<String>()
            val items = patterns.take(slotsPerDay).mapNotNull { pattern ->
                pick(pattern, candidates, profile.sport, dayIndex, used)?.let { ex ->
                    used += ex.id
                    prescribe(ex, profile.experience)
                }
            }
            WorkoutTemplate(
                name = dayName(dayIndex, days),
                warmup = warmup(profile.sport, profile.equipment.available),
                items = items,
            )
        }
        return TrainingPlan(settings.copy(daysPerWeek = days), workouts)
    }

    /** Broj vježbi koji stane u trajanje (uz ~8 min zagrijavanja). */
    fun exercisesFor(durationMinutes: Int): Int = when {
        durationMinutes <= 30 -> 4
        durationMinutes <= 45 -> 5
        durationMinutes <= 60 -> 7
        else -> 8
    }

    fun prescribe(ex: Exercise, experience: Experience): Prescription {
        if (ex.timed) {
            return Prescription(ex.id, sets = 2, repMin = 20, repMax = 40, targetRir = 2, restSeconds = 60)
        }
        if (!ex.compound) {
            return Prescription(ex.id, sets = 2, repMin = 10, repMax = 15, targetRir = 2, restSeconds = 75)
        }
        return when (experience) {
            Experience.BEGINNER -> Prescription(ex.id, 2, 10, 15, 3, 120)
            Experience.INTERMEDIATE -> Prescription(ex.id, 3, 8, 12, 2, 120)
            Experience.ADVANCED -> Prescription(ex.id, 3, 6, 10, 2, 150)
        }
    }

    private fun fullBodyDay(day: Int, days: Int, sport: Sport, focus: Focus): List<MovementPattern> {
        val lower = if (day % 2 == 0) listOf(SQUAT, HINGE) else listOf(HINGE, SINGLE_LEG)
        // Uz samo 2 dana oba dana imaju vodoravni potisak, inače bi prsa bila samo 1× tjedno.
        val push = if (day % 2 == 0 || days == 2) HORIZONTAL_PUSH else VERTICAL_PUSH
        val pull = if (day % 2 == 0) HORIZONTAL_PULL else VERTICAL_PULL
        val base = mutableListOf(lower[0], push, pull, lower[1])
        base += focusExtras(focus, day)
        base += sportExtras(sport, day)
        base += listOf(CORE, ARMS, CARRY)
        return base.distinctKeepingOrder()
    }

    private fun lowerDay(sport: Sport, focus: Focus): List<MovementPattern> {
        val l = mutableListOf(SQUAT, HINGE, SINGLE_LEG)
        if (focus == Focus.CORE) l += CORE
        l += sportExtras(sport, 0).filter { it in LOWER_OR_CORE }
        l += listOf(CORE, CALF, CARRY)
        return l.distinctKeepingOrder()
    }

    private fun upperDay(sport: Sport, focus: Focus): List<MovementPattern> {
        val u = mutableListOf(HORIZONTAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, VERTICAL_PUSH)
        if (focus == Focus.CORE) u.add(0, CORE)
        u += sportExtras(sport, 1).filter { it !in LOWER_OR_CORE }
        u += listOf(ARMS, SHOULDERS, CORE)
        return u.distinctKeepingOrder()
    }

    private fun focusExtras(focus: Focus, day: Int): List<MovementPattern> = when (focus) {
        Focus.FULL_BODY -> emptyList()
        Focus.LEGS -> listOf(SINGLE_LEG, SQUAT)
        Focus.POSTERIOR_CHAIN -> listOf(HINGE, HORIZONTAL_PULL)
        Focus.UPPER_BODY -> if (day % 2 == 0) listOf(VERTICAL_PULL, VERTICAL_PUSH) else listOf(HORIZONTAL_PULL, HORIZONTAL_PUSH)
        Focus.CORE -> listOf(CORE)
    }

    /**
     * Sportski naglasci (polazne točke, ne konačan program):
     * veslanje – trup/anti-rotacija i potisak kao protuteža povlačenju; trčanje – jednonožno i listovi;
     * biciklizam – nošenje (opterećenje kostiju i trupa) i gornji dio tijela.
     */
    private fun sportExtras(sport: Sport, day: Int): List<MovementPattern> = when (sport) {
        Sport.GENERAL -> emptyList()
        Sport.ROWING -> listOf(CORE, if (day % 2 == 0) VERTICAL_PUSH else HORIZONTAL_PUSH)
        Sport.RUNNING -> listOf(SINGLE_LEG, CALF)
        Sport.CYCLING -> listOf(CARRY, if (day % 2 == 0) VERTICAL_PULL else HORIZONTAL_PUSH)
    }

    private fun pick(
        pattern: MovementPattern,
        candidates: List<Exercise>,
        sport: Sport,
        dayIndex: Int,
        used: Set<String>,
    ): Exercise? {
        val options = candidates.filter { it.pattern == pattern && it.id !in used }
            .sortedWith(
                compareByDescending<Exercise> { sport in it.sports }
                    .thenByDescending { it.id in TESTED }
                    .thenByDescending { it.compound }
                    .thenBy { it.id },
            )
        if (options.isEmpty()) return null
        // Isti obrazac u različitim danima koristi različite varijante kad ih ima.
        return options[(dayIndex / 2) % options.size.coerceAtMost(2)]
    }

    private fun warmup(sport: Sport, equipment: Set<Equipment>): String {
        val erg = when {
            sport == Sport.ROWING && Equipment.ROW_ERG in equipment -> "RowErg"
            sport == Sport.CYCLING && Equipment.BIKE_ERG in equipment -> "BikeErg"
            Equipment.BIKE_ERG in equipment -> "BikeErg"
            Equipment.ROW_ERG in equipment -> "RowErg"
            else -> null
        }
        val cardio = erg?.let { "5–8 min $it laganim tempom" } ?: "5–8 min laganog hodanja"
        return "$cardio, zatim mobilnost kukova i ramena te 1–2 lagane serije prve vježbe."
    }

    private fun dayName(index: Int, days: Int): String =
        if (days == 4) listOf("Donji dio A", "Gornji dio A", "Donji dio B", "Gornji dio B")[index]
        else "Trening ${'A' + index}"

    private fun <T> List<T>.distinctKeepingOrder(): List<T> = LinkedHashSet(this).toList()

    private val LOWER_OR_CORE = setOf(SQUAT, HINGE, SINGLE_LEG, CALF, CORE, CARRY)

    /** Vježbe iz početnog testa – imaju procijenjeni 1RM pa im se daje prednost. */
    private val TESTED = InitialTest.allTestExerciseIds
}

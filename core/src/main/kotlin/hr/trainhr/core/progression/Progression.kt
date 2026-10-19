package hr.trainhr.core.progression

import hr.trainhr.core.model.Exercise
import hr.trainhr.core.model.Experience
import hr.trainhr.core.model.LoadType
import hr.trainhr.core.model.Prescription
import hr.trainhr.core.model.SetLog
import hr.trainhr.core.util.Loads
import hr.trainhr.core.util.OneRepMax
import hr.trainhr.core.util.formatKg
import kotlinx.serialization.Serializable

/** Trenutno stanje vježbe za korisnika: radna težina i broj serija. */
@Serializable
data class ExerciseState(
    val exerciseId: String,
    /** null = vježba još nije kalibrirana (nije testirana ni odrađena). */
    val workingWeightKg: Double?,
    /** Dodatne serije dodane umjesto prevelikog skoka težine. */
    val extraSets: Int = 0,
    /** Uzastopni treninzi ispod donje granice ponavljanja. */
    val failStreak: Int = 0,
)

data class Decision(val state: ExerciseState, val reason: String)

/**
 * Dvostruka progresija: kad sve serije dosegnu gornju granicu ponavljanja uz ciljani RIR,
 * težina se diže za najmanji korak opreme. Ako bi taj korak bio veći od [MAX_JUMP]
 * (npr. bučice 8 → 10 kg = +25 %), najprije se dodaje serija.
 */
object Progression {

    const val MAX_JUMP = 0.10
    const val MAX_EXTRA_SETS = 1
    const val PAIN_LIMIT = 3
    const val FAILS_BEFORE_DECREASE = 2

    /**
     * Početni udio 1RM prema iskustvu – ACSM (2011) za starije osobe: početnici 40–50 % 1RM,
     * početni do srednji 60–70 %. Uzimamo donji, oprezniji kraj; progresija dalje radi sama.
     */
    fun startFraction(experience: Experience): Double = when (experience) {
        Experience.BEGINNER -> 0.50
        Experience.INTERMEDIATE -> 0.60
        Experience.ADVANCED -> 0.70
    }

    fun initialWeight(
        oneRm: Double,
        p: Prescription,
        exercise: Exercise,
        experience: Experience,
        loads: Loads,
    ): Double? {
        if (exercise.loadType == LoadType.NONE) return null
        // Nikad teže od onoga s čime se mogu napraviti sva ponavljanja uz ciljani RIR.
        val feasible = OneRepMax.weightFor(oneRm, p.repMax + p.targetRir)
        val target = minOf(oneRm * startFraction(experience), feasible)
        return loads.roundDown(exercise.loadType, target)
    }

    fun next(
        p: Prescription,
        exercise: Exercise,
        state: ExerciseState,
        logged: List<SetLog>,
        loads: Loads,
        experience: Experience,
    ): Decision {
        if (logged.isEmpty()) return Decision(state, "Nema zabilježenih serija.")
        val type = exercise.loadType
        val maxPain = logged.maxOf { it.pain }
        val current = state.workingWeightKg ?: logged.mapNotNull { it.weightKg }.maxOrNull()

        if (type == LoadType.NONE || current == null) {
            return Decision(state, "Bez opterećenja – napredujte ponavljanjima ili trajanjem.")
        }

        if (maxPain >= PAIN_LIMIT) {
            val lower = loads.previous(type, current) ?: current
            return Decision(
                state.copy(workingWeightKg = lower, extraSets = 0, failStreak = 0),
                "Bol $maxPain/10 – težina smanjena na ${formatKg(lower)} kg. Ako se bol ponavlja, zamijenite vježbu.",
            )
        }

        if (state.workingWeightKg == null) {
            return calibrate(p, exercise, state, logged, loads, experience)
        }

        val allTop = logged.size >= p.sets && logged.all {
            it.reps >= p.repMax && (it.rir == null || it.rir >= p.targetRir - 1)
        }
        val anyBelow = logged.any { it.reps < p.repMin }

        if (allTop) {
            val up = loads.next(type, current)
                ?: return if (state.extraSets < MAX_EXTRA_SETS) {
                    Decision(state.copy(extraSets = state.extraSets + 1, failStreak = 0),
                        "Najveća dostupna težina – dodana serija.")
                } else {
                    Decision(state.copy(failStreak = 0), "Najveća dostupna težina – zadržano.")
                }
            val jump = (up - current) / current
            if (jump > MAX_JUMP && state.extraSets < MAX_EXTRA_SETS) {
                return Decision(
                    state.copy(extraSets = state.extraSets + 1, failStreak = 0),
                    "Sljedeći korak (${formatKg(up)} kg) je +${(jump * 100).toInt()} % – najprije dodana serija.",
                )
            }
            return Decision(
                state.copy(workingWeightKg = up, extraSets = 0, failStreak = 0),
                "Sve serije na gornjoj granici – težina povećana na ${formatKg(up)} kg.",
            )
        }

        if (anyBelow) {
            val streak = state.failStreak + 1
            if (streak >= FAILS_BEFORE_DECREASE) {
                val lower = loads.previous(type, current) ?: current
                return Decision(
                    state.copy(workingWeightKg = lower, extraSets = 0, failStreak = 0),
                    "Dva treninga zaredom ispod ${p.repMin} ponavljanja – težina smanjena na ${formatKg(lower)} kg.",
                )
            }
            return Decision(state.copy(failStreak = streak), "Ispod donje granice – težina zadržana, pokušajte ponovno.")
        }

        return Decision(state.copy(failStreak = 0), "Napredak unutar raspona – težina zadržana.")
    }

    private fun calibrate(
        p: Prescription,
        exercise: Exercise,
        state: ExerciseState,
        logged: List<SetLog>,
        loads: Loads,
        experience: Experience,
    ): Decision {
        val best = logged.filter { it.weightKg != null && it.reps > 0 }
            .maxByOrNull { OneRepMax.estimate(it.weightKg!!, it.reps, it.rir ?: 0) }
            ?: return Decision(state, "Kalibracija nije moguća – upišite težinu i ponavljanja.")
        val oneRm = OneRepMax.estimate(best.weightKg!!, best.reps, best.rir ?: 0)
        // Nakon kalibracije koristimo izvedenu seriju, ali ne više od procjene za raspon.
        val fromRange = OneRepMax.weightFor(oneRm, p.repMax + p.targetRir)
        val start = loads.roundDown(exercise.loadType, minOf(fromRange, best.weightKg))
            ?: return Decision(state, "Bez opterećenja.")
        return Decision(
            state.copy(workingWeightKg = start, extraSets = 0, failStreak = 0),
            "Kalibrirano: radna težina ${formatKg(start)} kg.",
        )
    }

    /** Lakši tjedan: jedna serija manje i ~10 % manja težina (zaokruženo na opremu). */
    fun deload(p: Prescription, weightKg: Double?, exercise: Exercise, loads: Loads): Pair<Int, Double?> {
        val sets = (p.sets - 1).coerceAtLeast(1)
        val w = weightKg?.let { loads.roundDown(exercise.loadType, it * 0.9) }
        return sets to w
    }

    fun isDeloadWeek(weekIndex: Int, every: Int): Boolean =
        every > 1 && weekIndex % every == every - 1
}

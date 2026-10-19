package hr.trainhr.core

import java.util.Locale

enum class Action { START, INCREASE, HOLD, DECREASE }

data class Recommendation(
    val action: Action,
    /** Preporučena težina; null za vlastitu težinu ili kad još nema podataka. */
    val weightKg: Double?,
    val text: String,
)

/**
 * Preporuka težine iz povijesti (dvostruka progresija):
 * - sve serije na gornjoj granici ponavljanja → povećaj za korak opreme,
 * - dva treninga zaredom s nekom serijom ispod donje granice → smanji za korak,
 * - inače zadrži.
 */
object Advisor {

    const val TARGET_SETS = 3

    fun recommend(exercise: Exercise, range: RepRange, history: List<List<SetEntry>>): Recommendation {
        val sessions = history.filter { it.isNotEmpty() }
        val min = if (exercise.timed) TIMED_MIN_SEC else range.min
        val max = if (exercise.timed) TIMED_MAX_SEC else range.max
        val unit = if (exercise.timed) "s" else "ponavljanja"
        val last = sessions.lastOrNull()
            ?: return Recommendation(Action.START, null,
                "Prvi put: izaberite težinu s kojom možete $TARGET_SETS serije po $min–$max $unit uz dobru tehniku.")

        val weight = last.mapNotNull { it.weightKg }.maxOrNull()
        val working = workingSets(last)
        val allTop = working.size >= TARGET_SETS && working.all { it.reps >= max }
        val below = working.any { it.reps < min }
        val prevBelow = sessions.getOrNull(sessions.size - 2)?.let { s -> workingSets(s).any { it.reps < min } } == true
        val step = exercise.kind.stepKg

        if (weight == null || step == 0.0) {
            return when {
                allTop -> Recommendation(Action.INCREASE, null,
                    if (exercise.timed) "Sve serije ≥ $max s – produljite ili otežajte vježbu."
                    else "Sve serije ≥ $max ponavljanja – otežajte varijantu ili dodajte ponavljanja.")
                else -> Recommendation(Action.HOLD, null, "Nastavite i pokušajte dodati ponavljanje ili sekundu.")
            }
        }
        return when {
            allTop -> {
                val up = weight + step
                Recommendation(Action.INCREASE, up,
                    "Sve serije na $max $unit – povećajte na ${kg(up)} kg.")
            }
            below && prevBelow -> {
                val down = (weight - step).coerceAtLeast(0.0)
                Recommendation(Action.DECREASE, down,
                    "Dva treninga zaredom ispod $min $unit – smanjite na ${kg(down)} kg.")
            }
            below -> Recommendation(Action.HOLD, weight,
                "Ispod $min $unit – ostanite na ${kg(weight)} kg i pokušajte ponovno.")
            else -> Recommendation(Action.HOLD, weight,
                "Ostanite na ${kg(weight)} kg dok sve serije ne dođu do $max $unit.")
        }
    }
}

/** Radne serije = serije s najvećom težinom (lakše serije za zagrijavanje se ne računaju). */
fun workingSets(sets: List<SetEntry>): List<SetEntry> {
    val top = sets.mapNotNull { it.weightKg }.maxOrNull() ?: return sets
    return sets.filter { it.weightKg == top }
}

/** Kilogrami u hrvatskom zapisu: 22,5 / 20. */
fun kg(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString()
    else String.format(Locale.forLanguageTag("hr"), "%.2f", v).trimEnd('0').trimEnd(',')

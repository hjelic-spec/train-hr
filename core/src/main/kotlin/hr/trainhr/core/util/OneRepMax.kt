package hr.trainhr.core.util

/**
 * Procjena maksimuma za jedno ponavljanje (1RM) iz submaksimalne serije.
 *
 * Ponavljanja "u rezervi" (RIR) se pribrajaju izvedenim ponavljanjima, jer formule
 * pretpostavljaju seriju do otkaza. Brzycki se koristi do 10 ponavljanja (unutar raspona
 * za koji je formula namijenjena), iznad toga Epley.
 */
object OneRepMax {

    const val BRZYCKI_MAX_REPS = 10

    fun brzycki(weight: Double, reps: Int): Double {
        require(reps in 1..36) { "Brzycki: ponavljanja moraju biti 1–36" }
        return weight * 36.0 / (37.0 - reps)
    }

    fun epley(weight: Double, reps: Int): Double {
        require(reps >= 1)
        return if (reps == 1) weight else weight * (1.0 + reps / 30.0)
    }

    /** Procijenjeni 1RM; [rir] = ponavljanja koja su ostala u rezervi. */
    fun estimate(weight: Double, reps: Int, rir: Int = 0): Double {
        val toFailure = reps + rir.coerceAtLeast(0)
        return if (toFailure <= BRZYCKI_MAX_REPS) brzycki(weight, toFailure) else epley(weight, toFailure)
    }

    /** Težina s kojom bi se moglo napraviti [repsToFailure] ponavljanja do otkaza. */
    fun weightFor(oneRm: Double, repsToFailure: Int): Double {
        require(repsToFailure >= 1)
        return if (repsToFailure <= BRZYCKI_MAX_REPS) {
            oneRm * (37.0 - repsToFailure) / 36.0
        } else {
            oneRm / (1.0 + repsToFailure / 30.0)
        }
    }
}

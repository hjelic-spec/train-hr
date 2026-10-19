package hr.trainhr.core

/** Za vježbe na vrijeme (plank, nošenje) raspon je u sekundama. */
const val TIMED_MIN_SEC = 20
const val TIMED_MAX_SEC = 45

/** Broj serija po vježbi. */
const val SETS_PER_EXERCISE = 3

/** Zagrijavanje na početku treninga (min) – uračunava se u trajanje. */
const val WARMUP_MINUTES = 5

/** Procijenjeno trajanje jednog ponavljanja (s) za izračun trajanja serije. */
const val SECONDS_PER_REP = 4

/** Ponuđena trajanja treninga (min). */
val DURATIONS = listOf(30, 45, 60, 75, 90)

object Timing {

    fun workSeconds(exercise: Exercise?, goal: Goal): Int =
        if (exercise?.timed == true) TIMED_MAX_SEC else goal.repMax * SECONDS_PER_REP

    /** Vrijeme jedne vježbe: serije + odmori između serija (bez odmora nakon vježbe). */
    fun exerciseSeconds(exercise: Exercise?, goal: Goal): Int =
        SETS_PER_EXERCISE * workSeconds(exercise, goal) + (SETS_PER_EXERCISE - 1) * goal.restBetweenSetsSec

    /** Procijenjeno trajanje treninga u minutama, uključujući zagrijavanje. */
    fun workoutMinutes(exercises: List<Exercise?>, goal: Goal): Int {
        if (exercises.isEmpty()) return WARMUP_MINUTES
        val sec = exercises.sumOf { exerciseSeconds(it, goal) } + (exercises.size - 1) * goal.restBetweenExercisesSec
        return WARMUP_MINUTES + (sec + 59) / 60
    }

    /** Koliko vježbi stane u trening zadanog trajanja (najmanje 1). */
    fun capacity(durationMinutes: Int, goal: Goal): Int {
        val available = (durationMinutes - WARMUP_MINUTES) * 60
        val perExercise = exerciseSeconds(null, goal) + goal.restBetweenExercisesSec
        return ((available + goal.restBetweenExercisesSec) / perExercise).coerceAtLeast(1)
    }
}

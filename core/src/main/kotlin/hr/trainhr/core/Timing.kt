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

/** Ponuđeni odmori (s). */
val REST_SET_OPTIONS = listOf(60, 90, 120, 150, 180, 240)
val REST_EXERCISE_OPTIONS = listOf(60, 90, 120, 180, 240)

/** Ponuđena trajanja treninga (min). */
val DURATIONS = listOf(30, 45, 60, 75, 90)

object Timing {

    /** Trajanje serije; za nepoznatu vježbu (izračun kapaciteta) uzima se dulje od dvoga. */
    fun workSeconds(exercise: Exercise?, goal: Goal): Int = when {
        exercise == null -> maxOf(TIMED_MAX_SEC, goal.repMax * SECONDS_PER_REP)
        exercise.timed -> TIMED_MAX_SEC
        else -> goal.repMax * SECONDS_PER_REP
    }

    /** Vrijeme jedne vježbe: serije + odmori između serija (bez odmora nakon vježbe). */
    fun exerciseSeconds(exercise: Exercise?, goal: Goal, restSets: Int = goal.restBetweenSetsSec): Int =
        SETS_PER_EXERCISE * workSeconds(exercise, goal) + (SETS_PER_EXERCISE - 1) * restSets

    /** Procijenjeno trajanje treninga u minutama, uključujući zagrijavanje. */
    fun workoutMinutes(
        exercises: List<Exercise?>,
        goal: Goal,
        restSets: Int = goal.restBetweenSetsSec,
        restExercises: Int = goal.restBetweenExercisesSec,
    ): Int {
        if (exercises.isEmpty()) return WARMUP_MINUTES
        val sec = exercises.sumOf { exerciseSeconds(it, goal, restSets) } + (exercises.size - 1) * restExercises
        return WARMUP_MINUTES + (sec + 59) / 60
    }

    /** Koliko vježbi stane u trening zadanog trajanja (najmanje 1). */
    fun capacity(
        durationMinutes: Int,
        goal: Goal,
        restSets: Int = goal.restBetweenSetsSec,
        restExercises: Int = goal.restBetweenExercisesSec,
    ): Int {
        val available = (durationMinutes - WARMUP_MINUTES) * 60
        val perExercise = exerciseSeconds(null, goal, restSets) + restExercises
        return ((available + restExercises) / perExercise).coerceAtLeast(1)
    }
}

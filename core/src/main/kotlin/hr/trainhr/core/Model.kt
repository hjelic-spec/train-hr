package hr.trainhr.core

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup(val label: String) {
    LEGS("Noge"),
    BACK("Leđa"),
    CHEST("Prsa"),
    SHOULDERS("Ramena"),
    ARMS("Ruke"),
    CORE("Trup"),
}

/** Vrsta opterećenja određuje korak povećanja težine. */
@Serializable
enum class LoadKind(val label: String, val stepKg: Double) {
    DUMBBELL("Bučice", 2.0),
    BARBELL("Šipka / trap bar", 2.5),
    MACHINE("Mašina / sajla", 5.0),
    BODYWEIGHT("Vlastita težina", 0.0),
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val group: MuscleGroup,
    val kind: LoadKind,
    /** Bilježi se u sekundama (plank, nošenje) umjesto ponavljanja. */
    val timed: Boolean = false,
)

@Serializable
enum class RepRange(val min: Int, val max: Int) {
    R6_10(6, 10),
    R8_12(8, 12),
    R10_15(10, 15);

    val label: String get() = "$min–$max"
}

/** Za vježbe na vrijeme raspon je u sekundama. */
const val TIMED_MIN_SEC = 20
const val TIMED_MAX_SEC = 45

@Serializable
data class SetEntry(val weightKg: Double?, val reps: Int)

@Serializable
data class Workout(val name: String, val exerciseIds: List<String>)

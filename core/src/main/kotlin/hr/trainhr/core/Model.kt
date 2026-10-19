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
    /** Samo za vlastite vježbe s bučicama: bučica u svakoj ruci (volumen se računa dvostruko). */
    val pair: Boolean? = null,
)

/**
 * Cilj treninga. Rasponi i odmori su zadane vrijednosti aplikacije, oslonjene na ACSM (2009):
 * hipertrofija ~8–12 RM uz 1–2 min odmora; snaga – teže serije s malo ponavljanja i duljim odmorom.
 * Za 50+ snaga je ograničena na najmanje 5 ponavljanja.
 */
@Serializable
enum class Goal(
    val label: String,
    val repMin: Int,
    val repMax: Int,
    val restBetweenSetsSec: Int,
    val restBetweenExercisesSec: Int,
) {
    STRENGTH("Snaga", 5, 8, 180, 180),
    HYPERTROPHY("Hipertrofija", 8, 12, 90, 120);

    val repLabel: String get() = "$repMin–$repMax"
}

@Serializable
data class SetEntry(
    val weightKg: Double?,
    val reps: Int,
    /** Trajanje serije (štoperica), ako je mjereno. */
    val workSeconds: Int? = null,
)



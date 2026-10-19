package hr.trainhr.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentSettings(
    val available: Set<Equipment> = Equipment.entries.toSet(),
    /** Težine pojedinačnih bučica koje postoje u teretani (kg). */
    val dumbbells: List<Double> = (8..40 step 2).map { it.toDouble() },
    val barbellKg: Double = 20.0,
    val trapBarKg: Double = 25.0,
    /** Najmanja ploča (kg); po jedna na svaku stranu → najmanji korak = 2 × ova vrijednost. */
    val smallestPlateKg: Double = 1.25,
    val latStackStepKg: Double = 5.0,
    val latStackMaxKg: Double = 100.0,
    val cableStackStepKg: Double = 5.0,
    val cableStackMaxKg: Double = 100.0,
)

@Serializable
data class UserProfile(
    val name: String = "",
    val age: Int = 55,
    val sex: Sex = Sex.MALE,
    val bodyWeightKg: Double = 80.0,
    val experience: Experience = Experience.BEGINNER,
    val sport: Sport = Sport.GENERAL,
    val limitations: Set<BodyRegion> = emptySet(),
    val equipment: EquipmentSettings = EquipmentSettings(),
    val readinessAnswers: List<Boolean> = emptyList(),
    /** Korisnik je potvrdio upozorenje upitnika (konzultacija s liječnikom / vlastita odgovornost). */
    val readinessAcknowledged: Boolean = false,
)

@Serializable
data class PlanSettings(
    val daysPerWeek: Int = 3,
    val durationMinutes: Int = 45,
    val focus: Focus = Focus.FULL_BODY,
    val mode: PlanMode = PlanMode.AUTOMATIC,
    /** Svaki koliki tjedan je lakši (deload). */
    val deloadEveryWeeks: Int = 5,
)

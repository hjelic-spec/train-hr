package hr.trainhr.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Prescription(
    val exerciseId: String,
    val sets: Int,
    /** Za vježbe na vrijeme: sekunde. */
    val repMin: Int,
    val repMax: Int,
    /** Ciljani broj ponavljanja "u rezervi" (RIR). */
    val targetRir: Int,
    val restSeconds: Int,
    /** Automatska progresija težine; može se isključiti po vježbi. */
    val autoProgression: Boolean = true,
)

@Serializable
data class WorkoutTemplate(
    val name: String,
    val warmup: String,
    val items: List<Prescription>,
)

@Serializable
data class TrainingPlan(
    val settings: PlanSettings,
    val workouts: List<WorkoutTemplate>,
)

/** Jedna odrađena serija. */
@Serializable
data class SetLog(
    val weightKg: Double?,
    val reps: Int,
    val rir: Int?,
    /** Bol 0–10. */
    val pain: Int = 0,
)

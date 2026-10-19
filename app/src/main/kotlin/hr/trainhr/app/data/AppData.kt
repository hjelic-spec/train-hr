package hr.trainhr.app.data

import hr.trainhr.core.model.Exercise
import hr.trainhr.core.model.SetLog
import hr.trainhr.core.model.TrainingPlan
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.progression.ExerciseState
import hr.trainhr.core.test.FunctionalTest
import hr.trainhr.core.test.TestResult
import kotlinx.serialization.Serializable

/** Svi podaci korisnika – spremaju se u jednu JSON datoteku na uređaju. */
@Serializable
data class AppData(
    val version: Int = 1,
    val profile: UserProfile? = null,
    val readinessDone: Boolean = false,
    val equipmentDone: Boolean = false,
    val test: InitialTestState = InitialTestState(),
    /** Povijest završenih početnih testova (zadnji je aktualan). */
    val testHistory: List<CompletedTest> = emptyList(),
    val plan: TrainingPlan? = null,
    val planStartEpochDay: Long? = null,
    val exerciseStates: Map<String, ExerciseState> = emptyMap(),
    val sessions: List<SessionRecord> = emptyList(),
    val customExercises: List<Exercise> = emptyList(),
    val lastExportEpochDay: Long? = null,
)

/** Test u tijeku. */
@Serializable
data class InitialTestState(
    val results: Map<String, TestResult> = emptyMap(),
    /** Vježbe koje korisnik nije mogao izvesti (bol, ograničenje). */
    val skipped: Set<String> = emptySet(),
    val functional: FunctionalTest? = null,
)

@Serializable
data class CompletedTest(
    val epochDay: Long,
    val results: Map<String, TestResult>,
    val skipped: Set<String>,
    val functional: FunctionalTest?,
)

@Serializable
data class SessionRecord(
    val id: Long,
    val epochDay: Long,
    val workoutName: String,
    val deload: Boolean,
    val entries: List<SessionEntry>,
)

@Serializable
data class SessionEntry(
    val exerciseId: String,
    val sets: List<SetLog>,
    /** Obrazloženje odluke o progresiji ("težina povećana jer…"). */
    val note: String = "",
)

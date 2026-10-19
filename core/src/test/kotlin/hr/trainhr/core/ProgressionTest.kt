package hr.trainhr.core

import hr.trainhr.core.model.EquipmentSettings
import hr.trainhr.core.model.ExerciseLibrary
import hr.trainhr.core.model.Experience
import hr.trainhr.core.model.Prescription
import hr.trainhr.core.model.SetLog
import hr.trainhr.core.progression.ExerciseState
import hr.trainhr.core.progression.Progression
import hr.trainhr.core.util.Loads
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressionTest {
    private val loads = Loads(EquipmentSettings())
    private val deadlift = ExerciseLibrary.require("trap_bar_deadlift")
    private val curl = ExerciseLibrary.require("db_curl")
    private val p = Prescription("trap_bar_deadlift", 3, 8, 12, 2, 120)

    private fun sets(w: Double, vararg reps: Int, rir: Int = 2) = reps.map { SetLog(w, it, rir) }

    @Test
    fun increasesWeightWhenAllSetsAtTop() {
        val d = Progression.next(p, deadlift, ExerciseState(deadlift.id, 60.0), sets(60.0, 12, 12, 12), loads, Experience.INTERMEDIATE)
        assertEquals(62.5, d.state.workingWeightKg)
    }

    @Test
    fun holdsInsideRange() {
        val d = Progression.next(p, deadlift, ExerciseState(deadlift.id, 60.0), sets(60.0, 12, 10, 9), loads, Experience.INTERMEDIATE)
        assertEquals(60.0, d.state.workingWeightKg)
    }

    @Test
    fun bigDumbbellJumpAddsSetFirst() {
        val pc = Prescription("db_curl", 2, 10, 15, 2, 75)
        val first = Progression.next(pc, curl, ExerciseState(curl.id, 8.0), sets(8.0, 15, 15), loads, Experience.BEGINNER)
        assertEquals(8.0, first.state.workingWeightKg)
        assertEquals(1, first.state.extraSets)
        val second = Progression.next(pc, curl, first.state, sets(8.0, 15, 15, 15), loads, Experience.BEGINNER)
        assertEquals(10.0, second.state.workingWeightKg)
        assertEquals(0, second.state.extraSets)
    }

    @Test
    fun decreasesAfterTwoFailedSessions() {
        val s1 = Progression.next(p, deadlift, ExerciseState(deadlift.id, 60.0), sets(60.0, 7, 6, 6), loads, Experience.INTERMEDIATE)
        assertEquals(60.0, s1.state.workingWeightKg)
        val s2 = Progression.next(p, deadlift, s1.state, sets(60.0, 7, 6, 6), loads, Experience.INTERMEDIATE)
        assertEquals(57.5, s2.state.workingWeightKg)
    }

    @Test
    fun painReducesWeight() {
        val logged = listOf(SetLog(60.0, 10, 2, pain = 4))
        val d = Progression.next(p, deadlift, ExerciseState(deadlift.id, 60.0), logged, loads, Experience.INTERMEDIATE)
        assertEquals(57.5, d.state.workingWeightKg)
    }

    @Test
    fun calibratesUntestedExercise() {
        val d = Progression.next(p, deadlift, ExerciseState(deadlift.id, null), sets(70.0, 10, rir = 2), loads, Experience.INTERMEDIATE)
        val w = d.state.workingWeightKg!!
        assertTrue(w <= 70.0 && w >= 25.0)
    }

    @Test
    fun beginnerStartsConservatively() {
        val w = Progression.initialWeight(100.0, p, deadlift, Experience.BEGINNER, loads)!!
        assertEquals(50.0, w)
    }

    @Test
    fun deloadWeekSchedule() {
        assertEquals(listOf(4, 9), (0..10).filter { Progression.isDeloadWeek(it, 5) })
    }
}

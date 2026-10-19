package hr.trainhr.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdvisorTest {
    private val curl = ExerciseLibrary.all.first { it.id == "db_curl" }
    private val squat = ExerciseLibrary.all.first { it.id == "back_squat" }
    private val plank = ExerciseLibrary.all.first { it.id == "plank" }
    private val goal = Goal.HYPERTROPHY

    private fun s(w: Double?, vararg reps: Int) = reps.map { SetEntry(w, it) }

    @Test
    fun noHistory() {
        val r = Advisor.recommend(curl, goal, emptyList())
        assertEquals(Action.START, r.action)
        assertNull(r.weightKg)
    }

    @Test
    fun increaseByEquipmentStep() {
        assertEquals(12.0, Advisor.recommend(curl, goal, listOf(s(10.0, 12, 12, 12))).weightKg)
        assertEquals(62.5, Advisor.recommend(squat, goal, listOf(s(60.0, 12, 12, 13))).weightKg)
    }

    @Test
    fun warmupSetsIgnored() {
        val session = s(40.0, 5) + s(60.0, 12, 12, 12)
        assertEquals(Action.INCREASE, Advisor.recommend(squat, goal, listOf(session)).action)
    }

    @Test
    fun holdInsideRange() {
        val r = Advisor.recommend(squat, goal, listOf(s(60.0, 12, 10, 9)))
        assertEquals(Action.HOLD, r.action)
        assertEquals(60.0, r.weightKg)
    }

    @Test
    fun decreaseAfterTwoSessionsBelowMin() {
        val one = Advisor.recommend(squat, goal, listOf(s(60.0, 8, 7, 6)))
        assertEquals(Action.HOLD, one.action)
        val two = Advisor.recommend(squat, goal, listOf(s(60.0, 8, 7, 6), s(60.0, 7, 7, 6)))
        assertEquals(Action.DECREASE, two.action)
        assertEquals(57.5, two.weightKg)
    }

    @Test
    fun timedBodyweight() {
        val r = Advisor.recommend(plank, goal, listOf(s(null, 45, 45, 50)))
        assertEquals(Action.INCREASE, r.action)
        assertNull(r.weightKg)
    }

    @Test
    fun strengthUsesLowerRange() {
        val r = Advisor.recommend(squat, Goal.STRENGTH, listOf(s(80.0, 8, 8, 8)))
        assertEquals(82.5, r.weightKg)
    }

    @Test
    fun croatianKgFormat() {
        assertEquals("22,5", kg(22.5))
        assertEquals("20", kg(20.0))
    }
}

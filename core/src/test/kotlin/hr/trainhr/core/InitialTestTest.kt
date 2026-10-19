package hr.trainhr.core

import hr.trainhr.core.model.Equipment
import hr.trainhr.core.model.EquipmentSettings
import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.test.Confidence
import hr.trainhr.core.test.InitialTest
import hr.trainhr.core.test.TestAttempt
import hr.trainhr.core.test.Verdict
import hr.trainhr.core.util.Loads
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InitialTestTest {
    private val catalog = ExerciseCatalog()
    private val loads = Loads(EquipmentSettings())

    @Test
    fun protocolIncludesTrapBarDeadliftAndBicepsCurl() {
        val ids = InitialTest.exercisesFor(UserProfile(), catalog).map { it.second }
        assertTrue("trap_bar_deadlift" in ids)
        assertTrue("db_curl" in ids)
        assertTrue("back_squat" in ids)
        assertEquals(setOf(1, 2), InitialTest.exercisesFor(UserProfile(), catalog).map { it.first }.toSet())
    }

    @Test
    fun fallsBackWithoutRack() {
        val eq = EquipmentSettings(available = Equipment.entries.toSet() - Equipment.SQUAT_RACK)
        val ids = InitialTest.exercisesFor(UserProfile(equipment = eq), catalog).map { it.second }
        assertTrue("goblet_squat" in ids)
    }

    @Test
    fun validSetFinishesWithHighConfidence() {
        val e = InitialTest.evaluate("trap_bar_deadlift", listOf(TestAttempt(80.0, 7, 1)), loads, catalog)
        assertEquals(Verdict.DONE, e.verdict)
        assertEquals(Confidence.HIGH, e.result!!.confidence)
        assertEquals(80.0 * 36 / 29, e.result!!.estimatedOneRmKg, 1e-9)
    }

    @Test
    fun tooManyRepsSuggestsHeavierWeight() {
        val e = InitialTest.evaluate("db_curl", listOf(TestAttempt(10.0, 14, 2)), loads, catalog)
        assertEquals(Verdict.TOO_LIGHT, e.verdict)
        val next = assertNotNull(e.nextWeightKg)
        assertTrue(next > 10.0)
    }

    @Test
    fun painStopsTest() {
        val e = InitialTest.evaluate("back_squat", listOf(TestAttempt(60.0, 5, 2, pain = 4)), loads, catalog)
        assertEquals(Verdict.STOP_PAIN, e.verdict)
    }

    @Test
    fun threeAttemptsAreTheLimit() {
        val attempts = listOf(TestAttempt(8.0, 20, 3), TestAttempt(10.0, 18, 3), TestAttempt(12.0, 15, 2))
        val e = InitialTest.evaluate("db_curl", attempts, loads, catalog)
        assertEquals(Verdict.DONE, e.verdict)
        assertEquals(Confidence.LOW, e.result!!.confidence)
    }

    @Test
    fun maxDumbbellAcceptedWithLowConfidence() {
        val e = InitialTest.evaluate("db_curl", listOf(TestAttempt(40.0, 15, 0)), loads, catalog)
        assertEquals(Verdict.DONE, e.verdict)
        assertEquals(Confidence.LOW, e.result!!.confidence)
    }
}

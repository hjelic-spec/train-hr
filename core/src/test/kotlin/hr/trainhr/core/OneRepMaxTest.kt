package hr.trainhr.core

import hr.trainhr.core.util.OneRepMax
import kotlin.test.Test
import kotlin.test.assertEquals

class OneRepMaxTest {
    @Test
    fun brzyckiKnownValue() {
        // 100 kg × 10: 100 × 36 / 27 = 133,33
        assertEquals(133.33, OneRepMax.brzycki(100.0, 10), 0.01)
    }

    @Test
    fun rirIsAddedToReps() {
        assertEquals(OneRepMax.brzycki(80.0, 8), OneRepMax.estimate(80.0, 6, rir = 2), 1e-9)
    }

    @Test
    fun epleyAboveTenReps() {
        assertEquals(OneRepMax.epley(50.0, 12), OneRepMax.estimate(50.0, 12), 1e-9)
    }

    @Test
    fun weightForIsInverseOfEstimate() {
        for (reps in listOf(3, 8, 10, 12, 15)) {
            val oneRm = OneRepMax.estimate(70.0, reps)
            assertEquals(70.0, OneRepMax.weightFor(oneRm, reps), 1e-9)
        }
    }
}

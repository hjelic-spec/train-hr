package hr.trainhr.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanBuilderTest {
    private fun ex(id: String) = ExerciseLibrary.all.first { it.id == id }

    @Test
    fun everyGroupInEveryWorkout() {
        val selected = listOf(ex("back_squat"), ex("lat_pulldown"), ex("bench_press"), ex("db_curl"))
        val plan = PlanBuilder.build(3, selected)
        assertEquals(listOf("Trening A", "Trening B", "Trening C"), plan.map { it.name })
        plan.forEach { assertEquals(selected.map { it.id }, it.exerciseIds) }
    }

    @Test
    fun variantsAlternateWhenFewerThanDays() {
        val plan = PlanBuilder.build(3, listOf(ex("back_squat"), ex("trap_bar_deadlift")))
        assertEquals(listOf("back_squat", "trap_bar_deadlift", "back_squat"), plan.map { it.exerciseIds.single() })
    }

    @Test
    fun allSelectedUsedWhenMoreThanDays() {
        val legs = listOf("back_squat", "trap_bar_deadlift", "lunge_db").map(::ex)
        val plan = PlanBuilder.build(2, legs)
        assertEquals(legs.map { it.id }.toSet(), plan.flatMap { it.exerciseIds }.toSet())
        assertTrue(plan.all { it.exerciseIds.isNotEmpty() })
    }

    @Test
    fun emptySelectionGivesEmptyWorkouts() {
        assertTrue(PlanBuilder.build(2, emptyList()).all { it.exerciseIds.isEmpty() })
    }
}

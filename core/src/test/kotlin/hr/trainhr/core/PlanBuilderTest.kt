package hr.trainhr.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanBuilderTest {
    private fun ex(id: String) = ExerciseLibrary.all.first { it.id == id }

    @Test
    fun everyGroupInEveryWorkout() {
        val selected = listOf(ex("back_squat"), ex("lat_pulldown"), ex("bench_press"), ex("db_curl"))
        val plan = PlanBuilder.build(3, selected, 10).workouts
        assertEquals(listOf("Trening A", "Trening B", "Trening C"), plan.map { it.name })
        plan.forEach { assertEquals(selected.map { it.id }, it.exerciseIds) }
    }

    @Test
    fun variantsAlternateWhenFewerThanDays() {
        val plan = PlanBuilder.build(3, listOf(ex("back_squat"), ex("trap_bar_deadlift")), 10).workouts
        assertEquals(listOf("back_squat", "trap_bar_deadlift", "back_squat"), plan.map { it.exerciseIds.single() })
    }

    @Test
    fun allSelectedUsedWhenMoreThanDays() {
        val legs = listOf("back_squat", "trap_bar_deadlift", "lunge_db").map(::ex)
        val plan = PlanBuilder.build(2, legs, 10).workouts
        assertEquals(legs.map { it.id }.toSet(), plan.flatMap { it.exerciseIds }.toSet())
        assertTrue(plan.all { it.exerciseIds.isNotEmpty() })
    }

    @Test
    fun emptySelectionGivesEmptyWorkouts() {
        assertTrue(PlanBuilder.build(2, emptyList(), 10).workouts.all { it.exerciseIds.isEmpty() })
    }

    private val sixGroups = listOf("back_squat", "lat_pulldown", "bench_press", "db_shoulder_press", "db_curl", "plank").map(::ex)

    @Test
    fun capacityLimitsWorkoutAndKeepsBigGroups() {
        val plan = PlanBuilder.build(3, sixGroups, 4).workouts
        plan.forEach { w ->
            assertEquals(4, w.exerciseIds.size)
            assertTrue(w.exerciseIds.containsAll(listOf("back_squat", "lat_pulldown", "bench_press")))
        }
        // Manje skupine se izmjenjuju, pa se kroz tjedan pojave sve.
        assertEquals(sixGroups.map { it.id }.toSet(), plan.flatMap { it.exerciseIds }.toSet())
    }

    @Test
    fun omittedReported() {
        val result = PlanBuilder.build(1, sixGroups, 3)
        assertEquals(3, result.workouts.single().exerciseIds.size)
        assertEquals(3, result.omitted.size)
    }

    @Test
    fun plannedWorkoutsFitDuration() {
        for (goal in Goal.entries) for (minutes in DURATIONS) {
            val cap = Timing.capacity(minutes, goal)
            val plan = PlanBuilder.build(3, ExerciseLibrary.all, cap).workouts
            plan.forEach { w ->
                val est = Timing.workoutMinutes(w.exerciseIds.map(::ex), goal)
                assertTrue(est <= minutes, "$goal $minutes min: procjena $est min")
            }
        }
    }

    @Test
    fun capacityValues() {
        assertEquals(5, Timing.capacity(60, Goal.STRENGTH))
        assertEquals(7, Timing.capacity(60, Goal.HYPERTROPHY))
    }
}

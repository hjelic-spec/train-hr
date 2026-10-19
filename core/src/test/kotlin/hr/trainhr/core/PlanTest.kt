package hr.trainhr.core

import hr.trainhr.core.model.BodyRegion
import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.Focus
import hr.trainhr.core.model.PlanSettings
import hr.trainhr.core.model.Sport
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.plan.PlanGenerator
import hr.trainhr.core.plan.PlanValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanTest {
    private val catalog = ExerciseCatalog()

    @Test
    fun generatedPlansHitMajorGroupsTwicePerWeek() {
        for (sport in Sport.entries) for (days in 2..4) for (minutes in listOf(30, 45, 60)) {
            val profile = UserProfile(sport = sport)
            val plan = PlanGenerator.generate(profile, PlanSettings(days, minutes, Focus.FULL_BODY), catalog)
            assertEquals(days, plan.workouts.size)
            val freq = PlanValidator.warnings(plan, profile, catalog).filter { "2×" in it }
            assertTrue(freq.isEmpty(), "$sport $days dana $minutes min: $freq")
        }
    }

    @Test
    fun noDuplicateExercisesWithinWorkout() {
        val plan = PlanGenerator.generate(UserProfile(sport = Sport.ROWING), PlanSettings(3, 60), catalog)
        for (w in plan.workouts) {
            assertEquals(w.items.size, w.items.map { it.exerciseId }.toSet().size)
        }
    }

    @Test
    fun limitationsExcludeStressingExercises() {
        val profile = UserProfile(limitations = setOf(BodyRegion.LOWER_BACK))
        val plan = PlanGenerator.generate(profile, PlanSettings(3, 60), catalog)
        val ids = plan.workouts.flatMap { it.items }.map { it.exerciseId }
        ids.forEach { assertTrue(BodyRegion.LOWER_BACK !in catalog[it]!!.stresses, it) }
    }

    @Test
    fun durationLimitsExerciseCount() {
        val plan = PlanGenerator.generate(UserProfile(), PlanSettings(3, 30), catalog)
        assertTrue(plan.workouts.all { it.items.size <= 4 })
    }

    @Test
    fun validatorFlagsEmptyPull() {
        val plan = PlanGenerator.generate(UserProfile(), PlanSettings(2, 45), catalog)
        val noPull = plan.copy(workouts = plan.workouts.map { w ->
            w.copy(items = w.items.filter { catalog[it.exerciseId]!!.pattern.name.endsWith("PULL").not() })
        })
        assertTrue(PlanValidator.warnings(noPull, UserProfile(), catalog).any { "povlačenja" in it })
    }
}

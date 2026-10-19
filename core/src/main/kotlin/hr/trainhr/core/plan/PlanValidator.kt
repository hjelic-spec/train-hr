package hr.trainhr.core.plan

import hr.trainhr.core.model.ExerciseCatalog
import hr.trainhr.core.model.MovementPattern
import hr.trainhr.core.model.MuscleGroup
import hr.trainhr.core.model.Sport
import hr.trainhr.core.model.TrainingPlan
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.model.WorkoutTemplate

/** Upozorenja za ručno složen ili mijenjan plan. Ne blokira – samo informira. */
object PlanValidator {

    private const val WARMUP_MIN = 8
    private const val WORK_SECONDS_PER_SET = 45

    private val majorGroups = listOf(
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
        MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
    )
    private val push = setOf(MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH)
    private val pull = setOf(MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL)

    fun estimatedMinutes(w: WorkoutTemplate, catalog: ExerciseCatalog): Int {
        val seconds = w.items.sumOf { p ->
            val work = if (catalog[p.exerciseId]?.timed == true) p.repMax else WORK_SECONDS_PER_SET
            p.sets * (work + p.restSeconds)
        }
        return WARMUP_MIN + (seconds + 59) / 60
    }

    fun warnings(plan: TrainingPlan, profile: UserProfile, catalog: ExerciseCatalog): List<String> {
        val out = mutableListOf<String>()
        if (plan.workouts.isEmpty()) return listOf("Plan nema nijedan trening.")

        val musclesPerDay = plan.workouts.map { w ->
            w.items.flatMap { catalog[it.exerciseId]?.muscles.orEmpty() }.toSet()
        }
        for (g in majorGroups) {
            val days = musclesPerDay.count { g in it }
            if (days < 2) {
                out += "${g.label}: trenira se ${days}× tjedno – preporuka je najmanje 2× (ACSM 2026)."
            }
        }

        val sets = plan.workouts.flatMap { it.items }.mapNotNull { p ->
            catalog[p.exerciseId]?.let { it.pattern to p.sets }
        }
        val pushSets = sets.filter { it.first in push }.sumOf { it.second }
        val pullSets = sets.filter { it.first in pull }.sumOf { it.second }
        if (pushSets == 0) out += "Nema vježbi potiska."
        if (pullSets == 0) out += "Nema vježbi povlačenja."
        if (profile.sport == Sport.ROWING && pullSets > pushSets * 1.5 && pushSets > 0) {
            out += "Veslanje je već pretežno povlačenje – razmislite o više potiska radi ravnoteže (orijentacijsko pravilo)."
        }

        for (w in plan.workouts) {
            for (p in w.items) {
                val ex = catalog[p.exerciseId]
                if (ex == null) {
                    out += "${w.name}: nepoznata vježba (${p.exerciseId})."
                    continue
                }
                val risky = ex.stresses.intersect(profile.limitations)
                if (risky.isNotEmpty()) {
                    out += "${w.name}: „${ex.name}“ opterećuje ${risky.joinToString { it.label.lowercase() }} – navedeno u ograničenjima."
                }
                if (!profile.equipment.available.containsAll(ex.equipment)) {
                    out += "${w.name}: „${ex.name}“ traži opremu koja nije označena kao dostupna."
                }
            }
            val minutes = estimatedMinutes(w, catalog)
            if (minutes > plan.settings.durationMinutes * 1.15) {
                out += "${w.name}: procjena trajanja ~$minutes min, a cilj je ${plan.settings.durationMinutes} min."
            }
        }
        return out
    }
}

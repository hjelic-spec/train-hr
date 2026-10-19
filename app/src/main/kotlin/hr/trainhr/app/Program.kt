package hr.trainhr.app

import hr.trainhr.core.Advisor
import hr.trainhr.core.Catalog
import hr.trainhr.core.DAY_OFFSETS
import hr.trainhr.core.Recommendation
import hr.trainhr.core.SetEntry
import hr.trainhr.core.Timing
import hr.trainhr.core.WeekPlan
import hr.trainhr.core.WeekPlanner
import hr.trainhr.core.estimateKcal
import hr.trainhr.core.setVolume
import java.time.LocalDate

fun today(): Long = LocalDate.now().toEpochDay()

val AppData.restSets: Int get() = restSetsSec ?: goal.restBetweenSetsSec
val AppData.restExercises: Int get() = restExercisesSec ?: goal.restBetweenExercisesSec
val AppData.start: Long get() = startDay ?: today()
val AppData.capacity: Int get() = Timing.capacity(durationMinutes, goal, restSets, restExercises)
fun AppData.catalog() = Catalog(customExercises)

fun AppData.weekOf(day: Long): Int = Math.floorDiv(day - start, 7L).toInt()
fun AppData.dayDate(week: Int, k: Int): Long = start + 7L * week + DAY_OFFSETS.getValue(daysPerWeek)[k]
fun AppData.inProgram(week: Int, k: Int): Boolean = endDay == null || dayDate(week, k) <= endDay
/** Trening je vezan uz planirani datum, pa promjena početka ili broja dana ne miješa stare treninge. */
fun AppData.sessionFor(week: Int, k: Int): Session? { val d = dayDate(week, k); return sessions.find { it.plannedDay == d } }
fun AppData.lastWeek(): Int? = endDay?.let { weekOf(it) }
fun AppData.programWeeks(): Int? = lastWeek()?.plus(1)

fun AppData.planForWeek(week: Int): WeekPlan {
    val c = catalog()
    val seed = WeekPlanner.seed(start, week, salts[week] ?: 0, daysPerWeek, durationMinutes)
    return WeekPlanner.plan(selected.mapNotNull { c[it] }, daysPerWeek, capacity, seed, pinned)
}

data class Slot(val week: Int, val k: Int)

/** Sljedeći trening: današnji ako nije odrađen, inače prvi sljedeći neodrađeni u programu. */
fun AppData.nextTarget(t: Long = today()): Slot? {
    val cur = maxOf(0, weekOf(t))
    for (w in cur until cur + 2) for (k in 0 until daysPerWeek) {
        if (sessionFor(w, k) == null && dayDate(w, k) >= t && inProgram(w, k)) return Slot(w, k)
    }
    return null
}

/** Povijest serija vježbe po treninzima, kronološki. */
fun AppData.history(exerciseId: String): List<Pair<Long, List<SetEntry>>> =
    sessions.sortedWith(compareBy<Session> { it.epochDay }.thenBy { it.id }).mapNotNull { s ->
        s.entries.firstOrNull { it.exerciseId == exerciseId }?.takeIf { it.sets.isNotEmpty() }?.let { s.epochDay to it.sets }
    }

fun AppData.recommendation(exerciseId: String): Recommendation? {
    val ex = catalog()[exerciseId] ?: return null
    return Advisor.recommend(ex, goal, history(exerciseId).map { it.second })
}

data class Stats(val kg: Int, val sets: Int, val minutes: Int, val kcal: Int) {
    operator fun plus(o: Stats) = Stats(kg + o.kg, sets + o.sets, minutes + o.minutes, kcal + o.kcal)
    companion object { val ZERO = Stats(0, 0, 0, 0) }
}

fun AppData.stats(s: Session): Stats {
    val c = catalog()
    var kg = 0.0
    var sets = 0
    s.entries.forEach { e -> val ex = c[e.exerciseId]; e.sets.forEach { kg += setVolume(ex, it); sets++ } }
    val sec = s.durationSeconds ?: 0
    return Stats(Math.round(kg).toInt(), sets, Math.round(sec / 60.0).toInt(), estimateKcal(bodyWeightKg, sec))
}

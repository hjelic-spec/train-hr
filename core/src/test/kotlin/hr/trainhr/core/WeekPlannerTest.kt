package hr.trainhr.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeekPlannerTest {
    private val catalog = Catalog(emptyList())
    private fun ex(id: String) = catalog[id]!!
    private val selected = listOf(
        "trap_bar_deadlift", "back_squat", "rdl_db", "lat_pulldown", "cable_row", "db_row", "bench_press",
        "db_bench_press", "db_shoulder_press", "lateral_raise", "db_curl", "triceps_pushdown", "plank", "pallof_press",
    ).map(::ex)
    private val pins = listOf("trap_bar_deadlift", "bench_press")

    /** Isti ulaz → isti plan kao u web pregledu (fixture generiran iz web/index.html). */
    @Test
    fun matchesWebPreview() {
        val root = Json.parseToJsonElement(javaClass.getResource("/web_fixtures.json")!!.readText()).jsonObject
        val r = Mulberry32(12345)
        root["nums"]!!.jsonArray.forEach { assertEquals(it.jsonPrimitive.double, r.next(), 1e-15) }
        root["cases"]!!.jsonArray.map { it.jsonObject }.forEach { c: JsonObject ->
            val seed = WeekPlanner.seed(c["sd"]!!.jsonPrimitive.long, c["w"]!!.jsonPrimitive.int, c["salt"]!!.jsonPrimitive.int,
                c["days"]!!.jsonPrimitive.int, c["min"]!!.jsonPrimitive.int)
            assertEquals(c["seed"]!!.jsonPrimitive.int, seed)
            val plan = WeekPlanner.plan(selected, c["days"]!!.jsonPrimitive.int, c["cap"]!!.jsonPrimitive.int, seed,
                c["pins"]!!.jsonArray.map { it.jsonPrimitive.content })
            val expected = c["plan"]!!.jsonArray.map { d -> d.jsonArray.map { it.jsonPrimitive.content } }
            assertEquals(expected, plan.days)
            assertEquals(c["omitted"]!!.jsonArray.map { it.jsonPrimitive.content }, plan.omitted.map { it.id })
        }
    }

    @Test
    fun rulesHoldForManyWeeks() {
        for (goal in Goal.entries) for (minutes in DURATIONS) for (days in 1..5) for (seed in 1..40) {
            val cap = Timing.capacity(minutes, goal)
            val p = WeekPlanner.plan(selected, days, cap, seed * 7919, pins)
            assertEquals(days, p.days.size)
            p.days.forEach { d ->
                assertTrue(d.size <= cap, "kapacitet")
                assertEquals(d.size, d.toSet().size, "ponavljanje u treningu")
                if (d.size > 1) assertTrue(Timing.workoutMinutes(d.map(::ex), goal) <= minutes, "$goal $minutes min")
                val tiers = d.map { GROUP_TIER.getValue(ex(it).group) }
                assertEquals(tiers.sorted(), tiers, "redoslijed")
            }
            if (cap >= 2) pins.forEach { pin -> assertEquals(pinnedPerWeek(days), p.days.count { pin in it }, "stalna $pin") }
        }
    }

    @Test
    fun allExercisesRotateIn() {
        val cap = Timing.capacity(60, Goal.HYPERTROPHY)
        val seen = (0 until 3).flatMap { w -> WeekPlanner.plan(selected, 3, cap, 1234 + w * 7919).days.flatten() }.toSet()
        assertEquals(selected.map { it.id }.toSet(), seen)
    }

    @Test
    fun capacityValues() {
        assertEquals(5, Timing.capacity(60, Goal.STRENGTH))
        assertEquals(7, Timing.capacity(60, Goal.HYPERTROPHY))
        assertTrue(Timing.capacity(60, Goal.STRENGTH, 120, 90) > Timing.capacity(60, Goal.STRENGTH))
    }

    @Test
    fun volumeAndKcal() {
        assertEquals(700.0, listOf(12, 12, 11).sumOf { setVolume(ex("db_curl"), SetEntry(10.0, it)) })
        assertEquals(600.0, setVolume(ex("back_squat"), SetEntry(60.0, 10)))
        assertEquals(0.0, setVolume(ex("plank"), SetEntry(null, 40)))
        assertEquals(266, estimateKcal(80.0, 57 * 60))
    }
}

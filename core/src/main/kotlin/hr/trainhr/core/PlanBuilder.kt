package hr.trainhr.core

data class PlanResult(
    val workouts: List<Workout>,
    /** Odabrane vježbe koje nisu stale ni u jedan trening. */
    val omitted: List<Exercise>,
)

/**
 * Raspoređuje odabrane vježbe po treninzima u tjednu, najviše [capacity] vježbi po treningu.
 * Svaka mišićna skupina s barem jednom vježbom dolazi u svaki trening ako ima mjesta; ako nema,
 * prednost imaju velike skupine (noge, leđa, prsa), a manje (ramena, ruke, trup) se izmjenjuju
 * po danima.
 */
object PlanBuilder {

    private val bigGroups = setOf(MuscleGroup.LEGS, MuscleGroup.BACK, MuscleGroup.CHEST)

    fun build(daysPerWeek: Int, selected: List<Exercise>, capacity: Int): PlanResult {
        val days = daysPerWeek.coerceIn(1, 6)
        val cap = capacity.coerceAtLeast(1)
        val byGroup = MuscleGroup.entries.associateWith { g -> selected.filter { it.group == g } }

        val workouts = List(days) { d ->
            val wanted = MuscleGroup.entries.flatMap { g ->
                val list = byGroup.getValue(g)
                when {
                    list.isEmpty() -> emptyList()
                    list.size <= days -> listOf(list[d % list.size])
                    else -> list.filterIndexed { i, _ -> i % days == d }
                }
            }
            val chosen = if (wanted.size <= cap) {
                wanted
            } else {
                val big = wanted.filter { it.group in bigGroups }
                if (big.size >= cap) {
                    big.take(cap)
                } else {
                    val small = wanted.filter { it.group !in bigGroups }
                    val free = cap - big.size
                    val rotated = List(small.size) { small[(it + d * free) % small.size] }
                    val keep = rotated.take(free).toSet()
                    wanted.filter { it.group in bigGroups || it in keep }
                }
            }
            Workout("Trening ${'A' + d}", chosen.map { it.id })
        }
        val used = workouts.flatMap { it.exerciseIds }.toSet()
        return PlanResult(workouts, selected.filter { it.id !in used })
    }
}

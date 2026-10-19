package hr.trainhr.core

/**
 * Raspoređuje odabrane vježbe po treninzima u tjednu.
 * Sve odabrane vježbe se koriste; svaka mišićna skupina s barem jednom vježbom
 * dolazi u svakom treningu (ako ima manje vježbi nego treninga, vježbe se izmjenjuju).
 */
object PlanBuilder {

    fun build(daysPerWeek: Int, selected: List<Exercise>): List<Workout> {
        val days = daysPerWeek.coerceIn(1, 6)
        val byGroup = MuscleGroup.entries.associateWith { g -> selected.filter { it.group == g } }
        return List(days) { d ->
            val ids = MuscleGroup.entries.flatMap { g ->
                val list = byGroup.getValue(g)
                when {
                    list.isEmpty() -> emptyList()
                    list.size <= days -> listOf(list[d % list.size])
                    else -> list.filterIndexed { i, _ -> i % days == d }
                }
            }.map { it.id }
            Workout("Trening ${'A' + d}", ids)
        }
    }
}

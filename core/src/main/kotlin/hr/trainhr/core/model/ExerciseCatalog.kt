package hr.trainhr.core.model

/** Ugrađene vježbe + vježbe koje je korisnik sam dodao. */
class ExerciseCatalog(custom: List<Exercise> = emptyList()) {
    val all: List<Exercise> = ExerciseLibrary.all + custom.map { it.copy(custom = true) }
    private val byId = all.associateBy { it.id }

    operator fun get(id: String): Exercise? = byId[id]

    fun available(equipment: Set<Equipment>): List<Exercise> =
        all.filter { equipment.containsAll(it.equipment) }
}

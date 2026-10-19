package hr.trainhr.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val pattern: MovementPattern,
    val muscles: Set<MuscleGroup>,
    /** Sva oprema potrebna za izvođenje. */
    val equipment: Set<Equipment>,
    val loadType: LoadType,
    val compound: Boolean,
    /** Sportovi za koje je vježba posebno korisna. */
    val sports: Set<Sport> = emptySet(),
    /** Dijelovi tijela koje vježba značajnije opterećuje. */
    val stresses: Set<BodyRegion> = emptySet(),
    /** Vježba se zadaje u sekundama (plank, nošenje, ergometar) umjesto ponavljanja. */
    val timed: Boolean = false,
    val cue: String = "",
    /** Korisnik ju je sam dodao. */
    val custom: Boolean = false,
)

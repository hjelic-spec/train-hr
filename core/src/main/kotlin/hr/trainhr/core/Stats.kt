package hr.trainhr.core

/** Vježbe s bučicom u svakoj ruci (jednoručno veslanje: obje strane) – podignuto je dvostruko od upisane težine. */
val TWO_DUMBBELLS = setOf(
    "lunge_db", "step_up_db", "rdl_db", "db_row", "db_bench_press", "incline_db_press",
    "db_shoulder_press", "lateral_raise", "db_curl", "hammer_curl",
)

fun loadFactor(ex: Exercise): Int =
    if (ex.kind == LoadKind.DUMBBELL && (ex.pair ?: (ex.id in TWO_DUMBBELLS))) 2 else 1

/** Podignuti kilogrami: težina × ponavljanja × broj bučica (bez vježbi na vrijeme i bez vlastite težine). */
fun setVolume(ex: Exercise?, set: SetEntry): Double =
    if (ex != null && !ex.timed && set.weightKg != null) set.weightKg * set.reps * loadFactor(ex) else 0.0

/** 2024 Adult Compendium of Physical Activities, šifra 02054: trening s otporom, više vježbi, 8–15 pon. */
const val RESISTANCE_MET = 3.5

/** Procjena potrošnje: MET × tjelesna masa × sati (1 MET ≈ 1 kcal/kg/h). Gruba procjena. */
fun estimateKcal(bodyWeightKg: Double, durationSeconds: Int): Int =
    Math.round(RESISTANCE_MET * bodyWeightKg * durationSeconds / 3600.0).toInt()

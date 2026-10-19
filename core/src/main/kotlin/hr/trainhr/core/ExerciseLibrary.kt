package hr.trainhr.core

import hr.trainhr.core.LoadKind.BARBELL
import hr.trainhr.core.LoadKind.BODYWEIGHT
import hr.trainhr.core.LoadKind.DUMBBELL
import hr.trainhr.core.LoadKind.MACHINE
import hr.trainhr.core.MuscleGroup.ARMS
import hr.trainhr.core.MuscleGroup.BACK
import hr.trainhr.core.MuscleGroup.CHEST
import hr.trainhr.core.MuscleGroup.CORE
import hr.trainhr.core.MuscleGroup.LEGS
import hr.trainhr.core.MuscleGroup.SHOULDERS

object ExerciseLibrary {
    val all: List<Exercise> = listOf(
        Exercise("back_squat", "Čučanj – šipka", LEGS, BARBELL),
        Exercise("trap_bar_deadlift", "Mrtvo dizanje – trap bar", LEGS, BARBELL),
        Exercise("rdl_barbell", "Rumunjsko mrtvo dizanje – šipka", LEGS, BARBELL),
        Exercise("goblet_squat", "Goblet čučanj – bučica", LEGS, DUMBBELL),
        Exercise("lunge_db", "Iskorak – bučice", LEGS, DUMBBELL),
        Exercise("step_up_db", "Penjanje na klupu – bučice", LEGS, DUMBBELL),
        Exercise("rdl_db", "Rumunjsko mrtvo dizanje – bučice", LEGS, DUMBBELL),
        Exercise("calf_raise_db", "Podizanje na prste – bučica", LEGS, DUMBBELL),

        Exercise("lat_pulldown", "Lat mašina – povlačenje ispred", BACK, MACHINE),
        Exercise("lat_pulldown_close", "Lat mašina – uski hvat", BACK, MACHINE),
        Exercise("cable_row", "Veslanje na sajli – sjedeće", BACK, MACHINE),
        Exercise("db_row", "Jednoručno veslanje – bučica", BACK, DUMBBELL),
        Exercise("barbell_row", "Pretklonjeno veslanje – šipka", BACK, BARBELL),

        Exercise("bench_press", "Potisak s klupe – šipka", CHEST, BARBELL),
        Exercise("db_bench_press", "Potisak s klupe – bučice", CHEST, DUMBBELL),
        Exercise("incline_db_press", "Kosi potisak – bučice", CHEST, DUMBBELL),
        Exercise("push_up", "Sklek", CHEST, BODYWEIGHT),

        Exercise("db_shoulder_press", "Potisak bučica iznad glave", SHOULDERS, DUMBBELL),
        Exercise("barbell_ohp", "Potisak šipke iznad glave", SHOULDERS, BARBELL),
        Exercise("lateral_raise", "Odručenje u stranu – bučice", SHOULDERS, DUMBBELL),
        Exercise("face_pull", "Face pull – sajla", SHOULDERS, MACHINE),

        Exercise("db_curl", "Pregib bučicama – biceps", ARMS, DUMBBELL),
        Exercise("hammer_curl", "Čekić pregib – bučice", ARMS, DUMBBELL),
        Exercise("barbell_curl", "Pregib šipkom – biceps", ARMS, BARBELL),
        Exercise("triceps_pushdown", "Triceps potisak – sajla", ARMS, MACHINE),
        Exercise("db_triceps_ext", "Francuski potisak – bučica", ARMS, DUMBBELL),

        Exercise("plank", "Prednji upor (plank)", CORE, BODYWEIGHT, timed = true),
        Exercise("side_plank", "Bočni upor", CORE, BODYWEIGHT, timed = true),
        Exercise("pallof_press", "Pallof potisak – sajla", CORE, MACHINE),
        Exercise("dead_bug", "Dead bug", CORE, BODYWEIGHT),
        Exercise("farmer_carry", "Farmerovo nošenje – bučice", CORE, DUMBBELL, timed = true),
    )
}

/** Ugrađene vježbe + vlastite vježbe korisnika. */
class Catalog(custom: List<Exercise>) {
    val all: List<Exercise> = ExerciseLibrary.all + custom
    private val byId = all.associateBy { it.id }
    operator fun get(id: String): Exercise? = byId[id]
}

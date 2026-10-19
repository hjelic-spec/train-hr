package hr.trainhr.core.model

import hr.trainhr.core.model.BodyRegion.ELBOW_WRIST
import hr.trainhr.core.model.BodyRegion.HIP
import hr.trainhr.core.model.BodyRegion.KNEE
import hr.trainhr.core.model.BodyRegion.LOWER_BACK
import hr.trainhr.core.model.BodyRegion.SHOULDER
import hr.trainhr.core.model.Equipment.BARBELL
import hr.trainhr.core.model.Equipment.BENCH
import hr.trainhr.core.model.Equipment.BIKE_ERG
import hr.trainhr.core.model.Equipment.CABLE_ROW
import hr.trainhr.core.model.Equipment.DUMBBELL
import hr.trainhr.core.model.Equipment.LAT_MACHINE
import hr.trainhr.core.model.Equipment.ROW_ERG
import hr.trainhr.core.model.Equipment.SKI_ERG
import hr.trainhr.core.model.Equipment.SQUAT_RACK
import hr.trainhr.core.model.Equipment.TRAP_BAR
import hr.trainhr.core.model.MovementPattern.ARMS
import hr.trainhr.core.model.MovementPattern.BALANCE
import hr.trainhr.core.model.MovementPattern.CALF
import hr.trainhr.core.model.MovementPattern.CARRY
import hr.trainhr.core.model.MovementPattern.CONDITIONING
import hr.trainhr.core.model.MovementPattern.CORE
import hr.trainhr.core.model.MovementPattern.HINGE
import hr.trainhr.core.model.MovementPattern.HORIZONTAL_PULL
import hr.trainhr.core.model.MovementPattern.HORIZONTAL_PUSH
import hr.trainhr.core.model.MovementPattern.SHOULDERS
import hr.trainhr.core.model.MovementPattern.SINGLE_LEG
import hr.trainhr.core.model.MovementPattern.SQUAT
import hr.trainhr.core.model.MovementPattern.VERTICAL_PULL
import hr.trainhr.core.model.MovementPattern.VERTICAL_PUSH
import hr.trainhr.core.model.MuscleGroup.BACK
import hr.trainhr.core.model.MuscleGroup.BICEPS
import hr.trainhr.core.model.MuscleGroup.CALVES
import hr.trainhr.core.model.MuscleGroup.CHEST
import hr.trainhr.core.model.MuscleGroup.GLUTES
import hr.trainhr.core.model.MuscleGroup.GRIP
import hr.trainhr.core.model.MuscleGroup.HAMSTRINGS
import hr.trainhr.core.model.MuscleGroup.QUADS
import hr.trainhr.core.model.MuscleGroup.TRICEPS
import hr.trainhr.core.model.Sport.CYCLING
import hr.trainhr.core.model.Sport.ROWING
import hr.trainhr.core.model.Sport.RUNNING
import hr.trainhr.core.model.MuscleGroup.CORE as CORE_M
import hr.trainhr.core.model.MuscleGroup.SHOULDERS as SHOULDERS_M

/** Ugrađena biblioteka vježbi, složena oko opreme dostupne u teretani. */
object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        // Pregib u kuku
        Exercise(
            "trap_bar_deadlift", "Mrtvo dizanje – trap bar", HINGE,
            setOf(GLUTES, HAMSTRINGS, QUADS, BACK, GRIP), setOf(TRAP_BAR), LoadType.TRAP_BAR, true,
            sports = setOf(ROWING, CYCLING), stresses = setOf(LOWER_BACK),
            cue = "Neutralna leđa, gurni pod nogama, ustani zajedno kukovima i ramenima.",
        ),
        Exercise(
            "rdl_barbell", "Rumunjsko mrtvo dizanje – šipka", HINGE,
            setOf(HAMSTRINGS, GLUTES, BACK), setOf(BARBELL), LoadType.BARBELL, true,
            sports = setOf(ROWING), stresses = setOf(LOWER_BACK),
            cue = "Koljena blago savijena, kukovi idu unatrag, šipka klizi uz bedra.",
        ),
        Exercise(
            "rdl_dumbbell", "Rumunjsko mrtvo dizanje – bučice", HINGE,
            setOf(HAMSTRINGS, GLUTES, BACK), setOf(DUMBBELL), LoadType.DUMBBELL, true,
            sports = setOf(ROWING, RUNNING), stresses = setOf(LOWER_BACK),
        ),
        Exercise(
            "hip_thrust_barbell", "Potisak kukom – šipka uz klupu", HINGE,
            setOf(GLUTES, HAMSTRINGS), setOf(BARBELL, BENCH), LoadType.BARBELL, true,
            sports = setOf(ROWING, RUNNING, CYCLING), stresses = setOf(HIP),
            cue = "Lopatice na klupi, brada prema prsima, stisni stražnjicu na vrhu.",
        ),
        // Čučanj
        Exercise(
            "back_squat", "Čučanj – šipka (stalak)", SQUAT,
            setOf(QUADS, GLUTES, BACK), setOf(BARBELL, SQUAT_RACK), LoadType.BARBELL, true,
            sports = setOf(ROWING, CYCLING), stresses = setOf(KNEE, LOWER_BACK),
            cue = "Stopala u širini ramena, koljena prate prste, dubina koju možeš kontrolirati.",
        ),
        Exercise(
            "goblet_squat", "Goblet čučanj – bučica", SQUAT,
            setOf(QUADS, GLUTES, CORE_M), setOf(DUMBBELL), LoadType.DUMBBELL, true,
            sports = setOf(CYCLING), stresses = setOf(KNEE),
            cue = "Bučica uz prsa, laktovi između koljena, uspravan trup.",
        ),
        Exercise(
            "box_squat_bench", "Čučanj do klupe – bučice", SQUAT,
            setOf(QUADS, GLUTES), setOf(DUMBBELL, BENCH), LoadType.DUMBBELL, true,
            stresses = setOf(KNEE),
            cue = "Lagano dotakni klupu i ustani bez odmaranja.",
        ),
        // Jednonožno
        Exercise(
            "split_squat_db", "Split čučanj – bučice", SINGLE_LEG,
            setOf(QUADS, GLUTES), setOf(DUMBBELL), LoadType.DUMBBELL, true,
            sports = setOf(RUNNING, CYCLING), stresses = setOf(KNEE),
        ),
        Exercise(
            "step_up_db", "Penjanje na klupu – bučice", SINGLE_LEG,
            setOf(QUADS, GLUTES), setOf(DUMBBELL, BENCH), LoadType.DUMBBELL, true,
            sports = setOf(RUNNING, CYCLING), stresses = setOf(KNEE),
            cue = "Cijelo stopalo na klupi, gurni prednjom nogom, kontrolirano spuštanje.",
        ),
        Exercise(
            "reverse_lunge_db", "Iskorak unatrag – bučice", SINGLE_LEG,
            setOf(QUADS, GLUTES), setOf(DUMBBELL), LoadType.DUMBBELL, true,
            sports = setOf(RUNNING), stresses = setOf(KNEE),
        ),
        Exercise(
            "single_leg_rdl_db", "Jednonožno rumunjsko – bučica", SINGLE_LEG,
            setOf(HAMSTRINGS, GLUTES, CORE_M), setOf(DUMBBELL), LoadType.DUMBBELL, true,
            sports = setOf(RUNNING), stresses = setOf(LOWER_BACK),
            cue = "Po potrebi se drži slobodnom rukom za oslonac.",
        ),
        Exercise(
            "calf_raise_db", "Podizanje na prste – bučica", CALF,
            setOf(CALVES), setOf(DUMBBELL), LoadType.DUMBBELL, false,
            sports = setOf(RUNNING),
        ),
        // Potisak vodoravno
        Exercise(
            "bench_press", "Potisak s klupe – šipka", HORIZONTAL_PUSH,
            setOf(CHEST, TRICEPS, SHOULDERS_M), setOf(BARBELL, BENCH), LoadType.BARBELL, true,
            sports = setOf(ROWING), stresses = setOf(SHOULDER),
            cue = "Lopatice skupljene, šipka do donjeg dijela prsa. Uz stalak ili pomagača.",
        ),
        Exercise(
            "db_bench_press", "Potisak s klupe – bučice", HORIZONTAL_PUSH,
            setOf(CHEST, TRICEPS, SHOULDERS_M), setOf(DUMBBELL, BENCH), LoadType.DUMBBELL, true,
            sports = setOf(ROWING), stresses = setOf(SHOULDER),
        ),
        Exercise(
            "push_up", "Sklek (po potrebi rukama na klupi)", HORIZONTAL_PUSH,
            setOf(CHEST, TRICEPS, SHOULDERS_M, CORE_M), emptySet(), LoadType.NONE, true,
            sports = setOf(ROWING, CYCLING), stresses = setOf(SHOULDER, ELBOW_WRIST),
        ),
        // Potisak okomito
        Exercise(
            "db_shoulder_press", "Potisak bučica iznad glave – sjedeći", VERTICAL_PUSH,
            setOf(SHOULDERS_M, TRICEPS), setOf(DUMBBELL, BENCH), LoadType.DUMBBELL, true,
            stresses = setOf(SHOULDER),
            cue = "Leđa uz naslon, laktovi malo ispred tijela.",
        ),
        Exercise(
            "barbell_ohp", "Potisak šipke iznad glave – stojeći", VERTICAL_PUSH,
            setOf(SHOULDERS_M, TRICEPS, CORE_M), setOf(BARBELL, SQUAT_RACK), LoadType.BARBELL, true,
            stresses = setOf(SHOULDER, LOWER_BACK),
        ),
        // Povlačenje vodoravno
        Exercise(
            "cable_row", "Veslanje na sajli – sjedeće", HORIZONTAL_PULL,
            setOf(BACK, BICEPS), setOf(CABLE_ROW), LoadType.CABLE_STACK, true,
            sports = setOf(ROWING, CYCLING),
            cue = "Uspravan trup, povuci laktove uz tijelo, lopatice prema natrag.",
        ),
        Exercise(
            "db_row", "Jednoručno veslanje – bučica uz klupu", HORIZONTAL_PULL,
            setOf(BACK, BICEPS, GRIP), setOf(DUMBBELL, BENCH), LoadType.DUMBBELL, true,
            sports = setOf(ROWING),
        ),
        Exercise(
            "barbell_row", "Pretklonjeno veslanje – šipka", HORIZONTAL_PULL,
            setOf(BACK, BICEPS, HAMSTRINGS), setOf(BARBELL), LoadType.BARBELL, true,
            stresses = setOf(LOWER_BACK),
        ),
        // Povlačenje okomito
        Exercise(
            "lat_pulldown", "Lat mašina – povlačenje ispred", VERTICAL_PULL,
            setOf(BACK, BICEPS), setOf(LAT_MACHINE), LoadType.LAT_STACK, true,
            sports = setOf(ROWING),
            cue = "Povuci šipku do gornjeg dijela prsa, bez zabacivanja trupa.",
        ),
        Exercise(
            "lat_pulldown_close", "Lat mašina – uski hvat", VERTICAL_PULL,
            setOf(BACK, BICEPS), setOf(LAT_MACHINE), LoadType.LAT_STACK, true,
        ),
        // Ruke i ramena
        Exercise(
            "db_curl", "Pregib bučicama – biceps", ARMS,
            setOf(BICEPS, GRIP), setOf(DUMBBELL), LoadType.DUMBBELL, false,
            stresses = setOf(ELBOW_WRIST),
            cue = "Laktovi uz tijelo, bez njihanja trupom. Težina je po bučici.",
        ),
        Exercise(
            "hammer_curl", "Čekić pregib – bučice", ARMS,
            setOf(BICEPS, GRIP), setOf(DUMBBELL), LoadType.DUMBBELL, false,
            sports = setOf(ROWING),
        ),
        Exercise(
            "triceps_pushdown", "Triceps potisak na lat mašini", ARMS,
            setOf(TRICEPS), setOf(LAT_MACHINE), LoadType.LAT_STACK, false,
            stresses = setOf(ELBOW_WRIST),
        ),
        Exercise(
            "lateral_raise", "Odručenje u stranu – bučice", SHOULDERS,
            setOf(SHOULDERS_M), setOf(DUMBBELL), LoadType.DUMBBELL, false,
            stresses = setOf(SHOULDER),
            cue = "Najlakša bučica je 8 kg – ako je preteško, zamijeni vježbu.",
        ),
        Exercise(
            "face_pull", "Face pull – lat mašina s užetom", SHOULDERS,
            setOf(SHOULDERS_M, BACK), setOf(LAT_MACHINE), LoadType.LAT_STACK, false,
            sports = setOf(ROWING, CYCLING),
        ),
        // Nošenje
        Exercise(
            "farmer_carry", "Farmerovo nošenje – bučice", CARRY,
            setOf(GRIP, CORE_M, SHOULDERS_M), setOf(DUMBBELL), LoadType.DUMBBELL, true,
            sports = setOf(ROWING, CYCLING), timed = true,
        ),
        Exercise(
            "trap_bar_carry", "Nošenje trap bara", CARRY,
            setOf(GRIP, CORE_M, GLUTES), setOf(TRAP_BAR), LoadType.TRAP_BAR, true,
            sports = setOf(ROWING), timed = true, stresses = setOf(LOWER_BACK),
        ),
        Exercise(
            "suitcase_carry", "Nošenje jedne bučice (kovčeg)", CARRY,
            setOf(CORE_M, GRIP), setOf(DUMBBELL), LoadType.DUMBBELL, false,
            sports = setOf(ROWING, RUNNING), timed = true,
        ),
        // Trup
        Exercise(
            "pallof_press", "Pallof potisak – sajla", CORE,
            setOf(CORE_M), setOf(CABLE_ROW), LoadType.CABLE_STACK, false,
            sports = setOf(ROWING, RUNNING),
            cue = "Stani bočno od sajle, potisni ruke naprijed i ne dopusti rotaciju.",
        ),
        Exercise(
            "plank", "Prednji upor (plank)", CORE,
            setOf(CORE_M), emptySet(), LoadType.NONE, false, timed = true,
            stresses = setOf(SHOULDER),
        ),
        Exercise(
            "side_plank", "Bočni upor", CORE,
            setOf(CORE_M), emptySet(), LoadType.NONE, false,
            sports = setOf(ROWING, RUNNING), timed = true,
        ),
        Exercise(
            "dead_bug", "Dead bug", CORE,
            setOf(CORE_M), emptySet(), LoadType.NONE, false,
            cue = "Donji dio leđa pritisnut u pod, polagano.",
        ),
        // Ravnoteža i kondicija
        Exercise(
            "single_leg_stand", "Stajanje na jednoj nozi", BALANCE,
            setOf(CORE_M, CALVES), emptySet(), LoadType.NONE, false, timed = true,
        ),
        Exercise(
            "row_erg", "RowErg – veslanje", CONDITIONING,
            setOf(BACK, QUADS, GLUTES), setOf(ROW_ERG), LoadType.NONE, false,
            sports = setOf(ROWING), timed = true,
        ),
        Exercise(
            "bike_erg", "BikeErg – vožnja", CONDITIONING,
            setOf(QUADS, GLUTES), setOf(BIKE_ERG), LoadType.NONE, false,
            sports = setOf(CYCLING), timed = true,
        ),
        Exercise(
            "ski_erg", "SkiErg", CONDITIONING,
            setOf(BACK, TRICEPS, CORE_M), setOf(SKI_ERG), LoadType.NONE, false,
            sports = setOf(ROWING), timed = true,
        ),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): Exercise? = byId[id]

    fun require(id: String): Exercise = byId[id] ?: error("Nepoznata vježba: $id")
}

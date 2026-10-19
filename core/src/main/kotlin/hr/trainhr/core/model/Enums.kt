package hr.trainhr.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Equipment(val label: String) {
    DUMBBELL("Bučice"),
    BARBELL("Šipka s pločama"),
    TRAP_BAR("Trap bar"),
    LAT_MACHINE("Lat mašina"),
    CABLE_ROW("Sajla – sjedeće veslanje"),
    BENCH("Klupa"),
    SQUAT_RACK("Stalak za čučanj"),
    ROW_ERG("Concept2 RowErg"),
    BIKE_ERG("Concept2 BikeErg"),
    SKI_ERG("Concept2 SkiErg"),
}

@Serializable
enum class MovementPattern(val label: String) {
    SQUAT("Čučanj"),
    HINGE("Pregib u kuku"),
    SINGLE_LEG("Jednonožno"),
    CALF("Listovi"),
    HORIZONTAL_PUSH("Potisak vodoravno"),
    VERTICAL_PUSH("Potisak okomito"),
    HORIZONTAL_PULL("Povlačenje vodoravno"),
    VERTICAL_PULL("Povlačenje okomito"),
    ARMS("Ruke"),
    SHOULDERS("Ramena"),
    CARRY("Nošenje"),
    CORE("Trup"),
    BALANCE("Ravnoteža"),
    CONDITIONING("Kondicija"),
}

@Serializable
enum class MuscleGroup(val label: String) {
    QUADS("Prednja loža"),
    HAMSTRINGS("Stražnja loža"),
    GLUTES("Stražnjica"),
    CALVES("Listovi"),
    CHEST("Prsa"),
    BACK("Leđa"),
    SHOULDERS("Ramena"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    CORE("Trup"),
    GRIP("Stisak"),
}

/** Način na koji se zadaje opterećenje – određuje zaokruživanje na stvarno dostupne težine. */
@Serializable
enum class LoadType {
    /** Težina jedne bučice (po ruci). */
    DUMBBELL,
    /** Ukupna težina šipke s pločama. */
    BARBELL,
    /** Ukupna težina trap bara s pločama. */
    TRAP_BAR,
    /** Slaganje utega na lat mašini. */
    LAT_STACK,
    /** Slaganje utega na sajli za veslanje. */
    CABLE_STACK,
    /** Vlastita težina / vrijeme – bez kilograma. */
    NONE,
}

@Serializable
enum class Sport(val label: String) {
    GENERAL("Opći program"),
    ROWING("Veslanje"),
    RUNNING("Trčanje"),
    CYCLING("Biciklizam"),
}

@Serializable
enum class Experience(val label: String) {
    BEGINNER("Početnik"),
    INTERMEDIATE("Srednje iskustvo"),
    ADVANCED("Napredni"),
}

@Serializable
enum class Focus(val label: String) {
    FULL_BODY("Cijelo tijelo"),
    LEGS("Noge"),
    POSTERIOR_CHAIN("Leđa i stražnji lanac"),
    UPPER_BODY("Gornji dio tijela"),
    CORE("Trup"),
}

/** Dijelovi tijela s ozljedom ili ograničenjem – vježbe koje ih opterećuju se izbjegavaju. */
@Serializable
enum class BodyRegion(val label: String) {
    LOWER_BACK("Donji dio leđa"),
    SHOULDER("Rame"),
    KNEE("Koljeno"),
    HIP("Kuk"),
    ELBOW_WRIST("Lakat / zapešće"),
}

@Serializable
enum class Sex(val label: String) {
    MALE("Muški"),
    FEMALE("Ženski"),
}

@Serializable
enum class PlanMode(val label: String, val description: String) {
    AUTOMATIC("Automatski", "Aplikacija slaže plan."),
    CUSTOMIZED("Prilagođeni", "Aplikacija predloži plan, vi ga mijenjate."),
    MANUAL("Ručni", "Sami birate vježbe, serije i ponavljanja."),
}

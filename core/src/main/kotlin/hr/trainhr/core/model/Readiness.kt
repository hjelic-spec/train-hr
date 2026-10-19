package hr.trainhr.core.model

/**
 * Upitnik spremnosti za tjelesnu aktivnost – sedam općih pitanja prema PAR-Q+
 * (parafrazirano na hrvatski). Bilo koji odgovor "DA" → preporuka razgovora s liječnikom
 * prije početka ili povećanja aktivnosti.
 */
object Readiness {
    val questions = listOf(
        "Je li vam liječnik ikada rekao da imate srčanu bolest ili povišen krvni tlak?",
        "Osjećate li bol u prsima u mirovanju, tijekom svakodnevnih aktivnosti ili tjelesne aktivnosti?",
        "Jeste li u posljednjih 12 mjeseci gubili ravnotežu zbog vrtoglavice ili izgubili svijest?",
        "Je li vam dijagnosticirana neka druga kronična bolest (osim srčane bolesti ili povišenog tlaka)?",
        "Uzimate li trenutno lijekove propisane za kroničnu bolest?",
        "Imate li problem s kostima, zglobovima ili mekim tkivom koji bi se mogao pogoršati većom aktivnošću?",
        "Je li vam liječnik rekao da se smijete baviti tjelesnom aktivnošću samo pod medicinskim nadzorom?",
    )

    fun needsClearance(answers: List<Boolean>): Boolean = answers.any { it }

    fun complete(answers: List<Boolean>): Boolean = answers.size == questions.size
}

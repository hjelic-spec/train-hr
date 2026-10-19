package hr.trainhr.app.ui

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import hr.trainhr.app.data.AppData
import hr.trainhr.core.util.OneRepMax
import hr.trainhr.core.util.formatKg
import kotlin.math.roundToInt

private const val WINDOW_DAYS = 28
private const val RETEST_AFTER_DAYS = 56

/** Najbolji procijenjeni 1RM vježbe iz serija u zadnjih [WINDOW_DAYS] dana. */
private fun recentBestOneRm(d: AppData, exerciseId: String, today: Long): Double? =
    d.sessions.asSequence()
        .filter { today - it.epochDay <= WINDOW_DAYS }
        .flatMap { it.entries.asSequence() }
        .filter { it.exerciseId == exerciseId }
        .flatMap { it.sets.asSequence() }
        .filter { it.weightKg != null && it.reps > 0 && it.pain < 3 }
        .maxOfOrNull { OneRepMax.estimate(it.weightKg!!, it.reps, it.rir ?: 0) }

@Composable
fun ProgressScreen(vm: AppViewModel, d: AppData, onRetest: () -> Unit) {
    val today = AppViewModel.today()
    val catalog = vm.catalog(d)
    val plan = d.plan
    val test = vm.latestTest(d)

    ScreenColumn {
        Text("Napredak", style = MaterialTheme.typography.headlineSmall)

        if (plan != null && d.planStartEpochDay != null) {
            val daysInPlan = (today - d.planStartEpochDay + 1).coerceAtLeast(1)
            val window = minOf(WINDOW_DAYS.toLong(), daysInPlan)
            val expected = (plan.settings.daysPerWeek * window / 7.0).coerceAtLeast(1.0)
            val done = d.sessions.count { today - it.epochDay < window }
            val pct = (done / expected * 100).roundToInt().coerceAtMost(100)
            SectionTitle("Redovitost (zadnjih $window dana)")
            ContentCard {
                Text("Odrađeno $done od ~${expected.roundToInt()} planiranih treninga ($pct %).")
                Text(
                    when {
                        pct >= 80 -> "Odlična redovitost – to je najvažniji faktor napretka."
                        pct >= 50 -> "Solidno. Ako je plan prezahtjevan, razmislite o kraćim ili rjeđim treninzima – održivost je važnija od savršenog plana."
                        else -> "Redovitost je niska. Predlažem manje treninga tjedno ili kraće treninge koje možete održati."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (test != null) {
            SectionTitle("Snaga: test ${formatDate(test.epochDay)} → danas")
            ContentCard {
                test.results.values.forEach { r ->
                    val name = catalog[r.exerciseId]?.name ?: r.exerciseId
                    val now = recentBestOneRm(d, r.exerciseId, today)
                    val change = now?.let { ((it - r.estimatedOneRmKg) / r.estimatedOneRmKg * 100).roundToInt() }
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Test: ${formatKg(r.estimatedOneRmKg)} kg" +
                            (now?.let { " · sada: ${formatKg((it * 10).roundToInt() / 10.0)} kg (${if (change!! >= 0) "+" else ""}$change %)" }
                                ?: " · nema serija u zadnja 4 tjedna"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HorizontalDivider()
                }
                Text(
                    "Procjene 1RM iz serija su orijentacijske; najpouzdanije su iz serija s 4–10 ponavljanja.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val since = today - test.epochDay
            if (since >= RETEST_AFTER_DAYS) {
                InfoCard("Prošlo je ${since / 7} tjedana od testa – vrijeme je za ponovni test (najbolje nakon lakšeg tjedna).")
            }
            OutlinedButton(onClick = onRetest) { Text("Ponovi početni test") }
        }

        val functional = d.testHistory.mapNotNull { t -> t.functional?.let { t.epochDay to it } }
        if (functional.isNotEmpty()) {
            SectionTitle("Funkcionalni testovi")
            ContentCard {
                functional.forEach { (day, f) ->
                    Text(
                        "${formatDate(day)}: ustajanja ${f.chairStands30s}, ravnoteža L ${f.balanceLeftSec} s / D ${f.balanceRightSec} s",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        SectionTitle("Povijest treninga")
        if (d.sessions.isEmpty()) Text("Još nema odrađenih treninga.")
        d.sessions.asReversed().take(30).forEach { s ->
            ContentCard {
                Text("${formatDate(s.epochDay)} · ${s.workoutName}${if (s.deload) " (lakši tjedan)" else ""}", style = MaterialTheme.typography.titleMedium)
                s.entries.forEach { e ->
                    val name = catalog[e.exerciseId]?.name ?: e.exerciseId
                    val sets = e.sets.joinToString("  ") { st -> (st.weightKg?.let { "${formatKg(it)}×" } ?: "") + st.reps }
                    Text("$name: $sets", style = MaterialTheme.typography.bodyMedium)
                    if (e.note.isNotBlank()) Text("→ ${e.note}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

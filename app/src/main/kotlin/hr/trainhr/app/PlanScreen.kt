package hr.trainhr.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import hr.trainhr.core.SETS_PER_EXERCISE
import hr.trainhr.core.Timing
import hr.trainhr.core.kg

@Composable
fun PlanScreen(vm: AppViewModel, d: AppData) {
    val catalog = vm.catalog(d)
    val next = vm.nextWorkoutIndex(d)
    ScreenColumn {
        Title("Plan – ${d.goal.label}")
        Text(
            "${d.daysPerWeek}× tjedno · ${d.durationMinutes} min · $SETS_PER_EXERCISE × ${d.goal.repLabel} ponavljanja · " +
                "odmor ${restLabel(d.restSets)} / ${restLabel(d.restExercises)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (d.omitted.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "Ne stane u ${d.durationMinutes} min: " + d.omitted.mapNotNull { catalog[it]?.name }.joinToString() +
                        ". Produljite trening, dodajte trening tjedno ili odaberite manje vježbi.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        d.plan.forEachIndexed { i, w ->
            val exercises = w.exerciseIds.mapNotNull { catalog[it] }
            BoxCard {
                Text(
                    w.name + " · ~${Timing.workoutMinutes(exercises, d.goal, d.restSets, d.restExercises)} min" + if (i == next) "  · sljedeći" else "",
                    style = MaterialTheme.typography.titleMedium,
                )
                exercises.forEach { ex ->
                    val r = vm.recommendation(d, ex.id)
                    Text(ex.name + (r?.weightKg?.let { " – ${kg(it)} kg" } ?: ""))
                }
                if (i == next) {
                    Button(onClick = { vm.startWorkout(i) }, modifier = Modifier.fillMaxWidth()) { Text("Započni trening") }
                } else {
                    OutlinedButton(onClick = { vm.startWorkout(i) }) { Text("Započni") }
                }
            }
        }
    }
}

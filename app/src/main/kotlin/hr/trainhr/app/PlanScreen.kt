package hr.trainhr.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hr.trainhr.core.kg

@Composable
fun PlanScreen(vm: AppViewModel, d: AppData) {
    val catalog = vm.catalog(d)
    val next = vm.nextWorkoutIndex(d)
    ScreenColumn {
        Title("Plan – ${d.daysPerWeek}× tjedno")
        d.plan.forEachIndexed { i, w ->
            BoxCard {
                Text(w.name + if (i == next) "  · sljedeći" else "", style = MaterialTheme.typography.titleMedium)
                w.exerciseIds.forEach { id ->
                    val ex = catalog[id] ?: return@forEach
                    val r = vm.recommendation(d, id)
                    Text(ex.name)
                    r?.weightKg?.let { Text("   ${kg(it)} kg", style = MaterialTheme.typography.bodyMedium) }
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

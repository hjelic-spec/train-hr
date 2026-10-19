package hr.trainhr.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import hr.trainhr.core.SetEntry
import hr.trainhr.core.kg
import hr.trainhr.core.workingSets
import kotlin.math.roundToInt

/** Vrijednost za graf: najveća težina, a za vježbe bez težine najbolji broj ponavljanja/sekundi. */
private fun progressValue(sets: List<SetEntry>): Double {
    val top = sets.mapNotNull { it.weightKg }.maxOrNull()
    return top ?: sets.maxOf { it.reps }.toDouble()
}

@Composable
fun ProgressScreen(vm: AppViewModel, d: AppData) {
    val catalog = vm.catalog(d)
    var deleting by remember { mutableStateOf<Session?>(null) }
    val ids = (d.plan.flatMap { it.exerciseIds } + d.sessions.flatMap { s -> s.entries.map { it.exerciseId } }).distinct()

    ScreenColumn {
        Title("Napredak")
        if (d.sessions.isEmpty()) Text("Još nema odrađenih treninga.")
        ids.forEach { id ->
            val ex = catalog[id] ?: return@forEach
            val history = vm.history(d, id)
            if (history.isEmpty()) return@forEach
            val values = history.map { progressValue(it.second) }
            val unit = if (history.last().second.any { it.weightKg != null }) "kg" else if (ex.timed) "s" else "pon."
            BoxCard {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                val first = values.first()
                val now = values.last()
                val change = if (first > 0) ((now - first) / first * 100).roundToInt() else 0
                Text(
                    "Početak ${kg(first)} $unit → sada ${kg(now)} $unit" +
                        if (history.size > 1) " (${if (change >= 0) "+" else ""}$change %)" else "",
                )
                if (values.size >= 2) LineChart(values)
                vm.recommendation(d, id)?.let { Text("Preporuka: ${it.text}", style = MaterialTheme.typography.bodyMedium) }
                history.takeLast(3).asReversed().forEach { (day, sets) ->
                    Text("${formatDate(day)}: ${describeSets(workingSets(sets), ex.timed)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (d.sessions.isNotEmpty()) {
            Section("Odrađeni treninzi")
            d.sessions.asReversed().forEach { s ->
                BoxCard {
                    Text("${formatDate(s.epochDay)} · ${s.workoutName}", style = MaterialTheme.typography.titleMedium)
                    s.entries.forEach { e ->
                        val ex = catalog[e.exerciseId]
                        Text("${ex?.name ?: e.exerciseId}: ${describeSets(e.sets, ex?.timed == true)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { deleting = s }) { Text("Obriši trening") }
                }
            }
        }
    }

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Obrisati trening?") },
            text = { Text("${formatDate(s.epochDay)} · ${s.workoutName} – brisanje se ne može poništiti.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(s.id); deleting = null }) { Text("Obriši") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Odustani") } },
        )
    }
}

@Composable
private fun LineChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val pad = 8.dp.toPx()
        val points = values.mapIndexed { i, v ->
            Offset(
                pad + i * (size.width - 2 * pad) / (values.size - 1),
                size.height - pad - ((v - min) / span * (size.height - 2 * pad)).toFloat(),
            )
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        points.forEach { drawCircle(color, radius = 4.dp.toPx(), center = it) }
    }
}

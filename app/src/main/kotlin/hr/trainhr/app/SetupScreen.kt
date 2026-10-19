package hr.trainhr.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.trainhr.core.Catalog
import hr.trainhr.core.Exercise
import hr.trainhr.core.LoadKind
import hr.trainhr.core.MuscleGroup
import hr.trainhr.core.RepRange

@Composable
fun SetupScreen(d: AppData, onSave: (Int, RepRange, List<String>, List<Exercise>) -> Unit, onCancel: (() -> Unit)?) {
    var days by remember { mutableStateOf(d.daysPerWeek) }
    var range by remember { mutableStateOf(d.repRange) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(d.selected) } }
    val custom = remember { mutableStateListOf<Exercise>().apply { addAll(d.customExercises) } }
    var addingTo by remember { mutableStateOf<MuscleGroup?>(null) }
    val catalog = Catalog(custom.toList())

    ScreenColumn {
        Title("Postavke plana")
        Section("Broj treninga tjedno")
        Chips((1..5).toList(), days, { "$it" }) { days = it }
        Section("Ponavljanja po seriji")
        Chips(RepRange.entries, range, { it.label }) { range = it }
        Text("Svaka vježba: 3 serije u odabranom rasponu ponavljanja.", style = MaterialTheme.typography.bodyMedium)

        Section("Vježbe po mišićnim skupinama")
        Text("Odaberite vježbe koje želite raditi. Aplikacija ih raspoređuje po treninzima.", style = MaterialTheme.typography.bodyMedium)
        MuscleGroup.entries.forEach { g ->
            BoxCard {
                val count = catalog.all.count { it.group == g && it.id in selected }
                Text("${g.label}${if (count > 0) " ($count)" else ""}", style = MaterialTheme.typography.titleMedium)
                catalog.all.filter { it.group == g }.forEach { ex ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { if (ex.id in selected) selected.remove(ex.id) else selected.add(ex.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(ex.id in selected, null)
                        Text(ex.name)
                    }
                }
                TextButton(onClick = { addingTo = g }) { Text("+ Vlastita vježba") }
            }
        }
        Button(
            onClick = { onSave(days, range, selected.toList(), custom.toList()) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Složi plan") }
        if (onCancel != null) TextButton(onClick = onCancel) { Text("Odustani") }
    }

    addingTo?.let { g ->
        CustomExerciseDialog(g, onDismiss = { addingTo = null }) { ex ->
            custom.add(ex)
            selected.add(ex.id)
            addingTo = null
        }
    }
}

@Composable
private fun CustomExerciseDialog(group: MuscleGroup, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(LoadKind.DUMBBELL) }
    var timed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vlastita vježba – ${group.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Naziv") }, singleLine = true)
                Text("Opterećenje")
                Chips(LoadKind.entries, kind, { it.label }) { kind = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(timed, { timed = it })
                    Text("  Bilježi se u sekundama")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onSave(Exercise("custom_${System.currentTimeMillis()}", name.trim(), group, kind, timed))
            }) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}

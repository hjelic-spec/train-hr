package hr.trainhr.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, onEditPlan: () -> Unit) {
    var confirmImport by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(it) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.import(it) }
    }

    ScreenColumn {
        Title("Postavke")
        OutlinedButton(onClick = onEditPlan, modifier = Modifier.fillMaxWidth()) { Text("Promijeni broj treninga i vježbe") }
        Section("Sigurnosna kopija")
        Text(
            "Podaci su samo na ovom telefonu (aplikacija ne koristi internet). Povremeno ih izvezite u datoteku.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = { exporter.launch("trening-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
            Text("Izvoz podataka")
        }
        OutlinedButton(onClick = { confirmImport = true }, modifier = Modifier.fillMaxWidth()) { Text("Uvoz podataka") }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Uvesti podatke?") },
            text = { Text("Uvoz zamjenjuje sve trenutne podatke podacima iz datoteke.") },
            confirmButton = {
                TextButton(onClick = { confirmImport = false; importer.launch(arrayOf("application/json", "*/*")) }) {
                    Text("Odaberi datoteku")
                }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Odustani") } },
        )
    }
}

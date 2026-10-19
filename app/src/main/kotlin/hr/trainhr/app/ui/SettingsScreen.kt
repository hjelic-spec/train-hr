package hr.trainhr.app.ui

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
import hr.trainhr.app.data.AppData
import java.time.LocalDate

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    d: AppData,
    onEditProfile: () -> Unit,
    onEditEquipment: () -> Unit,
    onEditReadiness: () -> Unit,
) {
    var confirmImport by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(it) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.import(it) }
    }

    ScreenColumn {
        Text("Postavke", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) { Text("Profil i ograničenja") }
        OutlinedButton(onClick = onEditEquipment, modifier = Modifier.fillMaxWidth()) { Text("Oprema i težine") }
        OutlinedButton(onClick = onEditReadiness, modifier = Modifier.fillMaxWidth()) { Text("Upitnik spremnosti") }

        SectionTitle("Sigurnosna kopija")
        Text(
            "Aplikacija ne koristi internet; podaci su samo na ovom telefonu. Izvezite ih povremeno " +
                "u datoteku i spremite na sigurno mjesto." +
                (d.lastExportEpochDay?.let { " Zadnji izvoz: ${formatDate(it)}." } ?: " Izvoz još nije napravljen."),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = { exporter.launch("trening-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
            Text("Izvoz podataka")
        }
        OutlinedButton(onClick = { confirmImport = true }, modifier = Modifier.fillMaxWidth()) { Text("Uvoz podataka") }

        SectionTitle("O aplikaciji")
        Text(
            "Preporuke se temelje na smjernicama ACSM-a (2011. za starije osobe; 2026. za trening snage) i " +
                "ne zamjenjuju liječnika ni trenera. Procjena 1RM: Brzycki (do 10 ponavljanja) i Epley.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Uvesti podatke?") },
            text = { Text("Uvoz zamjenjuje SVE trenutne podatke na ovom telefonu podacima iz datoteke.") },
            confirmButton = {
                TextButton(onClick = { confirmImport = false; importer.launch(arrayOf("application/json", "*/*")) }) { Text("Odaberi datoteku") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Odustani") } },
        )
    }
}

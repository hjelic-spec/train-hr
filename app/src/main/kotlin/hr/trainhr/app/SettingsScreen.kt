package hr.trainhr.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onEditPlan: () -> Unit) {
    var confirmImport by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(it) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.import(it) }
    }
    BackHandler(onBack = onBack)

    ScreenColumn {
        TextButton(onClick = onBack) { Text("‹ Natrag") }
        Title("Postavke")
        Card(Modifier.fillMaxWidth().clickable(onClick = onEditPlan)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Plan i program", fontWeight = FontWeight.SemiBold)
                    Muted("Cilj, početak i kraj, broj i trajanje treninga, odmori, vježbe, tjelesna masa")
                }
                Muted("›")
            }
        }
        Section("Sigurnosna kopija")
        Muted("Podaci su samo na ovom telefonu (aplikacija ne koristi internet). Povremeno ih izvezite u datoteku.")
        OutlinedButton(onClick = { exporter.launch("trening-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("Izvoz podataka") }
        OutlinedButton(onClick = { confirmImport = true }, modifier = Modifier.fillMaxWidth()) { Text("Uvoz podataka") }
        Section("O procjenama")
        Muted("Preporuke težine: dvostruka progresija (povećanje kad sve serije dođu do gornje granice ponavljanja). " +
            "Kalorije: 3,5 MET × tjelesna masa × trajanje (2024 Adult Compendium of Physical Activities, šifra 02054) – gruba procjena.")
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Uvesti podatke?") },
            text = { Text("Uvoz zamjenjuje sve trenutne podatke podacima iz datoteke.") },
            confirmButton = {
                TextButton(onClick = { confirmImport = false; importer.launch(arrayOf("application/json", "*/*")) }) { Text("Odaberi datoteku") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Odustani") } },
        )
    }
}

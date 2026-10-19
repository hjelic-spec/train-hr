package hr.trainhr.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hr.trainhr.core.model.BodyRegion
import hr.trainhr.core.model.Equipment
import hr.trainhr.core.model.EquipmentSettings
import hr.trainhr.core.model.Experience
import hr.trainhr.core.model.Readiness
import hr.trainhr.core.model.Sex
import hr.trainhr.core.model.Sport
import hr.trainhr.core.model.UserProfile
import hr.trainhr.core.util.formatKg

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
fun ProfileScreen(initial: UserProfile, buttonText: String, onSave: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var age by remember { mutableStateOf(initial.age.toString()) }
    var sex by remember { mutableStateOf(initial.sex) }
    var weight by remember { mutableStateOf(formatKg(initial.bodyWeightKg)) }
    var experience by remember { mutableStateOf(initial.experience) }
    var sport by remember { mutableStateOf(initial.sport) }
    var limitations by remember { mutableStateOf(initial.limitations) }

    val ageNum = age.toIntOrNull()
    val weightNum = parseNumber(weight)
    val valid = ageNum != null && ageNum in 18..100 && weightNum != null && weightNum in 30.0..250.0

    ScreenColumn {
        Text("Profil", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(name, { name = it }, label = { Text("Ime") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Dob", age, { age = it }, Modifier.weight(1f), decimal = false)
            NumberField("Tjelesna masa (kg)", weight, { weight = it }, Modifier.weight(1f))
        }
        SectionTitle("Spol")
        ChoiceChips(Sex.entries, sex, { it.label }) { sex = it }
        SectionTitle("Iskustvo s treningom snage")
        ChoiceChips(Experience.entries, experience, { it.label }) { experience = it }
        SectionTitle("Sport")
        ChoiceChips(Sport.entries, sport, { it.label }) { sport = it }
        SectionTitle("Ozljede ili ograničenja")
        Text("Vježbe koje opterećuju označene dijelove tijela neće se automatski birati.", style = MaterialTheme.typography.bodyMedium)
        MultiChips(BodyRegion.entries, limitations, { it.label }) { r ->
            limitations = if (r in limitations) limitations - r else limitations + r
        }
        Button(
            onClick = {
                onSave(
                    initial.copy(
                        name = name.trim(), age = ageNum!!, sex = sex, bodyWeightKg = weightNum!!,
                        experience = experience, sport = sport, limitations = limitations,
                    ),
                )
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text(buttonText) }
        if (!valid) Text("Upišite dob (18–100) i tjelesnu masu.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ReadinessScreen(initial: List<Boolean>, onSave: (List<Boolean>, Boolean) -> Unit) {
    val answers = remember {
        mutableStateListOf<Boolean?>().apply {
            Readiness.questions.indices.forEach { add(initial.getOrNull(it)) }
        }
    }
    var acknowledged by remember { mutableStateOf(false) }
    val complete = answers.all { it != null }
    val anyYes = answers.any { it == true }

    ScreenColumn {
        Text("Upitnik spremnosti", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Opća pitanja prema upitniku PAR-Q+. Ako na bilo koje odgovorite s DA, prije početka " +
                "ili povećanja aktivnosti preporučuje se razgovor s liječnikom.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Readiness.questions.forEachIndexed { i, q ->
            ContentCard {
                Text("${i + 1}. $q")
                ChoiceChips(listOf(true, false), answers[i], { if (it) "DA" else "NE" }) { answers[i] = it }
            }
        }
        if (anyYes) {
            InfoCard(
                "Odgovorili ste DA na barem jedno pitanje. Preporuka je da se prije treninga snage " +
                    "i početnog testa posavjetujete s liječnikom.",
                warning = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(acknowledged, { acknowledged = it })
                Text("Posavjetovao/la sam se s liječnikom ili nastavljam na vlastitu odgovornost.")
            }
        }
        Button(
            onClick = { onSave(answers.map { it == true }, acknowledged) },
            enabled = complete && (!anyYes || acknowledged),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("Dalje") }
    }
}

@Composable
fun EquipmentScreen(initial: EquipmentSettings, buttonText: String, onSave: (EquipmentSettings) -> Unit) {
    var available by remember { mutableStateOf(initial.available) }
    var dumbbells by remember { mutableStateOf(initial.dumbbells.joinToString(" ") { formatKg(it) }) }
    var barbell by remember { mutableStateOf(formatKg(initial.barbellKg)) }
    var trapBar by remember { mutableStateOf(formatKg(initial.trapBarKg)) }
    var plate by remember { mutableStateOf(formatKg(initial.smallestPlateKg)) }
    var latStep by remember { mutableStateOf(formatKg(initial.latStackStepKg)) }
    var latMax by remember { mutableStateOf(formatKg(initial.latStackMaxKg)) }
    var cableStep by remember { mutableStateOf(formatKg(initial.cableStackStepKg)) }
    var cableMax by remember { mutableStateOf(formatKg(initial.cableStackMaxKg)) }

    val dbList = dumbbells.split(' ', ';', '\n').mapNotNull { parseNumber(it) }.filter { it > 0 }.distinct().sorted()
    val nums = listOf(barbell, trapBar, plate, latStep, latMax, cableStep, cableMax).map { parseNumber(it) }
    val valid = nums.all { it != null && it > 0 } && (Equipment.DUMBBELL !in available || dbList.isNotEmpty())

    ScreenColumn {
        Text("Oprema", style = MaterialTheme.typography.headlineSmall)
        Text("Označite što postoji u teretani. Težine određuju korake progresije.", style = MaterialTheme.typography.bodyMedium)
        MultiChips(Equipment.entries, available, { it.label }) { e ->
            available = if (e in available) available - e else available + e
        }
        SectionTitle("Bučice")
        OutlinedTextField(
            dumbbells, { dumbbells = it },
            label = { Text("Težine bučica (kg), odvojene razmakom") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Prepoznato: ${dbList.size} težina", style = MaterialTheme.typography.bodyMedium)
        SectionTitle("Šipka i trap bar")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Šipka (kg)", barbell, { barbell = it }, Modifier.weight(1f))
            NumberField("Trap bar (kg)", trapBar, { trapBar = it }, Modifier.weight(1f))
        }
        NumberField("Najmanja ploča (kg)", plate, { plate = it }, Modifier.fillMaxWidth())
        Text("Trap barovi se razlikuju po težini – provjerite oznaku na šipki.", style = MaterialTheme.typography.bodyMedium)
        SectionTitle("Lat mašina")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Korak (kg)", latStep, { latStep = it }, Modifier.weight(1f))
            NumberField("Najviše (kg)", latMax, { latMax = it }, Modifier.weight(1f))
        }
        SectionTitle("Sajla za veslanje")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Korak (kg)", cableStep, { cableStep = it }, Modifier.weight(1f))
            NumberField("Najviše (kg)", cableMax, { cableMax = it }, Modifier.weight(1f))
        }
        Button(
            onClick = {
                onSave(
                    EquipmentSettings(
                        available = available,
                        dumbbells = dbList,
                        barbellKg = nums[0]!!, trapBarKg = nums[1]!!, smallestPlateKg = nums[2]!!,
                        latStackStepKg = nums[3]!!, latStackMaxKg = nums[4]!!,
                        cableStackStepKg = nums[5]!!, cableStackMaxKg = nums[6]!!,
                    ),
                )
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text(buttonText) }
    }
}

package hr.trainhr.app

import hr.trainhr.core.Exercise
import hr.trainhr.core.Goal
import hr.trainhr.core.SetEntry
import hr.trainhr.core.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppData(
    val goal: Goal = Goal.HYPERTROPHY,
    val daysPerWeek: Int = 3,
    val durationMinutes: Int = 60,
    /** Odabrane vježbe, redoslijedom odabira. */
    val selected: List<String> = emptyList(),
    val customExercises: List<Exercise> = emptyList(),
    val plan: List<Workout> = emptyList(),
    /** Odabrane vježbe koje nisu stale u plan zbog trajanja treninga. */
    val omitted: List<String> = emptyList(),
    val sessions: List<Session> = emptyList(),
)

@Serializable
data class Session(
    val id: Long,
    val epochDay: Long,
    val workoutName: String,
    val entries: List<Entry>,
    val durationSeconds: Int? = null,
)

@Serializable
data class Entry(val exerciseId: String, val sets: List<SetEntry>)

/** Svi podaci u jednoj JSON datoteci; zapis preko privremene datoteke da se ne ošteti. */
class Repository(private val dir: File) {
    private val file = File(dir, "podaci.json")
    private val mutex = Mutex()
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData =
        runCatching { json.decodeFromString(AppData.serializer(), file.readText()) }.getOrElse { AppData() }

    suspend fun update(transform: (AppData) -> AppData) = mutex.withLock {
        val next = transform(_data.value)
        _data.value = next
        withContext(Dispatchers.IO) {
            val tmp = File(dir, "podaci.json.tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), next))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        out.use { it.write(json.encodeToString(AppData.serializer(), _data.value).toByteArray()) }
    }

    suspend fun import(input: InputStream): Result<Unit> = runCatching {
        val text = withContext(Dispatchers.IO) { input.use { it.readBytes().decodeToString() } }
        val imported = json.decodeFromString(AppData.serializer(), text)
        update { imported }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

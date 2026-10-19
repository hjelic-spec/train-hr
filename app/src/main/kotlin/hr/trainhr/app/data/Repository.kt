package hr.trainhr.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Spremište u jednoj JSON datoteci; zapis ide preko privremene datoteke da se ne ošteti. */
class Repository(private val dir: File) {

    private val file = File(dir, FILE_NAME)
    private val mutex = Mutex()
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData =
        runCatching { json.decodeFromString(AppData.serializer(), file.readText()) }.getOrElse { AppData() }

    suspend fun update(transform: (AppData) -> AppData) = mutex.withLock {
        val next = transform(_data.value)
        _data.value = next
        withContext(Dispatchers.IO) { write(next) }
    }

    private fun write(d: AppData) {
        val tmp = File(dir, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        out.use { it.write(json.encodeToString(AppData.serializer(), _data.value).toByteArray()) }
    }

    /** Vraća grešku ako datoteka nije valjana sigurnosna kopija; postojeći podaci tada ostaju. */
    suspend fun import(input: InputStream): Result<Unit> = runCatching {
        val text = withContext(Dispatchers.IO) { input.use { it.readBytes().decodeToString() } }
        val imported = json.decodeFromString(AppData.serializer(), text)
        update { imported }
    }

    companion object {
        const val FILE_NAME = "podaci.json"
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}

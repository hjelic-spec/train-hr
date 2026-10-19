package hr.trainhr.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Položaj figure: glava, linije (udovi, trup) i oprema; koordinate u kutiji 0..100 (pod je y = 92). */
class Pose(val head: Offset, val lines: List<List<Offset>>, val eq: List<JsonObject>)
class Art(val cue: String, val frames: List<Pose>)

/** Ilustracije iz assets/exercise_art.json (izvezene iz web pregleda – isti crteži). */
object ExerciseArtStore {
    @Volatile private var cache: Map<String, Art>? = null

    fun get(context: Context, id: String): Art? = (cache ?: load(context).also { cache = it })[id]

    private fun pt(e: kotlinx.serialization.json.JsonElement): Offset =
        e.jsonArray.let { Offset(it[0].jsonPrimitive.float, it[1].jsonPrimitive.float) }

    private fun load(context: Context): Map<String, Art> = runCatching {
        val root = Json.parseToJsonElement(context.assets.open("exercise_art.json").bufferedReader().readText()).jsonObject
        root.mapValues { (_, v) ->
            val o = v.jsonObject
            Art(
                o["cue"]!!.jsonPrimitive.content,
                o["frames"]!!.jsonArray.map { f ->
                    val fo = f.jsonObject
                    Pose(pt(fo["head"]!!), fo["lines"]!!.jsonArray.map { l -> l.jsonArray.map(::pt) }, fo["eq"]!!.jsonArray.map { it.jsonObject })
                },
            )
        }
    }.getOrElse { emptyMap() }
}

@Composable
fun ExerciseArtView(id: String) {
    val art = ExerciseArtStore.get(androidx.compose.ui.platform.LocalContext.current, id) ?: return
    val labels = if (art.frames.size == 1) listOf("Položaj") else listOf("Početak", "Kraj")
    val fg = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outlineVariant
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            art.frames.forEachIndexed { k, pose ->
                if (k > 0) Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val m = if (art.frames.size == 1) Modifier.fillMaxWidth(0.5f) else Modifier.weight(1f)
                Column(m, horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.fillMaxWidth().height(112.dp)) { drawPose(pose, fg, accent, line) }
                    Muted(labels[k].uppercase())
                }
            }
        }
    }
    Text(art.cue, style = MaterialTheme.typography.bodyMedium)
}

private fun DrawScope.drawPose(p: Pose, fg: Color, accent: Color, groundColor: Color) {
    // viewBox 0 -10 100 106, umanjeno da stane i centrirano vodoravno
    val s = minOf(size.width / 100f, size.height / 106f)
    val dx = (size.width - 100f * s) / 2f
    fun o(x: Float, y: Float) = Offset(dx + x * s, (y + 10f) * s)
    fun o(v: Offset) = o(v.x, v.y)
    fun arr(e: JsonObject, k: String) = e[k]!!.jsonArray
    fun xy(a: JsonArray, i: Int = 0) = Offset(a[i].jsonPrimitive.float, a[i + 1].jsonPrimitive.float)
    val thin = Stroke(1.5f * s)

    drawLine(groundColor, o(2f, 92f), o(98f, 92f), 2f * s)
    for (e in p.eq) {
        when {
            "plate" in e -> drawCircle(accent, 9f * s, o(xy(arr(e, "plate"))))
            "db" in e -> {
                val c = xy(arr(e, "db"))
                val (w, h) = if ("v" in e) 5f to 10f else 10f to 5f
                drawRoundRect(accent, o(c.x - w / 2, c.y - h / 2), Size(w * s, h * s), androidx.compose.ui.geometry.CornerRadius(1.5f * s))
            }
            "bar" in e -> { val c = xy(arr(e, "bar")); drawRoundRect(accent, o(c.x - 6, c.y - 1.5f), Size(12 * s, 3 * s)) }
            "cable" in e -> {
                val a = arr(e, "cable"); val p1 = pointOf(a[0]); val p2 = pointOf(a[1])
                drawLine(accent, o(p1), o(p2), 1.5f * s, pathEffect = if ("dash" in e) PathEffect.dashPathEffect(floatArrayOf(3 * s, 3 * s)) else null)
                drawCircle(accent, 2.5f * s, o(p2))
            }
            "bench" in e -> {
                val a = arr(e, "bench"); val x = a[0].jsonPrimitive.float; val y = a[1].jsonPrimitive.float; val w = a[2].jsonPrimitive.float
                drawRect(accent, o(x, y), Size(w * s, 5 * s))
                drawLine(accent, o(x + 4, y + 5), o(x + 4, 92f), thin.width); drawLine(accent, o(x + w - 4, y + 5), o(x + w - 4, 92f), thin.width)
            }
            "seat" in e -> {
                val a = arr(e, "seat"); val x = a[0].jsonPrimitive.float; val y = a[1].jsonPrimitive.float; val w = a[2].jsonPrimitive.float
                drawRect(accent, o(x, y), Size(w * s, 5 * s)); drawLine(accent, o(x + w / 2, y + 5), o(x + w / 2, 92f), thin.width)
            }
            "box" in e -> {
                val a = arr(e, "box")
                drawRect(accent, o(a[0].jsonPrimitive.float, a[1].jsonPrimitive.float), Size(a[2].jsonPrimitive.float * s, a[3].jsonPrimitive.float * s), style = Stroke(2f * s))
            }
            "pad" in e -> drawCircle(accent, 3.5f * s, o(xy(arr(e, "pad"))))
            "incline" in e -> {
                val a = arr(e, "incline"); val p1 = pointOf(a[0]); val p2 = pointOf(a[1])
                drawLine(accent, o(p1), o(p2), 5f * s, cap = StrokeCap.Round); drawLine(accent, o(p2), o(p2.x, 92f), thin.width)
            }
            "plateStand" in e -> { val c = xy(arr(e, "plateStand")); drawRect(accent, o(c), Size(4 * s, 26 * s)) }
        }
    }
    val limb = Stroke(4.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    for (l in p.lines) {
        val path = Path().apply { moveTo(o(l[0]).x, o(l[0]).y); l.drop(1).forEach { lineTo(o(it).x, o(it).y) } }
        drawPath(path, fg, style = limb)
    }
    drawCircle(fg, 6f * s, o(p.head))
}

private fun pointOf(e: kotlinx.serialization.json.JsonElement) = e.jsonArray.let { Offset(it[0].jsonPrimitive.float, it[1].jsonPrimitive.float) }

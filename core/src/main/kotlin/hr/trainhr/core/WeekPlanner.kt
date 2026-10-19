package hr.trainhr.core

/** Treninzi u tjednu u odnosu na početni datum: razmak između treninga ≥ 1 dan gdje god je moguće. */
val DAY_OFFSETS: Map<Int, List<Int>> = mapOf(
    1 to listOf(0), 2 to listOf(0, 3), 3 to listOf(0, 2, 4), 4 to listOf(0, 1, 3, 4), 5 to listOf(0, 1, 2, 4, 5),
)

/** Redoslijed u treningu: velike skupine i višezglobne vježbe prije malih (ACSM 2009), trup na kraju. */
val GROUP_TIER: Map<MuscleGroup, Int> = mapOf(
    MuscleGroup.LEGS to 0, MuscleGroup.BACK to 0, MuscleGroup.CHEST to 0,
    MuscleGroup.SHOULDERS to 1, MuscleGroup.ARMS to 2, MuscleGroup.CORE to 3,
)

val BIG_GROUPS = setOf(MuscleGroup.LEGS, MuscleGroup.BACK, MuscleGroup.CHEST)

/** Koliko puta tjedno dolazi stalna vježba: u 2 treninga (ili u svaki, ako ih je manje). */
fun pinnedPerWeek(days: Int): Int = minOf(days, 2)

/** mulberry32 – ponovljiv slučajni niz; bit po bit isti kao u web pregledu (isti plan za isti tjedan). */
class Mulberry32(seed: Int) {
    private var a = seed

    fun next(): Double {
        a += 0x6D2B79F5
        var t = a
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296.0
    }
}

fun <T> List<T>.shuffledWith(r: Mulberry32): List<T> {
    val x = toMutableList()
    for (i in x.size - 1 downTo 1) {
        val j = (r.next() * (i + 1)).toInt()
        val tmp = x[i]; x[i] = x[j]; x[j] = tmp
    }
    return x
}

data class WeekPlan(
    /** Za svaki trening u tjednu: id-ovi vježbi redom izvođenja. */
    val days: List<List<String>>,
    /** Odabrane vježbe koje ovaj tjedan nisu stale. */
    val omitted: List<Exercise>,
)

/**
 * Raspored za jedan tjedan: svaki tjedan (i na „Promiješaj”) drugačiji izbor i redoslijed vježbi,
 * ali uvijek iz odabranih vježbi, najviše [capacity] po treningu (stane u trajanje).
 * Pravila: stalne vježbe u [pinnedPerWeek] treninga; velike skupine (noge, leđa, prsa) u svakom
 * treningu kad ima mjesta; ostale skupine se izmjenjuju; svaka skupina kruži kroz sve svoje vježbe;
 * ista vježba ne dolazi dva treninga zaredom ako postoji zamjena.
 */
object WeekPlanner {

    fun plan(
        selected: List<Exercise>,
        daysPerWeek: Int,
        capacity: Int,
        seed: Int,
        pinnedIds: Collection<String> = emptyList(),
    ): WeekPlan {
        val days = daysPerWeek.coerceIn(1, 5)
        val cap = capacity.coerceAtLeast(1)
        val r = Mulberry32(seed)
        val pinSet = pinnedIds.toSet()
        val pinned = selected.filter { it.id in pinSet }
        val loose = selected.filter { it.id !in pinSet }

        // Stalne vježbe raspoređene ravnomjerno po danima, s pomakom da ne padnu sve na isti dan.
        val pinnedDays = List(days) { mutableListOf<Exercise>() }
        val n = pinnedPerWeek(days)
        val base = (r.next() * days).toInt()
        pinned.forEachIndexed { idx, e ->
            for (j in 0 until n) pinnedDays[(base + idx + (j * days / n)) % days].add(e)
        }

        val groups = MuscleGroup.entries.filter { g -> selected.any { it.group == g } }
        val queues = groups.associateWith { g -> loose.filter { it.group == g }.shuffledWith(r) }
        val cursor = groups.associateWith { 0 }.toMutableMap()
        val big = groups.filter { it in BIG_GROUPS }
        val small = groups.filter { it !in BIG_GROUPS }.shuffledWith(r)
        var smallPos = 0
        var prevDay = emptySet<String>()
        val weekCount = groups.associateWith { 0 }.toMutableMap()
        val out = mutableListOf<List<String>>()

        for (d in 0 until days) {
            val today = mutableListOf<Exercise>()
            val used = mutableSetOf<String>()
            fun take(g: MuscleGroup): Boolean {
                val q = queues.getValue(g)
                for (k in q.indices) {
                    val ex = q[(cursor.getValue(g) + k) % q.size]
                    if (ex.id in used) continue
                    if (ex.id in prevDay && q.any { it.id !in used && it.id !in prevDay }) continue
                    cursor[g] = (cursor.getValue(g) + k + 1) % q.size
                    used += ex.id
                    today += ex
                    return true
                }
                return false
            }

            for (e in pinnedDays[d]) if (today.size < cap && e.id !in used) { used += e.id; today += e }
            for (g in big.shuffledWith(r)) if (today.size < cap && today.none { it.group == g }) take(g)
            var k = 0
            while (k < small.size && today.size < cap) {
                val g = small[(smallPos + k) % small.size]
                if (today.none { it.group == g }) take(g)
                k++
            }
            smallPos = (smallPos + maxOf(1, cap - big.size)) % maxOf(1, small.size)

            // Preostala mjesta: skupina s najmanje vježbi ovaj tjedan (najviše 2 po skupini u treningu).
            var added = true
            while (today.size < cap && added) {
                added = false
                val order = groups.shuffledWith(r).sortedBy { weekCount.getValue(it) }
                for (g in order) {
                    if (today.count { it.group == g } >= 2) continue
                    if (take(g)) { added = true; break }
                }
            }
            today.forEach { weekCount[it.group] = weekCount.getValue(it.group) + 1 }

            // Velike skupine prije malih; unutar iste razine stalne vježbe prve (dok ste svježi).
            val ordered = today.shuffledWith(r).sortedWith(
                compareBy<Exercise> { GROUP_TIER.getValue(it.group) }.thenBy { if (it.id in pinSet) 0 else 1 },
            )
            out += ordered.map { it.id }
            prevDay = used
        }
        val usedAll = out.flatten().toSet()
        return WeekPlan(out, selected.filter { it.id !in usedAll })
    }

    /** Sjeme tjedna – isto kao u web pregledu (cijeli broj s preljevom kao JavaScript `| 0`). */
    fun seed(startDay: Long, week: Int, salt: Int, daysPerWeek: Int, durationMinutes: Int): Int =
        (startDay * 1000003L + week * 7919L + salt * 104729L + daysPerWeek * 31L + durationMinutes).toInt()
}

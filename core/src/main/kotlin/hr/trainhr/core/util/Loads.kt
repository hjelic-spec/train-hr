package hr.trainhr.core.util

import hr.trainhr.core.model.EquipmentSettings
import hr.trainhr.core.model.LoadType
import kotlin.math.floor
import kotlin.math.round

/** Zaokruživanje na težine koje stvarno postoje u teretani. */
class Loads(private val eq: EquipmentSettings) {

    /** Sve moguće težine za dani tip opterećenja, uzlazno (prazno za [LoadType.NONE]). */
    fun options(type: LoadType): List<Double> = when (type) {
        LoadType.DUMBBELL -> eq.dumbbells.sorted()
        LoadType.BARBELL -> plateLoaded(eq.barbellKg)
        LoadType.TRAP_BAR -> plateLoaded(eq.trapBarKg)
        LoadType.LAT_STACK -> stack(eq.latStackStepKg, eq.latStackMaxKg)
        LoadType.CABLE_STACK -> stack(eq.cableStackStepKg, eq.cableStackMaxKg)
        LoadType.NONE -> emptyList()
    }

    /** Najveća dostupna težina ≤ [target]; ako je i najmanja veća, vraća najmanju. */
    fun roundDown(type: LoadType, target: Double): Double? {
        val opts = options(type)
        if (opts.isEmpty()) return null
        return opts.lastOrNull { it <= target + EPS } ?: opts.first()
    }

    fun minimum(type: LoadType): Double? = options(type).firstOrNull()

    fun next(type: LoadType, current: Double): Double? =
        options(type).firstOrNull { it > current + EPS }

    fun previous(type: LoadType, current: Double): Double? =
        options(type).lastOrNull { it < current - EPS }

    private fun plateLoaded(bar: Double): List<Double> {
        val step = 2 * eq.smallestPlateKg
        if (step <= 0) return listOf(bar)
        return (0..MAX_PLATE_STEPS).map { r2(bar + it * step) }
    }

    private fun stack(step: Double, max: Double): List<Double> {
        if (step <= 0) return emptyList()
        val n = floor(max / step + EPS).toInt()
        return (1..n).map { r2(it * step) }
    }

    private fun r2(v: Double) = round(v * 100) / 100

    private companion object {
        const val EPS = 1e-6
        const val MAX_PLATE_STEPS = 200
    }
}

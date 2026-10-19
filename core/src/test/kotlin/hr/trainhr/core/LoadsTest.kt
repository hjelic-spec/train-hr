package hr.trainhr.core

import hr.trainhr.core.model.EquipmentSettings
import hr.trainhr.core.model.LoadType
import hr.trainhr.core.util.Loads
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadsTest {
    private val loads = Loads(EquipmentSettings())

    @Test
    fun dumbbellsRoundDownToAvailable() {
        assertEquals(14.0, loads.roundDown(LoadType.DUMBBELL, 15.3))
        assertEquals(8.0, loads.roundDown(LoadType.DUMBBELL, 5.0)) // ispod minimuma → najmanja bučica
        assertEquals(40.0, loads.roundDown(LoadType.DUMBBELL, 55.0))
    }

    @Test
    fun barbellStepsByTwoSmallestPlates() {
        assertEquals(62.5, loads.roundDown(LoadType.BARBELL, 64.0))
        assertEquals(65.0, loads.next(LoadType.BARBELL, 62.5))
        assertEquals(20.0, loads.minimum(LoadType.BARBELL))
    }

    @Test
    fun trapBarUsesItsOwnWeight() {
        assertEquals(25.0, loads.minimum(LoadType.TRAP_BAR))
        assertEquals(27.5, loads.next(LoadType.TRAP_BAR, 25.0))
    }

    @Test
    fun stackAndNone() {
        assertEquals(45.0, loads.roundDown(LoadType.LAT_STACK, 47.0))
        assertNull(loads.next(LoadType.CABLE_STACK, 100.0))
        assertNull(loads.roundDown(LoadType.NONE, 10.0))
    }
}

package ru.systemt.highwater.data.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleConfigTest {

    @Test
    fun `scan windows match the specification`() {
        assertEquals(60_000L, BleConfig.SCAN_DURATION_MS)
        assertEquals(15_000L, BleConfig.SEARCH_TIMEOUT_MS)
        assertEquals(15_000L, BleConfig.CONNECT_TIMEOUT_MS)
    }

    @Test
    fun `manufacturer filter is well formed`() {
        assertTrue(BleConfig.MANUFACTURER_DATA_PREFIX.isNotEmpty())
        assertEquals(
            BleConfig.MANUFACTURER_DATA_PREFIX.size,
            BleConfig.MANUFACTURER_DATA_MASK.size,
        )
        assertTrue(BleConfig.MANUFACTURER_DATA_MASK.all { it == 0xFF.toByte() })
    }
}

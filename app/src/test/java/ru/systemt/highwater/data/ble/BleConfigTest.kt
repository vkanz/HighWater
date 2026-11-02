package ru.systemt.highwater.data.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class BleConfigTest {

    @Test
    fun `scan windows match the specification`() {
        assertEquals(60_000L, BleConfig.SCAN_DURATION_MS)
        assertEquals(15_000L, BleConfig.SEARCH_TIMEOUT_MS)
        assertEquals(15_000L, BleConfig.CONNECT_TIMEOUT_MS)
    }

    @Test
    fun `amd filter matches the delphi prototype`() {
        assertEquals(0x53, BleConfig.AMD_SERIAL_AD_TYPE)
        assertEquals(0x08, BleConfig.SHORTENED_LOCAL_NAME_AD_TYPE)
        assertEquals(4, BleConfig.AMD_AD_STRUCTURE_COUNT)
        assertEquals(12, BleConfig.AMD_SHORTENED_NAME_LENGTH)
        assertEquals("S", BleConfig.AMD_NAME_PREFIX)
    }
}

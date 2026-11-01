package ru.systemt.highwater.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleStateTest {

    @Test
    fun `scanning and connected phases are active`() {
        assertTrue(BleState(phase = BlePhase.SCANNING).isActive)
        assertTrue(BleState(phase = BlePhase.SEARCHING).isActive)
        assertTrue(BleState(phase = BlePhase.CONNECTING).isActive)
        assertTrue(BleState(phase = BlePhase.CONNECTED).isActive)
    }

    @Test
    fun `idle and failed phases are not active`() {
        assertFalse(BleState(phase = BlePhase.IDLE).isActive)
        assertFalse(BleState(phase = BlePhase.FAILED).isActive)
    }
}

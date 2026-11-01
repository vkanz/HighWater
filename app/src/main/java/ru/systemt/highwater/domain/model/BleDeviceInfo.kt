package ru.systemt.highwater.domain.model

/**
 * Информация об обнаруженном BLE-устройстве.
 */
data class BleDeviceInfo(
    val name: String?,
    val address: String,
    val rssi: Int,
)

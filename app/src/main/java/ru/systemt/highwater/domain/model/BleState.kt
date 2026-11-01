package ru.systemt.highwater.domain.model

/**
 * Текущая фаза работы BLE-менеджера.
 */
enum class BlePhase {
    IDLE,
    SCANNING,
    SEARCHING,
    CONNECTING,
    CONNECTED,
    FAILED,
}

/**
 * Единое состояние BLE-сессии, которое наблюдают ViewModel-и и foreground-сервис.
 */
data class BleState(
    val phase: BlePhase = BlePhase.IDLE,
    val devices: List<BleDeviceInfo> = emptyList(),
    /** Прогресс сканирования списка устройств, 0f..1f. */
    val scanProgress: Float = 0f,
    /** Прогресс поиска сохранённого устройства, 0f..1f. */
    val searchProgress: Float = 0f,
    val targetAddress: String? = null,
    val connectedDevice: BleDeviceInfo? = null,
    val rssi: Int? = null,
    val errorMessage: String? = null,
) {
    /** true, пока выполняется сканирование, поиск, подключение или есть активное соединение. */
    val isActive: Boolean
        get() = phase == BlePhase.SCANNING ||
            phase == BlePhase.SEARCHING ||
            phase == BlePhase.CONNECTING ||
            phase == BlePhase.CONNECTED
}

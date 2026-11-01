package ru.systemt.highwater.data.ble

/**
 * Настройки BLE-сканирования и фильтрации.
 *
 * Значения Company ID и первых байт manufacturer data зависят от конкретного
 * устройства. Замените их на данные из документации производителя.
 */
object BleConfig {
    /** Длительность сканирования списка устройств. */
    const val SCAN_DURATION_MS = 60_000L

    /** Таймаут поиска сохранённого устройства. */
    const val SEARCH_TIMEOUT_MS = 15_000L

    /** Таймаут установления GATT-соединения. */
    const val CONNECT_TIMEOUT_MS = 15_000L

    /** Период обновления RSSI активного соединения. */
    const val RSSI_UPDATE_INTERVAL_MS = 3_000L

    /** Как часто обновлять прогресс сканирования. */
    const val PROGRESS_UPDATE_INTERVAL_MS = 100L

    /** Company ID из advertising packet. По умолчанию — заглушка 0x1234. */
    const val COMPANY_ID = 0x1234

    /** Первые байты manufacturer data, по которым фильтруются устройства. */
    val MANUFACTURER_DATA_PREFIX = byteArrayOf(0x48, 0x57) // "HW"

    /** Маска для сравнения префикса manufacturer data. */
    val MANUFACTURER_DATA_MASK = ByteArray(MANUFACTURER_DATA_PREFIX.size) { 0xFF.toByte() }

    /** Идентификатор foreground-уведомления. */
    const val NOTIFICATION_ID = 1001

    /** Канал foreground-уведомления. */
    const val NOTIFICATION_CHANNEL_ID = "ble_connection"

    /** Небольшая задержка перед остановкой сервиса, когда BLE-сессия неактивна. */
    const val IDLE_STOP_DELAY_MS = 5_000L
}

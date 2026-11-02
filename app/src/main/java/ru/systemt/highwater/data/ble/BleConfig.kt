package ru.systemt.highwater.data.ble

/**
 * Настройки BLE-сканирования и фильтрации АМД.
 *
 * Фильтрация повторяет метод IsAMD() из Delphi-прототипа
 * (Pages.SelectDevice.pas): разбирается рекламный пакет и проверяется
 * набор AD-структур. Значения вынесены сюда, чтобы их можно было
 * менять без правки логики.
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

    /** AD-тип, в котором АМД передаёт серийный номер в ASCII. */
    const val AMD_SERIAL_AD_TYPE = 0x53

    /** AD-тип Shortened Local Name. */
    const val SHORTENED_LOCAL_NAME_AD_TYPE = 0x08

    /** Ожидаемое количество различных AD-структур в пакете АМД. */
    const val AMD_AD_STRUCTURE_COUNT = 4

    /** Ожидаемая длина Shortened Local Name в пакете АМД. */
    const val AMD_SHORTENED_NAME_LENGTH = 12

    /** Префикс отображаемого имени: "S" + серийный номер. */
    const val AMD_NAME_PREFIX = "S"

    /** Идентификатор foreground-уведомления. */
    const val NOTIFICATION_ID = 1001

    /** Канал foreground-уведомления. */
    const val NOTIFICATION_CHANNEL_ID = "ble_connection"

    /** Небольшая задержка перед остановкой сервиса, когда BLE-сессия неактивна. */
    const val IDLE_STOP_DELAY_MS = 5_000L
}

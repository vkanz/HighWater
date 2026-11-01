package ru.systemt.highwater.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Постоянное хранилище выбранного BLE-устройства.
 */
interface DevicePreferencesRepository {
    val savedDeviceAddress: Flow<String?>

    suspend fun saveDeviceAddress(address: String)

    suspend fun clearDeviceAddress()
}

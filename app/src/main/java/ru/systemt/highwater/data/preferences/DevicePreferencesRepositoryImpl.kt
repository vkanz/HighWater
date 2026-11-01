package ru.systemt.highwater.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.systemt.highwater.domain.repository.DevicePreferencesRepository

@Singleton
class DevicePreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : DevicePreferencesRepository {

    override val savedDeviceAddress: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[DEVICE_ADDRESS_KEY] }

    override suspend fun saveDeviceAddress(address: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ADDRESS_KEY] = address.uppercase(Locale.US)
        }
    }

    override suspend fun clearDeviceAddress() {
        dataStore.edit { preferences ->
            preferences.remove(DEVICE_ADDRESS_KEY)
        }
    }

    private companion object {
        val DEVICE_ADDRESS_KEY = stringPreferencesKey("device_address")
    }
}

package ru.systemt.highwater.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val PREFERENCES_NAME = "highwater_preferences"

/** Единственный экземпляр DataStore на процесс. */
val Context.highWaterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME,
)

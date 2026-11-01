package ru.systemt.highwater.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.systemt.highwater.data.preferences.DevicePreferencesRepositoryImpl
import ru.systemt.highwater.data.preferences.highWaterDataStore
import ru.systemt.highwater.domain.repository.DevicePreferencesRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.highWaterDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDevicePreferencesRepository(
        impl: DevicePreferencesRepositoryImpl,
    ): DevicePreferencesRepository
}

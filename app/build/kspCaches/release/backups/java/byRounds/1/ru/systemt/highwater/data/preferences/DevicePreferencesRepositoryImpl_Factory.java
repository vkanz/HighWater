package ru.systemt.highwater.data.preferences;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class DevicePreferencesRepositoryImpl_Factory implements Factory<DevicePreferencesRepositoryImpl> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public DevicePreferencesRepositoryImpl_Factory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public DevicePreferencesRepositoryImpl get() {
    return newInstance(dataStoreProvider.get());
  }

  public static DevicePreferencesRepositoryImpl_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new DevicePreferencesRepositoryImpl_Factory(dataStoreProvider);
  }

  public static DevicePreferencesRepositoryImpl newInstance(DataStore<Preferences> dataStore) {
    return new DevicePreferencesRepositoryImpl(dataStore);
  }
}

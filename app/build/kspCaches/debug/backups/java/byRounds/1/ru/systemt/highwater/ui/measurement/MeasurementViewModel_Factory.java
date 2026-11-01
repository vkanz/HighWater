package ru.systemt.highwater.ui.measurement;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import ru.systemt.highwater.data.ble.BleManager;
import ru.systemt.highwater.domain.repository.DevicePreferencesRepository;
import ru.systemt.highwater.service.BleServiceManager;

@ScopeMetadata
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
public final class MeasurementViewModel_Factory implements Factory<MeasurementViewModel> {
  private final Provider<BleManager> bleManagerProvider;

  private final Provider<DevicePreferencesRepository> devicePreferencesProvider;

  private final Provider<BleServiceManager> serviceManagerProvider;

  public MeasurementViewModel_Factory(Provider<BleManager> bleManagerProvider,
      Provider<DevicePreferencesRepository> devicePreferencesProvider,
      Provider<BleServiceManager> serviceManagerProvider) {
    this.bleManagerProvider = bleManagerProvider;
    this.devicePreferencesProvider = devicePreferencesProvider;
    this.serviceManagerProvider = serviceManagerProvider;
  }

  @Override
  public MeasurementViewModel get() {
    return newInstance(bleManagerProvider.get(), devicePreferencesProvider.get(), serviceManagerProvider.get());
  }

  public static MeasurementViewModel_Factory create(Provider<BleManager> bleManagerProvider,
      Provider<DevicePreferencesRepository> devicePreferencesProvider,
      Provider<BleServiceManager> serviceManagerProvider) {
    return new MeasurementViewModel_Factory(bleManagerProvider, devicePreferencesProvider, serviceManagerProvider);
  }

  public static MeasurementViewModel newInstance(BleManager bleManager,
      DevicePreferencesRepository devicePreferences, BleServiceManager serviceManager) {
    return new MeasurementViewModel(bleManager, devicePreferences, serviceManager);
  }
}

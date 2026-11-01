package ru.systemt.highwater.ui.devices;

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
public final class DeviceSelectionViewModel_Factory implements Factory<DeviceSelectionViewModel> {
  private final Provider<BleManager> bleManagerProvider;

  private final Provider<DevicePreferencesRepository> devicePreferencesProvider;

  private final Provider<BleServiceManager> serviceManagerProvider;

  public DeviceSelectionViewModel_Factory(Provider<BleManager> bleManagerProvider,
      Provider<DevicePreferencesRepository> devicePreferencesProvider,
      Provider<BleServiceManager> serviceManagerProvider) {
    this.bleManagerProvider = bleManagerProvider;
    this.devicePreferencesProvider = devicePreferencesProvider;
    this.serviceManagerProvider = serviceManagerProvider;
  }

  @Override
  public DeviceSelectionViewModel get() {
    return newInstance(bleManagerProvider.get(), devicePreferencesProvider.get(), serviceManagerProvider.get());
  }

  public static DeviceSelectionViewModel_Factory create(Provider<BleManager> bleManagerProvider,
      Provider<DevicePreferencesRepository> devicePreferencesProvider,
      Provider<BleServiceManager> serviceManagerProvider) {
    return new DeviceSelectionViewModel_Factory(bleManagerProvider, devicePreferencesProvider, serviceManagerProvider);
  }

  public static DeviceSelectionViewModel newInstance(BleManager bleManager,
      DevicePreferencesRepository devicePreferences, BleServiceManager serviceManager) {
    return new DeviceSelectionViewModel(bleManager, devicePreferences, serviceManager);
  }
}
